/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
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

    public static void send(String userName, final String password,
            String remoteHost, String localFilePath, String remoteFilePath,
            String keyFilePath, ProgressMonitor monitor) throws Exception {
        FileInputStream fis = null;

        JSch jsch = new JSch();
        jsch.addIdentity(keyFilePath);
        Session session = jsch.getSession(userName, remoteHost, 22);
        session.setUserInfo(new UserInfo() {
            @Override
            public String getPassphrase() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public String getPassword() {
                return password;
            }

            @Override
            public boolean promptPassphrase(String arg0) {
                return true;
            }

            @Override
            public boolean promptPassword(String arg0) {
                return true;
            }

            @Override
            public boolean promptYesNo(String arg0) {
                return true;
            }

            @Override
            public void showMessage(String arg0) {
            }
        });
        session.connect();

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
            return;
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
                return;
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
            return;
        }

        // send a content of lfile
        long sent = 0;
        long sentLastSecond = 0;
        long startTime = System.currentTimeMillis();
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
            if (t2 - startTime > 1000) {
                long speed = 1000 * sentLastSecond
                        / (System.currentTimeMillis() - startTime);
                if (monitor != null) {
                    monitor.progress(filesize, sent, speed);
                }
                startTime = t2;
                sentLastSecond = 0;
            }
        }
        fis.close();
        fis = null;
        // send '\0'
        buf[0] = 0;
        out.write(buf, 0, 1);
        out.flush();
        if (checkAck(in) != 0) {
            return;
        }
        out.close();

        channel.disconnect();
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
