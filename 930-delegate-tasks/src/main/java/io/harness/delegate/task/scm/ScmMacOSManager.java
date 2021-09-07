package io.harness.delegate.task.scm;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import io.grpc.ManagedChannel;
import io.grpc.netty.NettyChannelBuilder;
import io.netty.channel.ChannelOption;
import io.netty.channel.kqueue.KQueueDomainSocketChannel;
import io.netty.channel.kqueue.KQueueEventLoopGroup;
import io.netty.channel.unix.DomainSocketAddress;
import java.io.IOException;

@OwnedBy(HarnessTeam.DX)
public class ScmMacOSManager extends ScmUnixManager {
  public ScmMacOSManager() throws IOException {}

  public ManagedChannel getChannel() {
    KQueueEventLoopGroup klg = new KQueueEventLoopGroup();
    return NettyChannelBuilder.forAddress(new DomainSocketAddress(socketAddress))
        .eventLoopGroup(klg)
        .channelType(KQueueDomainSocketChannel.class)
        .usePlaintext()
        .withOption(ChannelOption.SO_KEEPALIVE, false)
        .build();
  }
}