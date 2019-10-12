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
import spoon.processing.Processor;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.visitor.DefaultJavaPrettyPrinter;
import spoon.support.sniper.SniperJavaPrettyPrinter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class Refactor extends DefaultTask {
    private Project project;
    private LauncherExtension launcherExtension;

    void init(Project project, LauncherExtension launcherExtension) {
        this.project = project;
        this.launcherExtension = launcherExtension;
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
        environment.setPrettyPrinterCreator(() -> {
            DefaultJavaPrettyPrinter printer = new DefaultJavaPrettyPrinter(environment);
            List<Processor<CtElement>> preprocessors = Collections.unmodifiableList(new ArrayList());
            printer.setIgnoreImplicit(true);
            printer.setPreprocessors(preprocessors);
            return printer;
        });

        // Create a group of compilation units
        CompilationUnitGroup compilationUnitGroup = new CompilationUnitGroup(launcher);
        System.out.println("PROJECT PATH:" + Paths.get(project.getProjectDir().toPath().toString(), "src", "main", "java"));

        // todo - Add application variant support
//        System.out.println(appExtension.getSourceSets().getByName("main").getRes());
//        appExtension.getApplicationVariants().forEach((applicationVariant -> {
//            System.out.println("Variant [" + applicationVariant.getName() + "]");
//            applicationVariant.getOutputs().forEach(baseVariantOutput -> {
//
//            });
//        }));

        File outputDirectory = null;
        if(launcherExtension.getSourceOutputDirectory() != null) {
            outputDirectory = new File(launcherExtension.getSourceOutputDirectory());
        }

        if(outputDirectory != null && !outputDirectory.isDirectory()) {
            throw new RuntimeException("No such directory " + launcherExtension.getSourceOutputDirectory());
        }

        if(outputDirectory != null) {
            compilationUnitGroup.setSourceOutputDirectory(outputDirectory);
        }

        System.out.println("Extension" + launcherExtension);
        if(launcherExtension != null) {
            for (String file : launcherExtension.getFiles()) {
                System.out.println("file" + file);
                try {
                    compilationUnitGroup.add(new File(file));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        try {
            // Run the group of compilation units with the set of refactoring rules
            compilationUnitGroup.run(refactoringRules);
        } catch (Exception e) {
            // Todo - Be more specific.
            System.out.println("Something went wrong.");
        }
    }
}