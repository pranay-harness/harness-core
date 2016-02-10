// package software.wings.core.executors;
//
// import com.jcraft.jsch.*;
// import org.slf4j.*;
// import software.wings.core.executors.callbacks.SSHCommandExecutionCallback;
//
// import java.io.*;
//
// import static software.wings.utils.Misc.quietSleep;
//
///**
// * Created by anubhaw on 2/4/16.
// */
//
//
// public class SSHSudoCommandExecutor implements Executor {
//
//    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(SSHCommandExecutor.class);
//    private String hostName;
//    private String sshUser;
//    private String sshPassword;
//    private String command;
//    private SSHCommandExecutionCallback callback;
//    private Session session;
//    private Channel channel;
//
//    private int sshPort = 22;
//
//    public SSHSudoCommandExecutor(String hostName, int sshPort, String sshUser, String sshPassword, String command,
//    SSHCommandExecutionCallback callback) {
//        this.hostName = hostName;
//        this.sshPort = sshPort;
//        this.sshUser = sshUser;
//        this.sshPassword = sshPassword;
//        this.command = command;
//        this.callback = callback;
//    }
//    public void execute() {
//        try {
//            JSch jsch=new JSch();
//            session=jsch.getSession(sshUser, hostName, sshPort);
//            UserInfo ui=new MyUserInfo(sshPassword);
//            session.setUserInfo(ui);
//
//            callback.log("Trying to connect over ssh");
//            session.connect(SSHConnectionTimeout);
//            session.setTimeout(SSHSessionTimeout);
//            callback.log("Connection established... going to execute command : " + command);
//            channel=session.openChannel("exec");
//            ((ChannelExec)channel).setCommand(command);
//            ((ChannelExec)channel).setErrStream(System.err);
//
//            ByteArrayOutputStream os = new ByteArrayOutputStream();
//            channel.setOutputStream(os);
//            OutputStream out = channel.getOutputStream();
//
//            ((ChannelExec) channel).setPty(true);
//            channel.connect();
//            out.write((ui.getPassword() + "\n").getBytes());
//            out.flush();
//
//            Thread thread = new Thread(()-> {
//                            while (!channel.isClosed()) {
//                                try {
//                                    quietSleep(RetryInterval);
//                                } catch (Exception e) {
//                                    // ignored
//                                }
//                            }
//                    });
//            thread.start();
//            thread.join(SSHSessionTimeout);
//
//            if (thread.isAlive()) {
//                callback.log("Command couldn't complete in time. Connection closed");
//            } else {
//                callback.log("[" + new String(os.toByteArray(), "UTF-8") + "]");
//                int ec = channel.getExitStatus();
//                if (ec != 0) {
//                    callback.log("Remote command failed with exit status " + ec);
//                }
//            }
//        } catch (JSchException | IOException e) {
//            e.printStackTrace();
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        } finally {
//            if (null != channel) {
//                channel.disconnect();
//            }
//            if (null != session) {
//                session.disconnect();
//            }
//        }
//    }
//
//    @Override
//    public void abort() {
//
//    }
//}
