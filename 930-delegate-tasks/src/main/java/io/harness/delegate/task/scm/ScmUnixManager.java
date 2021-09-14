/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.task.scm;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.UUIDGenerator;
import io.harness.delegate.configuration.InstallUtils;

import io.grpc.ManagedChannel;
import java.io.File;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.StartedProcess;

@Slf4j
@OwnedBy(HarnessTeam.DX)
public abstract class ScmUnixManager implements AutoCloseable {
  abstract ManagedChannel getChannel();

  private final String PATH_TO_SCM_BUILD = InstallUtils.getScmFolderPath();

  protected String socketAddress;
  protected ProcessExecutor processBuilder;
  protected StartedProcess server;

  public ScmUnixManager() throws IOException {
    socketAddress = "/tmp/" + UUIDGenerator.generateUuid();
    processBuilder = new ProcessExecutor();
    runServer();
  }

  public void close() throws IOException {
    server.getProcess().destroy();
    processBuilder.command("rm", "-rf", socketAddress);
    final StartedProcess process = processBuilder.start();
    process.getProcess().destroy();
  }

  private void runServer() throws IOException {
    processBuilder.directory(new File(PATH_TO_SCM_BUILD));
    processBuilder.command("./" + InstallUtils.getScmBinary(), "--unix", socketAddress);
    log.info("Running SCM server at path: {} on port: {}", PATH_TO_SCM_BUILD, socketAddress);
    processBuilder.redirectOutput(System.out);
    processBuilder.redirectError(System.err);
    server = processBuilder.start();
  }
}
