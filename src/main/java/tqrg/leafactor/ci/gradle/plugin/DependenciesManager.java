package tqrg.leafactor.ci.gradle.plugin;

import com.android.build.gradle.AppExtension;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.apache.commons.io.FileUtils;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class DependenciesManager {

    private File sdkDirectory;
    private String apiLevel;
    private String projectPath;
    private String androidJarPath;
    private List<String> dependencies = new ArrayList<>();

    DependenciesManager(AppExtension appExtension, Project project) {
        // Get the SDK directory and the current API LEVEL.
        sdkDirectory = appExtension.getSdkDirectory();
        apiLevel = appExtension.getCompileSdkVersion();
        projectPath = project.getProjectDir().toPath().toString();
        androidJarPath = sdkDirectory.getAbsolutePath() + "/platforms/" + apiLevel + "/android.jar";
        // Adding android dependency
        dependencies.add(androidJarPath);
        if (project.getConfigurations().getByName("implementation").getState() == Configuration.State.UNRESOLVED) {
            project.getConfigurations().getByName("implementation").setCanBeResolved(true);
        }
        // Getting the resolved configurations and gathering the file dependencies.
        project.getConfigurations().getByName("implementation").getResolvedConfiguration().getResolvedArtifacts()
                .forEach(resolvedArtifact -> {
                    dependencies.add(resolvedArtifact.getFile().getAbsolutePath());
                });
    }

    public File getSdkDirectory() {
        return sdkDirectory;
    }

    public String getApiLevel() {
        return apiLevel;
    }

    public String getProjectPath() {
        return projectPath;
    }

    public String getAndroidJarPath() {
        return androidJarPath;
    }

    public List<String> getDependencies() {
        return dependencies;
    }
/*
            List<String> dependencies = new ArrayList<>();
        // this is not required to reproduce the bug
        dependencies.add(downloadAndroidPlatform());

        // Preparing to Repository system for maven dependency resolution.
//        RepositorySystem repositorySystem = newSystem();
//        File temp = Files.createTempDirectory("maven-repo").toFile();
//        LocalRepository localRepository = new LocalRepository(temp, "simple");
//        RepositorySystemSession repositorySystemSession = newSession(repositorySystem, localRepository);
        // Download an artifact from maven remote repo
//        Artifact artifact = new DefaultArtifact("com.android.support:appcompat-v7:aar:28.0.0");
//        dependencies.add(resolveArtifact(repositorySystem, repositorySystemSession, getRemoteRepos(), artifact).getArtifact().getFile().getAbsolutePath());


     */

    static List<RemoteRepository> getRemoteRepos() {
        RemoteRepository mavenCentral = new RemoteRepository.Builder("maven-central", "default", "http://repo1.maven.org/maven2/").build();
        RemoteRepository mavenGoogle = new RemoteRepository.Builder("maven-google", "default", "http://maven.google.com/").build();
        List<RemoteRepository> remoteRepositories = new ArrayList<>();
//        remoteRepositories.add(mavenCentral);
        remoteRepositories.add(mavenGoogle);
        return remoteRepositories;
    }

    static RepositorySystem newSystem() {
        DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
        locator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
        locator.addService(TransporterFactory.class, FileTransporterFactory.class);
        locator.addService(TransporterFactory.class, HttpTransporterFactory.class);
        locator.getService(RepositorySystem.class);
        return locator.getService(RepositorySystem.class);
    }

    static RepositorySystemSession newSession(RepositorySystem repositorySystem, LocalRepository localRepository) {
        DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
        session.setLocalRepositoryManager(repositorySystem.newLocalRepositoryManager(session, localRepository));
        return session;
    }

    static ArtifactResult resolveArtifact(RepositorySystem repoSystem,
                                          RepositorySystemSession repoSession,
                                          List<RemoteRepository> remoteRepos,
                                          Artifact artifact)
            throws IllegalArgumentException, ArtifactResolutionException {
        ArtifactRequest request = new ArtifactRequest();
        request.setArtifact(artifact);
        request.setRepositories(remoteRepos);
        return repoSystem.resolveArtifact(repoSession, request);
    }

    static String downloadAndroidPlatform() throws IOException {
        // todo - According to the API level
        String platform = "https://dl.google.com/android/repository/platform-28_r06.zip";
        File tempDirectory = Files.createTempDirectory("sdk").toFile();
        File destination = new File(tempDirectory.getAbsolutePath() + "/platform-28_r06.zip");
        // 80mb download
        System.out.println("Downloading platform");
        FileUtils.copyURLToFile(new URL(platform), destination);
        try {
            ZipFile zipFile = new ZipFile(destination);
            zipFile.extractAll(tempDirectory.getAbsolutePath());
        } catch (ZipException e) {
            e.printStackTrace();
        }
        File androidJar = new File(tempDirectory.getAbsolutePath() + "/android-9/android.jar");
        return androidJar.getAbsolutePath();
    }

    static String[] dependenciesToClassPath(List<String> dependencyPaths) throws IOException {
        // Migrate list to array of string
        String[] classPath = new String[dependencyPaths.size()];
        for (int i = 0; i < dependencyPaths.size(); i++) {
            String originalFile = dependencyPaths.get(i);
            if (dependencyPaths.get(i).endsWith(".aar")) {
                // AAR Files need to be converted to JAR in order to be included
                Path tempDirectory = Files.createTempDirectory("aarFileExtraction");
                String destination = tempDirectory.toAbsolutePath().toString();
                try {
                    ZipFile zipFile = new ZipFile(originalFile);
                    zipFile.extractAll(destination);
                } catch (ZipException e) {
                    e.printStackTrace();
                }
                classPath[i] = destination + "/classes.jar";
            } else {
                classPath[i] = originalFile;
            }
        }
        return classPath;
    }

}
