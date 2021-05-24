package io.harness.delegate.task.scm;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.UUIDGenerator;
import io.harness.delegate.configuration.InstallUtils;

import io.grpc.ManagedChannel;
import java.io.File;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(HarnessTeam.DX)
public abstract class ScmUnixManager implements AutoCloseable {
  abstract ManagedChannel getChannel();
  private final String PATH_TO_SCM_BUILD = InstallUtils.getScmFolderPath();

  protected String socketAddress;
  protected ProcessBuilder processBuilder;
  protected Process server;

  public ScmUnixManager() throws IOException {
    socketAddress = "/tmp/" + UUIDGenerator.generateUuid();
    processBuilder = new ProcessBuilder();
    runServer();
  }

  public void close() throws IOException {
    server.destroy();
    processBuilder.command("rm", "-rf", socketAddress);
    Process process = processBuilder.start();
    process.destroy();
  }

  private void runServer() throws IOException {
    processBuilder.directory(new File(PATH_TO_SCM_BUILD));
    processBuilder.command("./" + InstallUtils.getScmBinary(), "--unix", socketAddress);
    log.info("Running SCM server at path: {} on port: {}", PATH_TO_SCM_BUILD, socketAddress);
    server = processBuilder.start();
  }
}
