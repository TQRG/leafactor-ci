package com.leafactor.gradle.plugin;

public class LauncherExtension  {
    public String getSourceOutputDirectory() {
        return sourceOutputDirectory;
    }

    public void setSourceOutputDirectory(String sourceOutputDirectory) {
        this.sourceOutputDirectory = sourceOutputDirectory;
    }

    private String sourceOutputDirectory;
}