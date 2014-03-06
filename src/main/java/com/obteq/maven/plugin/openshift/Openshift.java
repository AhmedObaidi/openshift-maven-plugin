package com.obteq.maven.plugin.openshift;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.FilenameFilter;

@Mojo(name = "deploy", defaultPhase = LifecyclePhase.PACKAGE)
public class Openshift extends AbstractMojo {

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

    @Parameter(defaultValue = "", property = "deploy.preCommand")
    private String preCommand;

    @Parameter(defaultValue = "", property = "deploy.postCommand")
    private String postCommand;

    @Parameter(defaultValue = "", property = "deploy.keyFile")
    private String keyFilePath;

    @Parameter(property = "deploy.password")
    private String password;

    public void execute() throws MojoExecutionException {
        try {
            if (keyFilePath == null || "".equals(keyFilePath)) {
                keyFilePath = System.getProperty("user.home") + "/.ssh/id_rsa1";
            }

            File[] files = outputDirectory.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.toLowerCase().endsWith(".war");
                }
            });
            for (File f : files) {
                //getLog().info("Uploading file:" + f.getPath());
                SshManager.deploy(user, password, host, f.getPath(),
                        destination, keyFilePath, preCommand, postCommand, new ProgressMonitor() {
                            @Override
                            public void progress(long fileSize, long sentBytes,
                                    long speed) {
                                Openshift.this.getLog().info(
                                        String.format(
                                                "%d%% %s of %s  %s/s",
                                                (int) ((100 * sentBytes) / fileSize),
                                                byteCountToDisplaySize(sentBytes),
                                                byteCountToDisplaySize(fileSize),
                                                byteCountToDisplaySize(speed)));
                            }

                            @Override
                            public void info(String string) {
                                System.out.println(string);
                            }

                            @Override
                            public void error(String string, Throwable thr) {
                                System.out.println(string);
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
    /**
     * The number of bytes in a kilobyte.
     */
    public static final long ONE_KB = 1024;

    /**
     * The number of bytes in a megabyte.
     */
    public static final long ONE_MB = ONE_KB * ONE_KB;

    /**
     * The number of bytes in a gigabyte.
     */
    public static final long ONE_GB = ONE_KB * ONE_MB;

    public static String byteCountToDisplaySize(long size) {
        String displaySize;

        if (size / ONE_GB > 0) {
            displaySize = String.valueOf(size / ONE_GB) + " GB";
        } else if (size / ONE_MB > 0) {
            displaySize = String.valueOf(size / ONE_MB) + " MB";
        } else if (size / ONE_KB > 0) {
            displaySize = String.valueOf(size / ONE_KB) + " KB";
        } else {
            displaySize = String.valueOf(size) + " bytes";
        }
        return displaySize;
    }
}
