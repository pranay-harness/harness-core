package io.harness.delegate.task.scm;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import io.grpc.ManagedChannel;
import io.grpc.netty.NettyChannelBuilder;
import io.netty.channel.epoll.EpollDomainSocketChannel;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.unix.DomainSocketAddress;
import java.io.IOException;

@OwnedBy(HarnessTeam.DX)
public class ScmLinuxManager extends ScmUnixManager {
  public ScmLinuxManager() throws IOException {}

  public ManagedChannel getChannel() {
    return NettyChannelBuilder.forAddress(new DomainSocketAddress(socketAddress))
        .eventLoopGroup(new EpollEventLoopGroup())
        .channelType(EpollDomainSocketChannel.class)
        .usePlaintext()
        .build();
  }
}