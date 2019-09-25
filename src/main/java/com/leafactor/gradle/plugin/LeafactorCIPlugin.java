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
        project.getExtensions().create("launcherExtension", LauncherExtension.class);
        project.getTasks().create(TASK_NAME, Refactor.class, refactor -> refactor.init(project));
    }
}



