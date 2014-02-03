package com.obteq.maven.plugin.openshift;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.FilenameFilter;
import org.codehaus.plexus.util.FileUtils;

@Mojo(name = "deploy", defaultPhase = LifecyclePhase.PACKAGE)
public class Openshift
        extends AbstractMojo {

    /**
     * Location of the file.
     */
    @Parameter(defaultValue = "${project.build.directory}", property = "outputDir", required = true)
    private File outputDirectory;

    @Parameter(property = "deploy.destination")
    private String destination;

    @Parameter(property = "deploy.user")
    private String user;

    @Parameter(property = "deploy.host")
    private String host;

    @Parameter(defaultValue = "", property = "deploy.keyFile")
    private String keyFilePath;

    @Parameter(property = "deploy.password")
    private String password;

    public void execute()
            throws MojoExecutionException {
        try {
            if (keyFilePath == null || "".equals(keyFilePath)) {
                keyFilePath = System.getProperty("user.home") + "/.ssh/id_rsa1";
            }
            String dest = user + "@" + host + ":~/" + destination;
            System.out.println("Uploading to:" + dest);

            File[] files = outputDirectory.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.toLowerCase().endsWith(".war");
                }
            });
            for (File f : files) {
                getLog().info("Uploading file:" + f.getPath());
                SCPFileUpload.send(user, password, host, f.getPath(), destination, keyFilePath, new ProgressMonitor() {
                    public void progress(long fileSize, long sentBytes, long speed) {
                        getLog().info(String.format("%d%% %s of %s  %s/s",
                                (int) ((100 * sentBytes) / fileSize), FileUtils.byteCountToDisplaySize((int) sentBytes), FileUtils.byteCountToDisplaySize((int) fileSize), FileUtils.byteCountToDisplaySize((int) speed)));
                    }
                });
                getLog().info("File Upload Comleted!");
            }
            getLog().info("Finished");
        } catch (Exception ex) {
            getLog().error(ex);
            throw (new MojoExecutionException(ex.getMessage()));
        }
    }
}
