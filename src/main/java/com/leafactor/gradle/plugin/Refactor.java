package com.leafactor.gradle.plugin;

import com.android.build.gradle.AppExtension;
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

public class Refactor extends DefaultTask {
    private Project project;

    void init(Project project) {
        this.project = project;
    }

    @TaskAction
    public void task() {

        LauncherExtension extension = project.getExtensions().findByType(LauncherExtension.class);
        AppExtension appExtension = project.getExtensions().findByType(AppExtension.class);
        System.out.println("AppExtension: " + extension.getClassPath());

        Set<File> ccp = extension.getClassPath().getFiles();
        ccp.addAll(appExtension.getBootClasspath());

        IterationLogger logger = new IterationLogger();
        List<RefactoringRule> refactoringRules = new ArrayList<>();
        refactoringRules.add(new RecycleRefactoringRule(logger));
        refactoringRules.add(new ViewHolderRefactoringRule(logger));
        refactoringRules.add(new DrawAllocationRefactoringRule(logger));
        refactoringRules.add(new WakeLockRefactoringRule(logger));

        Launcher launcher = new Launcher();
        Environment environment = launcher.getEnvironment();

        System.out.println("information:");
        for (File file1 : ccp) {
            String absolutePath = file1.getAbsolutePath();
            System.out.println(absolutePath);
        }

        String [] files = ccp.stream().map(File::getAbsolutePath).toArray(String[]::new);
        for (String file : files) {
            System.out.println("ARRAY file" + file);
        }

        System.out.println("ccp.stream().map(File::getAbsolutePath).toArray(String[]::new)" + files);

        environment.setSourceClasspath(files);
        environment.setNoClasspath(false);
        environment.setAutoImports(true);
        environment.setPrettyPrinterCreator(() -> new SniperJavaPrettyPrinter(launcher.getEnvironment()));

        CompilationUnitGroup group = new CompilationUnitGroup(launcher);
        System.out.println("PROJECT PATH:" + Paths.get(project.getProjectDir().toPath().toString(), "src", "main", "java"));

        try {
            group.add(Paths.get(project.getProjectDir().toPath().toString(), "src", "main", "java").toFile());
        } catch (IOException e) {
            System.out.println("Error creating compilation group with file: " + Paths.get(project.getProjectDir().toPath().toString(), "src"));
        }


//        try {
//            Files.walk(Paths.get(project.getProjectDir().toPath().toString(), "src")).forEach((path -> {
//                File file = path.toFile();
//                if (!file.exists()) {
//                    System.out.println("File does not exist: " + file.getAbsolutePath());
//                    return;
//                }
//
//                if ((file.isDirectory() || file.getAbsolutePath().endsWith(".java"))) {
//                    try {
//                        group.add(file);
//                    } catch (IOException e) {
//                        System.out.println("Error creating compilation group with file: " + file.getAbsolutePath());
//                    }
//                } else {
////                    System.out.println("Invalid file: " + file.getAbsolutePath());
//                }
//            }));
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        try {
                group.run(refactoringRules);
            } catch (Exception e) {
                System.out.println("Something went wrong.");
            }
    }
}