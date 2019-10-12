package com.leafactor.gradle.plugin;

import java.util.ArrayList;

public class LauncherExtension  {
    private ArrayList<String> files;

    public String getSourceOutputDirectory() {
        return sourceOutputDirectory;
    }

    public void setSourceOutputDirectory(String sourceOutputDirectory) {
        this.sourceOutputDirectory = sourceOutputDirectory;
    }

    private String sourceOutputDirectory;

    public ArrayList<String> getFiles() {
        return files;
    }

    public void setFiles(ArrayList<String> files) {
        this.files = files;
    }
}