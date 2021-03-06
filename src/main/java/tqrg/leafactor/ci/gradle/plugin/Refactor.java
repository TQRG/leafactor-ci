package tqrg.leafactor.ci.gradle.plugin;

import com.android.build.gradle.AppExtension;
import com.android.build.gradle.AppPlugin;
import tqrg.leafactor.ci.engine.CompilationUnitGroup;
import tqrg.leafactor.ci.engine.RefactoringRule;
import tqrg.leafactor.ci.engine.logging.IterationLogEntry;
import tqrg.leafactor.ci.engine.logging.IterationLogger;
import tqrg.leafactor.ci.engine.logging.IterationPhaseLogEntry;
import tqrg.leafactor.ci.rules.DrawAllocationRefactoringRule;
import tqrg.leafactor.ci.rules.RecycleRefactoringRule;
import tqrg.leafactor.ci.rules.ViewHolderRefactoringRule;
import tqrg.leafactor.ci.rules.WakeLockRefactoringRule;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskAction;
import spoon.Launcher;
import spoon.compiler.Environment;
import spoon.support.sniper.SniperJavaPrettyPrinter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.*;

public class Refactor extends DefaultTask {
    private Project project;
    private LauncherExtension launcherExtension;

    void init(Project project, LauncherExtension launcherExtension) {
        this.project = project;
        this.launcherExtension = launcherExtension;
    }

    private AppExtension getAppExtension() {
        // Check if the Android AppPlugin is present
        if (!project.getPlugins().hasPlugin(AppPlugin.class)) {
            throw new RuntimeException("should be declared after 'com.android.application'");
        }
        // Get the AppExtension from the gradle runtime
        AppExtension appExtension = project.getExtensions().findByType(AppExtension.class);
        assert appExtension != null;
        return appExtension;
    }

    private boolean shouldProcessApplicationVariant(String variantName) {
        return (!launcherExtension.isWhiteListVariants() || launcherExtension.getVariants().contains(variantName))
                && (launcherExtension.isWhiteListVariants() || !launcherExtension.getVariants().contains(variantName));
    }

    private void setupOutputDirectory(CompilationUnitGroup compilationUnitGroup, String leafFolderName) {
        // Optional output directory
        File outputDirectory = null;
        if (launcherExtension.getSourceOutputDirectory() != null) {
            outputDirectory = new File(launcherExtension.getSourceOutputDirectory());
        }

        if (outputDirectory != null && !outputDirectory.isDirectory()) {
            throw new RuntimeException("No such directory " + launcherExtension.getSourceOutputDirectory());
        }

        if (outputDirectory != null) {
            File leafactorGenDir = new File(outputDirectory.getAbsolutePath() + "/leafactor-ci/" + leafFolderName);
            if (!leafactorGenDir.exists() && !leafactorGenDir.mkdirs()) {
                throw new RuntimeException("Could not create directory " + leafactorGenDir.getAbsolutePath());
            }
            compilationUnitGroup.setSourceOutputDirectory(leafactorGenDir);
        }
    }

