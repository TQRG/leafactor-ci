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
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
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
            classPath[i] = dependencyPaths.get(i);
        }

        // Configure the environment to use a custom classPath
        environment.setNoClasspath(false);
        environment.setSourceClasspath(classPath);
        environment.setAutoImports(true);
        environment.setPrettyPrinterCreator(() -> new SniperJavaPrettyPrinter(launcher.getEnvironment()));

        // Create a group of compilation units
        CompilationUnitGroup group = new CompilationUnitGroup(launcher);
        System.out.println("PROJECT PATH:" + Paths.get(project.getProjectDir().toPath().toString(), "src", "main", "java"));

        try {
            // todo - adding a single file for testing purposes
            group.add(Paths.get(project.getProjectDir().toPath().toString(), "src", "main", "java").toFile());
        } catch (IOException e) {
            System.out.println("Error creating compilation group with file: " + Paths.get(project.getProjectDir().toPath().toString(), "src"));
        }
//
////        try {
////            Files.walk(Paths.get(project.getProjectDir().toPath().toString(), "src")).forEach((path -> {
////                File file = path.toFile();
////                if (!file.exists()) {
////                    System.out.println("File does not exist: " + file.getAbsolutePath());
////                    return;
////                }
////
////                if ((file.isDirectory() || file.getAbsolutePath().endsWith(".java"))) {
////                    try {
////                        group.add(file);
////                    } catch (IOException e) {
////                        System.out.println("Error creating compilation group with file: " + file.getAbsolutePath());
////                    }
////                } else {
//////                    System.out.println("Invalid file: " + file.getAbsolutePath());
////                }
////            }));
////        } catch (IOException e) {
////            e.printStackTrace();
////        }
//
        try {
            // Run the group of compilation units with the set of refactoring rules
            group.run(refactoringRules);
        } catch (Exception e) {
            System.out.println("Something went wrong.");
        }
    }
}