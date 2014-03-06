/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import com.obteq.maven.plugin.openshift.Openshift;
import com.obteq.maven.plugin.openshift.ProgressMonitor;
import com.obteq.maven.plugin.openshift.SshManager;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * 
 * @author ahmed
 */
public class UploadJUnitTest {

	public UploadJUnitTest() {
	}

	// TODO add test methods here.
	// The methods must be annotated with annotation @Test. For example:
	//
	// @Test
	public void testUpload() throws Exception {
		String localFilePath = "C:\\Users\\ahmed\\Documents\\NetBeansProjects\\HelloJEE7\\target\\HelloJEE7.war";
		// String localFilePath = "D:\\sample.js";
		String user = "52ed9de4e0b8cd54a0000513";
		String password = "";
		String host = "wildfly-obteq.rhcloud.com";
		String remoteFilePath = "~/app-root/data/wildfly-8.0.0.CR1/standalone/deployments/";
		String keyFilePath = "C:\\cygwin64\\home\\ahmed\\.ssh\\id_rsa";
		String preUpload = "echo $RFILE > ~/app-root/data/preupload.txt";
		String postUpload = "echo $RFILE > ~/app-root/data/postpload.txt";
		SshManager.deploy(user, password, host, localFilePath,
				remoteFilePath, keyFilePath, preUpload, postUpload,
				new ProgressMonitor() {
					public void progress(long fileSize, long sentBytes,
							long speed) {
						System.out.println(String.format("%d%% %s of %s  %s/s",
								(int) ((100 * sentBytes) / fileSize),
								Openshift.byteCountToDisplaySize(sentBytes),
								Openshift.byteCountToDisplaySize(fileSize),
								Openshift.byteCountToDisplaySize(speed)));
					}

					public void info(String string) {
						System.out.println(string);

					}

					public void error(String string, Throwable thr) {
						System.out.println(string);

					}
				});
		System.out.println("File Upload Comleted!");
	}
}
