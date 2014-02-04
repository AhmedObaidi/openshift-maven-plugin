package com.obteq.maven.plugin.openshift;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UserInfo;

public class SCPFileUpload {

    private Session session;

    private void exec(String command, ProgressMonitor monitor) throws Exception {
        monitor.info("running " + command);
        Channel channel = session.openChannel("exec");
        ((ChannelExec) channel).setCommand(command);

        // X Forwarding
        // channel.setXForwarding(true);
        // channel.setInputStream(System.in);
        channel.setInputStream(null);

        // channel.setOutputStream(System.out);
        // FileOutputStream fos=new FileOutputStream("/tmp/stderr");
        // ((ChannelExec)channel).setErrStream(fos);
        ((ChannelExec) channel).setErrStream(System.err);

        InputStream in = channel.getInputStream();

        channel.connect();

        byte[] tmp = new byte[1024];
        while (true) {
            while (in.available() > 0) {
                int i = in.read(tmp, 0, 1024);
                if (i < 0) {
                    break;
                }
                System.out.print(new String(tmp, 0, i));
            }
            if (channel.isClosed()) {
                int status = channel.getExitStatus();
                if (status == 0) {
                    monitor.info("commaned run successfully.");
                } else {
                    monitor.info("error in execution.");
                }
                break;
            }
            Thread.sleep(1000);
        }
        channel.disconnect();
    }

    public void openSession(String userName, final String password,
            String remoteHost, String keyFilePath) throws Exception {
        JSch jsch = new JSch();
        jsch.addIdentity(keyFilePath);
        session = jsch.getSession(userName, remoteHost, 22);
        session.setUserInfo(new UserInfo() {
            public String getPassphrase() {
                return null;
            }

            public String getPassword() {
                return password;
            }

            public boolean promptPassphrase(String arg0) {
                return true;
            }

            public boolean promptPassword(String arg0) {
                return true;
            }

            public boolean promptYesNo(String arg0) {
                return true;
            }

            public void showMessage(String arg0) {
            }
        });
        session.connect();
    }

    public static void deploy(String userName, final String password,
            String remoteHost, String localFilePath, String remoteFilePath,
            String keyFilePath, String preUpload, String postUpload, ProgressMonitor monitor) throws Exception {
        SCPFileUpload upload = new SCPFileUpload();
        upload.openSession(userName, password, remoteHost, keyFilePath);
        File file = new File(localFilePath);
        if (preUpload != null && preUpload.length() > 0) {
            upload.exec(preUpload.replace("$RFILE", remoteFilePath + "/" + file.getName()), monitor);
        }
        monitor.info(String.format("Uploading %s to %s", localFilePath, remoteFilePath));
        upload.uploadFile(localFilePath, remoteFilePath,
                monitor);
        //upload.exec("touch " + remoteFilePath + "/" + file.getName());
        if (postUpload != null && postUpload.length() > 0) {
            upload.exec(postUpload.replace("$RFILE", remoteFilePath + "/" + file.getName()), monitor);
        }
        upload.close();
    }

    private String uploadFile(String localFilePath, String remoteFilePath,
            ProgressMonitor monitor) throws Exception {
        FileInputStream fis = null;

        boolean ptimestamp = true;

        // exec 'scp -t rfile' remotely
        String command = "scp " + (ptimestamp ? "-p" : "") + " -t "
                + remoteFilePath;
        Channel channel = session.openChannel("exec");
        ((ChannelExec) channel).setCommand(command);

        // get I/O streams for remote scp
        OutputStream out = channel.getOutputStream();
        InputStream in = channel.getInputStream();

        channel.connect();

        if (checkAck(in) != 0) {
            return null;
        }

        File _lfile = new File(localFilePath);

        if (ptimestamp) {
            command = "T " + (_lfile.lastModified() / 1000) + " 0";
            // The access time should be sent here,
            // but it is not accessible with JavaAPI ;-<
            command += (" " + (_lfile.lastModified() / 1000) + " 0\n");
            out.write(command.getBytes());
            out.flush();
            if (checkAck(in) != 0) {
                throw (new Exception());
            }
        }

        // send "C0644 filesize filename", where filename should not include
        // '/'
        long filesize = _lfile.length();
        command = "C0644 " + filesize + " " + _lfile.getName();
        command += "\n";
        out.write(command.getBytes());
        out.flush();
        if (checkAck(in) != 0) {
            throw (new Exception());
        }

        // send a content of lfile
        long sent = 0;
        long sentLastSecond = 0;
        long startTime = System.currentTimeMillis();
        long startTimeOfLastSecond = System.currentTimeMillis();
        fis = new FileInputStream(localFilePath);
        byte[] buf = new byte[1024];
        while (true) {
            int len = fis.read(buf, 0, buf.length);
            if (len <= 0) {
                break;
            }
            sent += len;
            out.write(buf, 0, len); // out.flush();

            sentLastSecond += len;
            long t2 = System.currentTimeMillis();
            if (t2 - startTimeOfLastSecond > 1000) {
                long speed = 1000 * sentLastSecond
                        / (System.currentTimeMillis() - startTimeOfLastSecond);
                if (monitor != null) {
                    monitor.progress(filesize, sent, speed);
                }
                startTimeOfLastSecond = t2;
                sentLastSecond = 0;
            }
        }
        long speed = 1000 * sent
                / (System.currentTimeMillis() - startTime);
        monitor.progress(filesize, sent, speed);
        fis.close();
        fis = null;
        // send '\0'
        buf[0] = 0;
        out.write(buf, 0, 1);
        out.flush();
        if (checkAck(in) != 0) {
            throw (new Exception());
        }
        out.close();

        channel.disconnect();
        return _lfile.getName();
    }

    private void close() {
        session.disconnect();
    }

    private static int checkAck(InputStream in) throws IOException {
        int b = in.read();
        // b may be 0 for success,
        // 1 for error,
        // 2 for fatal error,
        // -1
        if (b == 0) {
            return b;
        }
        if (b == -1) {
            return b;
        }

        if (b == 1 || b == 2) {
            StringBuffer sb = new StringBuffer();
            int c;
            do {
                c = in.read();
                sb.append((char) c);
            } while (c != '\n');
            if (b == 1) { // error
                System.out.print(sb.toString());
            }
            if (b == 2) { // fatal error
                System.out.print(sb.toString());
            }
        }
        return b;
    }
}
