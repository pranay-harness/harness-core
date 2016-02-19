package software.wings.core.ssh.executors;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import software.wings.exception.WingsException;

import java.io.*;

import static software.wings.beans.ErrorConstants.UNKNOWN_ERROR_CODE;
import static software.wings.beans.ErrorConstants.UNKNOWN_ERROR_MEG;
import static software.wings.core.ssh.executors.SSHExecutor.ExecutionResult.FAILURE;
import static software.wings.core.ssh.executors.SSHExecutor.ExecutionResult.SUCCESS;
import static software.wings.utils.Misc.quietSleep;

/**
 * Created by anubhaw on 2/18/16.
 */
public class SCPExecutor extends AbstractSSHExecutor {
  @Override
  public Session getSession(SSHSessionConfig config) throws JSchException {
    return SSHSessionFactory.getSSHSession(config);
  }

  public ExecutionResult transfer(String localFilePath, String remoteFilePath) {
    FileInputStream fis = null;
    try {
      String command = "scp -t " + remoteFilePath;
      Channel channel = session.openChannel("exec");
      ((ChannelExec) channel).setCommand(command);

      // get I/O streams for remote scp
      OutputStream out = channel.getOutputStream();
      InputStream in = channel.getInputStream();
      channel.connect();

      if (checkAck(in) != 0) {
        LOGGER.error("SCP connection initiation failed");
        return FAILURE;
      }

      File _lfile = new File(localFilePath);

      // send "C0644 filesize filename", where filename should not include '/'
      long filesize = _lfile.length();
      command = "C0644 " + filesize + " ";
      if (localFilePath.lastIndexOf('/') > 0) {
        command += localFilePath.substring(localFilePath.lastIndexOf('/') + 1);
      } else {
        command += localFilePath;
      }
      command += "\n";
      out.write(command.getBytes());
      out.flush();
      if (checkAck(in) != 0) {
        return FAILURE;
      }

      // send a content of lfile
      fis = new FileInputStream(localFilePath);
      byte[] buf = new byte[1024];
      while (true) {
        int len = fis.read(buf, 0, buf.length);
        if (len <= 0)
          break;
        out.write(buf, 0, len); // out.flush();
      }
      fis.close();
      fis = null;
      // send '\0'
      buf[0] = 0;
      out.write(buf, 0, 1);
      out.flush();

      if (checkAck(in) != 0) {
        LOGGER.error("SCP connection initiation failed");
        return FAILURE;
      }
      out.close();

      channel.disconnect();
      session.disconnect();
    } catch (FileNotFoundException ex) {
      LOGGER.error("file [" + localFilePath + "] could not be found");
      throw new WingsException(UNKNOWN_ERROR_CODE, UNKNOWN_ERROR_MEG, ex.getCause());
    } catch (IOException e) {
      LOGGER.error("Exception in reading InputStream");
      throw new WingsException(UNKNOWN_ERROR_CODE, UNKNOWN_ERROR_MEG, e.getCause());
    } catch (JSchException e) {
      SSHException shEx = extractSSHException(e);
      LOGGER.error("Command execution failed with error " + e.getMessage());
      throw new WingsException(shEx.getCode(), shEx.getMsg(), e.getCause());
    } finally {
      try {
        if (fis != null)
          fis.close();
      } catch (Exception ignored) {
      }
    }
    return SUCCESS;
  }

  int checkAck(InputStream in) throws IOException {
    int b = in.read();
    // b may be 0 for success,
    //          1 for error,
    //          2 for fatal error,
    //          -1
    if (b == 0)
      return b;
    else if (b == -1)
      return b;
    else { // error or echoed string on session initiation from remote host
      StringBuilder sb = new StringBuilder();
      if (b > 2) {
        sb.append((char) b);
      }

      int c;
      do {
        c = in.read();
        sb.append((char) c);
      } while (c != '\n');

      if (b <= 2) {
        throw new WingsException(UNKNOWN_ERROR_CODE, UNKNOWN_ERROR_MEG, new Throwable(sb.toString()));
      }
      LOGGER.error(sb.toString());
      return 0;
    }
  }
}
