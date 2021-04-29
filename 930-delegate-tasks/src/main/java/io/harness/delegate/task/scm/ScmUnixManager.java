package io.harness.delegate.task.scm;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.UUIDGenerator;

import io.grpc.ManagedChannel;
import java.io.File;
import java.io.IOException;

@OwnedBy(HarnessTeam.DX)
public abstract class ScmUnixManager implements AutoCloseable {
  abstract ManagedChannel getChannel();
  private final String PATH_TO_SCM_BUILD = "./client-tools/harness-pywinrm/versionofscm/scm.binary";

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
    processBuilder.command("./scm", "--unix", socketAddress);
    server = processBuilder.start();
  }
}
