package com.leafactor.gradle.plugin;

import com.android.build.gradle.AppExtension;
import com.android.build.gradle.AppPlugin;
import com.leafactor.cli.engine.CompilationUnitGroup;
import com.leafactor.cli.engine.RefactoringRule;
import com.leafactor.cli.engine.logging.IterationLogger;
import com.leafactor.cli.rules.DrawAllocationRefactoringRule;
import com.leafactor.cli.rules.RecycleRefactoringRule;
import com.leafactor.cli.rules.ViewHolderRefactoringRule;
import com.leafactor.cli.rules.WakeLockRefactoringRule;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.file.FileCollection;
import org.gradle.api.internal.file.UnionFileCollection;
import org.gradle.api.tasks.TaskAction;
import spoon.Launcher;
import spoon.compiler.Environment;
import spoon.support.sniper.SniperJavaPrettyPrinter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;

public class Refactor extends DefaultTask {
    private Project project;

    void init(Project project) {
        this.project = project;
    }

    @TaskAction
    public void task() {
        // Check if the Android AppPlugin is present
        if (!project.getPlugins().hasPlugin(AppPlugin.class)) {
            throw new RuntimeException("should be declared after 'com.android.application'");
        }
        // Get the AppExtension from the gradle runtime
        AppExtension appExtension = project.getExtensions().findByType(AppExtension.class);
        assert appExtension != null;
        // We will hold all the resolved dependency paths in a List of Strings
        final List<String> dependencyPaths = new ArrayList<>();
        // Getting the resolved configurations and gathering the file dependencies.
        // Note: Requires access to the implementation configuration, add in gradle:
        //  - configurations.implementation.setCanBeResolved(true)
        //  - configurations.api.setCanBeResolved(true)
        project.getConfigurations().getByName("implementation").getResolvedConfiguration().getResolvedArtifacts()
                .forEach(resolvedArtifact -> {
                    System.out.println("NAME"+ resolvedArtifact.getName());
                    dependencyPaths.add(resolvedArtifact.getFile().getAbsolutePath());
                });

        // Todo - Launcher extension is not in use for now.
        LauncherExtension extension = project.getExtensions().findByType(LauncherExtension.class);
        IterationLogger logger = new IterationLogger();
        List<RefactoringRule> refactoringRules = new ArrayList<>();
        // Adding all the refactoring rules
        refactoringRules.add(new RecycleRefactoringRule(logger));
        refactoringRules.add(new ViewHolderRefactoringRule(logger));
        refactoringRules.add(new DrawAllocationRefactoringRule(logger));
        refactoringRules.add(new WakeLockRefactoringRule(logger));

        // Creating the spoon launcher
        Launcher launcher = new Launcher();
        Environment environment = launcher.getEnvironment();

        // Get the SDK directory and the current API LEVEL.
        File sdkDirectory = appExtension.getSdkDirectory();
        String APILevel = appExtension.getCompileSdkVersion();
        System.out.println("SDK: " + sdkDirectory);
        System.out.println("API LEVEL: " + APILevel);
        String androidJarPath = sdkDirectory.getAbsolutePath() + "/platforms/" + APILevel + "/android.jar";
        dependencyPaths.add(androidJarPath);

        // Migrate to array of string
        String [] classPath = new String[dependencyPaths.size()];
        for(int i = 0; i < dependencyPaths.size(); i++) {
            String originalFile = dependencyPaths.get(i);
            System.out.println("-------------------------------------------------");
            if(dependencyPaths.get(i).endsWith(".aar")) {
                String destination = originalFile.substring(0, originalFile.length() - 4);
                System.out.println("Source: " + originalFile);
                System.out.println("Destination: " + destination);
                try {
                    ZipFile zipFile = new ZipFile(originalFile);
                    zipFile.extractAll(destination);
                } catch (ZipException e) {
                    e.printStackTrace();
                }
                classPath[i] = destination + "/classes.jar";
            } else {
                classPath[i] = originalFile;
            }
            System.out.println("Dependency added: " + classPath[i]);
            System.out.println("-------------------------------------------------");
        }

        // Configure the environment to use a custom classPath
        environment.setNoClasspath(false);
        environment.setSourceClasspath(classPath);
        environment.setAutoImports(true);
        environment.setPrettyPrinterCreator(() -> new SniperJavaPrettyPrinter(launcher.getEnvironment()));

        // Create a group of compilation units
        CompilationUnitGroup group = new CompilationUnitGroup(launcher);
        System.out.println("PROJECT PATH:" + Paths.get(project.getProjectDir().toPath().toString(), "src", "main", "java"));

        System.out.println(appExtension.getSourceSets().getByName("main").getRes());

        // todo - Add application variant support
//        appExtension.getApplicationVariants().forEach((applicationVariant -> {
//            System.out.println("Variant [" + applicationVariant.getName() + "]");
//            applicationVariant.getOutputs().forEach(baseVariantOutput -> {
//
//            });
//        }));
        System.out.println("Extension" + extension);
        try {
            if(extension != null) {
                if (extension.getFiles() instanceof UnionFileCollection) {
                    UnionFileCollection collection = (UnionFileCollection) extension.getFiles();
                    System.out.println("extension.getFiles()" + extension.getFiles());
                    for(FileCollection fileCollection : collection.getSources()) {
                        for(File file : fileCollection.getFiles()) {
                            System.out.println("File: " + file.getAbsolutePath());
                            group.add(file);
                        }
                    }
                }

            }
        } catch (IOException e) {
            System.out.println("Error creating compilation group with file: " + Paths.get(project.getProjectDir().toPath().toString(), "src"));
        }

        try {
            // Run the group of compilation units with the set of refactoring rules
            group.run(refactoringRules);
        } catch (Exception e) {
            // Todo - Be more specific
            System.out.println("Something went wrong.");
        }
    }
}