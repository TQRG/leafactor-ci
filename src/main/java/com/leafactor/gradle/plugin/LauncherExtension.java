package com.leafactor.gradle.plugin;

import java.util.ArrayList;
import java.util.List;

public class LauncherExtension  {
    private boolean useClasspath = false;
    private boolean whiteListVariants = false;
    private List<String> variants = new ArrayList<>();
    private String sourceOutputDirectory;

    public boolean isUsingClasspath() {
        return useClasspath;
    }

    public String getSourceOutputDirectory() {
        return sourceOutputDirectory;
    }

    public boolean isWhiteListVariants() {
        return whiteListVariants;
    }

    public List<String> getVariants() {
        return variants;
    }

    public void setUseClasspath(boolean useClasspath) {
        this.useClasspath = useClasspath;
    }

    public void setSourceOutputDirectory(String sourceOutputDirectory) {
        this.sourceOutputDirectory = sourceOutputDirectory;
    }

    public void setWhiteListVariants(boolean whiteListVariants) {
        this.whiteListVariants = whiteListVariants;
    }

    public void setVariants(List<String> variants) {
        this.variants = variants;
    }
}