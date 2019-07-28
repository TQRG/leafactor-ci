package com.leafactor.gradle.plugin;

import com.leafactor.cli.engine.CompilationUnitGroup;
import com.leafactor.cli.engine.RefactoringRule;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

import java.util.List;

public class Refactor extends DefaultTask {
    private CompilationUnitGroup group;
    private List<RefactoringRule> refactoringRules;

    void init(CompilationUnitGroup group, List<RefactoringRule> refactoringRules) {
        this.group = group;
        this.refactoringRules = refactoringRules;
    }

    @TaskAction
    public void task() {
            try {
                group.run(refactoringRules);
            } catch (Exception e) {
                System.out.println("Something went wrong.");
            }
    }
}