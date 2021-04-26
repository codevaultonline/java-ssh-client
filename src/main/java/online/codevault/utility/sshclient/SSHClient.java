package online.codevault.utility.sshclient;

import com.jcraft.jsch.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class SSHClient {

    private Session session;

    public SSHClient(String hostname, String username, String password, boolean strictHostChecking) throws JSchException {
        this(hostname, username, password, null, strictHostChecking);
    }

    public SSHClient(String hostname, String username, String password, File certificate, boolean strictHostChecking) throws JSchException {

        JSch jsch = new JSch();

        if (null != certificate) {
            jsch.addIdentity(certificate.getPath());
        }

        java.util.Properties config = new java.util.Properties();

        if (!strictHostChecking) {
            config.put("StrictHostKeyChecking", "no");
        }

        this.session = jsch.getSession(username, hostname);

        if (null != password) {
            this.session.setPassword(password);
        }

        this.session.setConfig(config);
        this.session.connect();

    }

    public boolean sendCommand(String command) throws JSchException, IOException {
        return sendCommand(command, null);
    }

    public boolean sendCommand(String command, String sudoPassword) throws JSchException, IOException {

        Channel channel = this.session.openChannel("exec");

        //http://www.jcraft.com/jsch/examples/Sudo.java

        if (null != sudoPassword) {
            ((ChannelExec) channel).setCommand("sudo -kS -p '' " + command);
        } else {
            ((ChannelExec) channel).setCommand(command);
        }

        channel.setInputStream(null);

        InputStream in = channel.getInputStream();

        channel.connect();

        if (null != sudoPassword) {
            OutputStream out = channel.getOutputStream();
            out.write((sudoPassword + "\n").getBytes());
            out.flush();
        }

        byte[] tmp = new byte[1024];

        boolean result = false;

        while (true) {

            while (in.available() > 0) {
                int i = in.read(tmp, 0, 1024);
                if (i < 0) break;
                System.out.print(new String(tmp, 0, i));
            }

            if (channel.isClosed()) {
                if (in.available() > 0) continue;
                result = 0 == channel.getExitStatus();
                break;
            }

            try {
                Thread.sleep(1000);
            } catch (Exception ee) {
            }

        }

        channel.disconnect();

        return result;

    }

    public void transferFile(InputStream is, String remotePath) throws SftpException, JSchException {
        ChannelSftp sftpChannel = (ChannelSftp) session.openChannel("sftp");
        sftpChannel.connect();
        sftpChannel.put(is, remotePath);
        sftpChannel.disconnect();
    }

    public String sendCommandGetResponse(String command) throws JSchException, IOException {
        return sendCommandGetResponse(command, null);
    }

    public String sendCommandGetResponse(String command, String sudoPassword) throws JSchException, IOException {

        Channel channel = this.session.openChannel("exec");

        if (null != sudoPassword) {
            ((ChannelExec) channel).setCommand("sudo -kS -p '' " + command);
        } else {
            ((ChannelExec) channel).setCommand(command);
        }

        InputStream in = channel.getInputStream();
        OutputStream out = channel.getOutputStream();

        channel.connect();

        if (null != sudoPassword) {
            out.write((sudoPassword + "\n").getBytes());
            out.flush();
        }

        byte[] tmp = new byte[1024];

        StringBuilder result = new StringBuilder();

        while (true) {

            while (in.available() > 0) {
                int i = in.read(tmp, 0, 1024);
                if (i < 0) break;
                result.append(new String(tmp, 0, i));
            }

            if (channel.isClosed()) {
                if (in.available() > 0) continue;
                if (0 != channel.getExitStatus()) {
                    throw new RuntimeException("Command Error");
                }
                break;
            }

            try {
                Thread.sleep(1000);
            } catch (Exception ee) {
            }

        }

        channel.disconnect();

        return result.toString();

    }

    public void disconnect() {
        this.session.disconnect();
    }

}