    // This will process the application variant with full classpath specification
    private void processApplicationVariant(ApplicationVariantManager applicationVariantManager) throws IOException {
        // Creating the spoon launcher
        Launcher launcher = new Launcher();
        Environment environment = launcher.getEnvironment();
        // Configure the environment to use a custom classPath
        List<String> flavorDependencies = applicationVariantManager.getFlavorDependencies(project);
        environment.setSourceClasspath(DependenciesManager.dependenciesToClassPath(flavorDependencies));
        environment.setAutoImports(true);
        environment.setPrettyPrinterCreator(() -> {
            SniperJavaPrettyPrinter sniperJavaPrettyPrinter = new SniperJavaPrettyPrinter(environment);
            sniperJavaPrettyPrinter.setIgnoreImplicit(false);
            return sniperJavaPrettyPrinter;
        });

        CompilationUnitGroup compilationUnitGroup = new CompilationUnitGroup(launcher);
        setupOutputDirectory(compilationUnitGroup, applicationVariantManager.getVariantName());

        if (new File(applicationVariantManager.getAidlFilesDir()).exists()) {
            compilationUnitGroup.add(new File(applicationVariantManager.getAidlFilesDir()));
        }
        if (new File(applicationVariantManager.getRFilesDir()).exists()) {
            compilationUnitGroup.add(new File(applicationVariantManager.getRFilesDir()));
        }
        if (new File(applicationVariantManager.getBuildConfigFilesDir()).exists()) {
            compilationUnitGroup.add(new File(applicationVariantManager.getBuildConfigFilesDir()));
        }
        if (new File(applicationVariantManager.getFlavorFilesDir()).exists()) {
            compilationUnitGroup.add(new File(applicationVariantManager.getFlavorFilesDir()));
        }
        if (new File(applicationVariantManager.getMainFilesDir()).exists()) {
            compilationUnitGroup.add(new File(applicationVariantManager.getMainFilesDir()));
        }

        // Run the group of compilation units with the set of refactoring rules
        IterationLogger logger = new IterationLogger();
        List<RefactoringRule> refactoringRules = new ArrayList<>();
        // Adding all the refactoring rules
        refactoringRules.add(new RecycleRefactoringRule(logger));
        refactoringRules.add(new ViewHolderRefactoringRule(logger));
        refactoringRules.add(new DrawAllocationRefactoringRule(logger));
        refactoringRules.add(new WakeLockRefactoringRule(logger));

        compilationUnitGroup.run(refactoringRules);
        for (IterationLogEntry entry : logger.getLogs()) {
            System.out.println("Log entry:");
            System.out.println("Timestamp: " + entry.getTimeStamp());
            System.out.println("Name: " + entry.getName());
            System.out.println("Description: " + entry.getDescription());
            System.out.println("Refactoring rule: " + entry.getRule().getClass().getName());
            if (entry instanceof IterationPhaseLogEntry) {
                IterationPhaseLogEntry iterationPhaseLogEntry = (IterationPhaseLogEntry) entry;
                Duration duration = iterationPhaseLogEntry.getPhaseDuration();
                System.out.println(iterationPhaseLogEntry.getStartPhaseTimestamp());
                System.out.println(iterationPhaseLogEntry.getEndPhaseTimestamp());
                System.out.println("Duration: " + duration.toNanos());
            }
        }
    }

    private void iterateOverApplicationVariants(AppExtension appExtension, DependenciesManager dependenciesManager) {
        appExtension.getApplicationVariants().forEach((applicationVariant -> {
            try {
                ApplicationVariantManager applicationVariantManager = new ApplicationVariantManager(appExtension, dependenciesManager, applicationVariant);
                if (!shouldProcessApplicationVariant(applicationVariantManager.getVariantName())) {
                    return;
                }
                processApplicationVariant(applicationVariantManager);
            } catch (Exception e) {
                // Todo - Be more specific.
                e.printStackTrace();
            }
        }));
    }

    private void processWithoutClassPath() throws IOException {
        String projectPath = project.getProjectDir().toPath().toString();
        String sourcePath = Paths.get(projectPath, "src", "main", "java").toString();

        CompilationUnitGroup compilationUnitGroup = new CompilationUnitGroup(null);
        setupOutputDirectory(compilationUnitGroup, "main");
        compilationUnitGroup.add(new File(sourcePath));

        IterationLogger logger = new IterationLogger();
        List<RefactoringRule> refactoringRules = new ArrayList<>();

        // Adding all the refactoring rules
        refactoringRules.add(new RecycleRefactoringRule(logger));
        refactoringRules.add(new ViewHolderRefactoringRule(logger));
        refactoringRules.add(new DrawAllocationRefactoringRule(logger));
        refactoringRules.add(new WakeLockRefactoringRule(logger));

        // Run the group of compilation units with the set of refactoring rules
        compilationUnitGroup.runInIsolation(refactoringRules);
//        compilationUnitGroup.run(refactoringRules);

//        for (IterationLogEntry entry : logger.getLogs()) {
//            System.out.println("Log entry:");
//            System.out.println("Timestamp: " + entry.getTimeStamp());
//            System.out.println("Name: " + entry.getName());
//            System.out.println("Description: " + entry.getDescription());
//            System.out.println("Refactoring rule: " + entry.getRule().getClass().getName());
//            if (entry instanceof IterationPhaseLogEntry) {
//                IterationPhaseLogEntry iterationPhaseLogEntry = (IterationPhaseLogEntry) entry;
//                Duration duration = iterationPhaseLogEntry.getPhaseDuration();
//                System.out.println(iterationPhaseLogEntry.getStartPhaseTimestamp());
//                System.out.println(iterationPhaseLogEntry.getEndPhaseTimestamp());
//                System.out.println("Duration: " + duration.toNanos());
//            }
//        }
    }

    @TaskAction
    public void task() throws IOException {
        AppExtension appExtension = getAppExtension();
        if (launcherExtension.isUsingClasspath()) {
            DependenciesManager dependenciesManager = new DependenciesManager(appExtension, project);
            iterateOverApplicationVariants(appExtension, dependenciesManager);
        } else {
            processWithoutClassPath();
        }
    }
}