/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package software.wings.core.ssh.executors;

import static io.harness.filesystem.FileIo.createDirectoryIfDoesNotExist;
import static io.harness.logging.CommandExecutionStatus.FAILURE;
import static io.harness.logging.CommandExecutionStatus.SUCCESS;

import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogCallback;
import io.harness.shell.AbstractScriptExecutor;
import io.harness.shell.ScriptExecutionContext;
import io.harness.shell.ShellExecutorConfig;

import software.wings.delegatetasks.DelegateFileManager;

import com.google.inject.Inject;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.ExecutionException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;

@Slf4j
public class FileBasedProcessScriptExecutor extends FileBasedAbstractScriptExecutor {
  private ShellExecutorConfig config;

  /**
   * Instantiates a new abstract ssh executor.
   *  @param delegateFileManager the file service
   * @param logCallback          the log service
   */
  @Inject
  public FileBasedProcessScriptExecutor(DelegateFileManager delegateFileManager, LogCallback logCallback,
      boolean shouldSaveExecutionLogs, ScriptExecutionContext shellExecutorConfig) {
    super(delegateFileManager, logCallback, shouldSaveExecutionLogs);
    this.config = (ShellExecutorConfig) shellExecutorConfig;
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
    return null;
  }

  @Override
  public CommandExecutionStatus scpOneFile(String remoteFilePath, AbstractScriptExecutor.FileProvider fileProvider) {
    CommandExecutionStatus commandExecutionStatus = FAILURE;
    try {
      Pair<String, Long> fileInfo = fileProvider.getInfo();
      createDirectoryIfDoesNotExist(remoteFilePath);
      OutputStream out = new FileOutputStream(remoteFilePath + "/" + fileInfo.getKey());
      fileProvider.downloadToStream(out);
      out.flush();
      out.close();
      commandExecutionStatus = SUCCESS;
      saveExecutionLog("File successfully downloaded to " + remoteFilePath);
    } catch (ExecutionException | IOException e) {
      log.error("Command execution failed with error", e);
    }
    return commandExecutionStatus;
  }
}
