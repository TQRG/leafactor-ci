package com.leafactor.gradle.plugin;

import com.android.build.gradle.AppExtension;
import com.android.build.gradle.api.ApplicationVariant;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class ApplicationVariantManager {
    private AppExtension appExtension;
    private DependenciesManager dependenciesManager;
    private ApplicationVariant applicationVariant;
    private String flavorName;
    private String buildTypeName;
    private String variantName;
    private String variantNameCapitalized;
    private String flavorImplementation;

    public ApplicationVariantManager(AppExtension appExtension, DependenciesManager dependenciesManager, ApplicationVariant applicationVariant) {
        this.appExtension = appExtension;
        this.dependenciesManager = dependenciesManager;
        this.applicationVariant = applicationVariant;
        this.flavorName = applicationVariant.getFlavorName();
        this.buildTypeName = applicationVariant.getBuildType().getName();
        this.variantName = applicationVariant.getName();
        this.variantNameCapitalized = variantName.substring(0, 1).toUpperCase() + variantName.substring(1);
        this.flavorImplementation = flavorName.isEmpty() ? "implementation" : flavorName + "Implementation";
    }

    List<String> getFlavorDependencies(Project project) {
        List<String> flavorDependencies = new ArrayList<>(dependenciesManager.getDependencies());
        if (project.getConfigurations().getByName(flavorImplementation).getState() == Configuration.State.UNRESOLVED) {
            project.getConfigurations().getByName(flavorImplementation).setCanBeResolved(true);
        }
        project.getConfigurations().getByName(flavorImplementation).getResolvedConfiguration().getResolvedArtifacts()
                .forEach(resolvedArtifact -> {
                    flavorDependencies.add(resolvedArtifact.getFile().getAbsolutePath());
                });
        return flavorDependencies;
    }

    String getAidlFilesDir() {
        String aidlFilesDir = String.format("%s/build/generated/aidl_source_output_dir/%s/compile%sAidl/out", dependenciesManager.getProjectPath(), variantName, variantNameCapitalized);
        if (flavorName.isEmpty()) {
            if (!new File(aidlFilesDir).exists()) {
                return String.format("%s/build/generated/aidl_source_output_dir/%s/out", dependenciesManager.getProjectPath(), variantName);
            }
        }
        return aidlFilesDir;
    }

    String getRFilesDir() {
        String rFilesDir = String.format("%s/build/generated/not_namespaced_r_class_sources/%s/process%sResources/r", dependenciesManager.getProjectPath(), variantName, variantNameCapitalized);
        if (flavorName.isEmpty()) {
            if (!new File(rFilesDir).exists()) {
                return String.format("%s/build/generated/not_namespaced_r_class_sources/%s/r", dependenciesManager.getProjectPath(), variantName);
            }
        }
        return rFilesDir;
    }

    String getBuildConfigFilesDir() {
        String buildConfigFilesDir = String.format("%s/build/generated/source/buildConfig/%s/%s", dependenciesManager.getProjectPath(), flavorName, buildTypeName);
        if (flavorName.isEmpty()) {
            if (!new File(buildConfigFilesDir).exists()) {
                return String.format("%s/build/generated/source/buildConfig/%s", dependenciesManager.getProjectPath(), buildTypeName);
            }
        }
        return buildConfigFilesDir;
    }

    String getMainFilesDir() {
        return Paths.get(dependenciesManager.getProjectPath(), "src", "main", "java").toString();
    }

    String getFlavorFilesDir() {
        return Paths.get(dependenciesManager.getProjectPath(), "src", flavorName, "java").toString();
    }

    public AppExtension getAppExtension() {
        return appExtension;
    }

    public DependenciesManager getDependenciesManager() {
        return dependenciesManager;
    }

    public ApplicationVariant getApplicationVariant() {
        return applicationVariant;
    }

    public String getFlavorName() {
        return flavorName;
    }

    public String getBuildTypeName() {
        return buildTypeName;
    }

    public String getVariantName() {
        return variantName;
    }

    public String getVariantNameCapitalized() {
        return variantNameCapitalized;
    }

    public String getFlavorImplementation() {
        return flavorImplementation;
    }
}
