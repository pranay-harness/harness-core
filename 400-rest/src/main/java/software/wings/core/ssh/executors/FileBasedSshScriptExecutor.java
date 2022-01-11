/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package software.wings.core.ssh.executors;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.eraro.ErrorCode.ERROR_IN_GETTING_CHANNEL_STREAMS;
import static io.harness.eraro.ErrorCode.INVALID_EXECUTION_ID;
import static io.harness.logging.CommandExecutionStatus.FAILURE;
import static io.harness.logging.CommandExecutionStatus.SUCCESS;
import static io.harness.shell.ScriptSshExecutor.CHANNEL_IS_NOT_OPENED;
import static io.harness.shell.SshHelperUtils.normalizeError;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;

import io.harness.exception.SshRetryableException;
import io.harness.exception.WingsException;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogCallback;
import io.harness.shell.AbstractScriptExecutor;
import io.harness.shell.ScriptExecutionContext;
import io.harness.shell.SshSessionConfig;
import io.harness.shell.SshSessionManager;

import software.wings.delegatetasks.DelegateFileManager;

import com.google.inject.Inject;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutionException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

@Slf4j
public class FileBasedSshScriptExecutor extends FileBasedAbstractScriptExecutor {
  public static final int ALLOWED_BYTES = 1024 * 1024; // 1MB

  protected SshSessionConfig config;

  /**
   * Instantiates a new abstract ssh executor.
   *  @param delegateFileManager the file service
   * @param logCallback          the log service
   */
  @Inject
  public FileBasedSshScriptExecutor(DelegateFileManager delegateFileManager, LogCallback logCallback,
      boolean shouldSaveExecutionLogs, ScriptExecutionContext config) {
    super(delegateFileManager, logCallback, shouldSaveExecutionLogs);
    if (isEmpty(((SshSessionConfig) config).getExecutionId())) {
      throw new WingsException(INVALID_EXECUTION_ID);
    }
    this.config = (SshSessionConfig) config;
  }

  @Override
  public CommandExecutionStatus scpOneFile(String remoteFilePath, AbstractScriptExecutor.FileProvider fileProvider) {
    try {
      return scpOneFile(remoteFilePath, fileProvider, false);
    } catch (SshRetryableException ex) {
      log.info("As MaxSessions limit reached, fetching new session for executionId: {}, hostName: {}",
          config.getExecutionId(), config.getHost());
      saveExecutionLog(format("Retry connecting to %s ....", config.getHost()));
      return scpOneFile(remoteFilePath, fileProvider, true);
    }
  }

  @NotNull
  private CommandExecutionStatus scpOneFile(
      String remoteFilePath, AbstractScriptExecutor.FileProvider fileProvider, boolean isRetry) {
    CommandExecutionStatus commandExecutionStatus = FAILURE;
    Channel channel = null;
    try {
      Pair<String, Long> fileInfo = fileProvider.getInfo();
      String command = format("mkdir -p \"%s\" && scp -r -d -t '%s'", remoteFilePath, remoteFilePath);
      Session session;
      if (isRetry) {
        session = SshSessionManager.getSimplexSession(config, logCallback);
      } else {
        session = SshSessionManager.getCachedSession(config, logCallback);
      }
      channel = session.openChannel("exec");
      ((ChannelExec) channel).setCommand(command);

      // get I/O streams for remote scp
      try (OutputStream out = channel.getOutputStream(); InputStream in = channel.getInputStream()) {
        saveExecutionLog(format("Connecting to %s ....", config.getHost()));
        channel.connect(config.getSocketConnectTimeout());

        if (checkAck(in) != 0) {
          saveExecutionLogError("SCP connection initiation failed");
          return FAILURE;
        } else {
          saveExecutionLog(format("Connection to %s established", config.getHost()));
        }

        // send "C0644 filesize filename", where filename should not include '/'
        command = "C0644 " + fileInfo.getValue() + " " + fileInfo.getKey() + "\n";

        out.write(command.getBytes(UTF_8));
        out.flush();
        if (checkAck(in) != 0) {
          saveExecutionLogError("SCP connection initiation failed");
          return commandExecutionStatus;
        }
        saveExecutionLog("Begin file transfer " + fileInfo.getKey() + " to " + config.getHost() + ":" + remoteFilePath);
        fileProvider.downloadToStream(out);
        out.write(new byte[1], 0, 1);
        out.flush();

        if (checkAck(in) != 0) {
          saveExecutionLogError("File transfer to " + config.getHost() + ":" + remoteFilePath + " failed");
          return commandExecutionStatus;
        }
        commandExecutionStatus = SUCCESS;
        saveExecutionLog("File successfully transferred to " + config.getHost() + ":" + remoteFilePath);
        channel.disconnect();
      }
    } catch (IOException | ExecutionException | JSchException ex) {
      if (ex instanceof FileNotFoundException) {
        saveExecutionLogError("File not found");
      } else if (ex instanceof JSchException) {
        log.error("Command execution failed with error", ex);
        if (!isRetry && CHANNEL_IS_NOT_OPENED.equals(ex.getMessage())) {
          throw new SshRetryableException(ex);
        } else {
          saveExecutionLogError("Command execution failed with error " + normalizeError((JSchException) ex));
        }
      } else {
        throw new WingsException(ERROR_IN_GETTING_CHANNEL_STREAMS, ex);
      }
      return commandExecutionStatus;
    } finally {
      if (channel != null && !channel.isClosed()) {
        log.info("Disconnect channel if still open post execution command");
        channel.disconnect();
      }
    }
    return commandExecutionStatus;
  }

  /**
   * Check ack.
   *
   * @param in the in
   * @return the int
   * @throws IOException Signals that an I/O exception has occurred.
   */
  int checkAck(InputStream in) throws IOException {
    int b = in.read();
    // b may be 0 for success,
    //          1 for error,
    //          2 for fatal error,
    //          -1
    if (b == 0) {
      return b;
    } else if (b == -1) {
      return b;
    } else { // error or echoed string on session initiation from remote host
      StringBuilder sb = new StringBuilder();
      if (b > 2) {
        sb.append((char) b);
      }

      int c;
      int totalBytesRead = 0;
      do {
        c = in.read();
        if (c == -1) {
          break;
        }
        totalBytesRead++;
        sb.append((char) c);
      } while (c != '\n' || totalBytesRead <= ALLOWED_BYTES);

      if (b <= 2) {
        saveExecutionLogError(sb.toString());
        return 1;
      }
      log.error(sb.toString());
      return 0;
    }
  }

  @Override
  public String getAccountId() {
    return config.getAccountId();
  }

  @Override
  public String getCommandUnitName() {
    return config.getCommandUnitName();
  }

  @Override
  public String getAppId() {
    return config.getAppId();
  }

  @Override
  public String getExecutionId() {
    return config.getExecutionId();
  }

  @Override
  public String getHost() {
    return config.getHost();
  }
}
