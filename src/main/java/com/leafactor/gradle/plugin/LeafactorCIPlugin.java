package com.leafactor.gradle.plugin;

import com.android.build.gradle.AppExtension;
import com.leafactor.cli.engine.CompilationUnitGroup;
import com.leafactor.cli.engine.RefactoringRule;
import com.leafactor.cli.engine.logging.IterationLogger;
import com.leafactor.cli.rules.DrawAllocationRefactoringRule;
import com.leafactor.cli.rules.RecycleRefactoringRule;
import com.leafactor.cli.rules.ViewHolderRefactoringRule;
import com.leafactor.cli.rules.WakeLockRefactoringRule;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import spoon.Launcher;
import spoon.compiler.Environment;
import spoon.support.sniper.SniperJavaPrettyPrinter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;


public class LeafactorCIPlugin implements Plugin<Project> {
    static final String TASK_NAME = "refactor";

    @Override
    public void apply(Project project) {
        AppExtension appExtension = project.getExtensions().findByType(AppExtension.class);
        List<File> ccp = appExtension.getBootClasspath();

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

        environment.setSourceClasspath(ccp.stream().map(File::getAbsolutePath).toArray(String[]::new));
        environment.setAutoImports(true);
        environment.setPrettyPrinterCreator(() -> new SniperJavaPrettyPrinter(launcher.getEnvironment()));

        CompilationUnitGroup group = new CompilationUnitGroup(launcher);
        try {
            Files.walk(project.getProjectDir().toPath()).forEach((path -> {
                File file = path.toFile();
                if (!file.exists()) {
                    System.out.println("File does not exist: " + file.getAbsolutePath());
                    return;
                }

                if ((file.isDirectory() || file.getAbsolutePath().endsWith(".java"))) {
                    try {
                        group.add(file);
                    } catch (IOException e) {
                        System.out.println("Error creating compilation group with file: " + file.getAbsolutePath());
                    }
                } else {
                    System.out.println("Invalid file: " + file.getAbsolutePath());
                }
            }));
        } catch (IOException e) {
            e.printStackTrace();
        }

        project.getTasks().create(TASK_NAME, Refactor.class, refactor -> refactor.init(group, refactoringRules));

    }
}



