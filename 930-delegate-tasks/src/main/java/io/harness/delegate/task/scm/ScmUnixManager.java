package io.harness.delegate.task.scm;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.UUIDGenerator;

import io.grpc.ManagedChannel;
import io.grpc.netty.NettyChannelBuilder;
import io.netty.channel.kqueue.KQueueDomainSocketChannel;
import io.netty.channel.kqueue.KQueueEventLoopGroup;
import io.netty.channel.unix.DomainSocketAddress;
import java.io.File;
import java.io.IOException;

@OwnedBy(HarnessTeam.DX)
public class ScmUnixManager {
  private final String PATH_TO_SCM_BUILD = "./client-tools/harness-pywinrm/versionofscm/scm.binary";

  private String socketAddress;
  private ProcessBuilder processBuilder;
  private Process server;

  public ScmUnixManager() throws IOException {
    socketAddress = "/tmp/" + UUIDGenerator.generateUuid();
    processBuilder = new ProcessBuilder();
    runServer();
  }

  public ManagedChannel getChannel() {
    KQueueEventLoopGroup klg = new KQueueEventLoopGroup();
    return NettyChannelBuilder.forAddress(new DomainSocketAddress(socketAddress))
        .eventLoopGroup(klg)
        .channelType(KQueueDomainSocketChannel.class)
        .usePlaintext()
        .build();
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