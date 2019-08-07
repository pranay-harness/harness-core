package io.harness.event.client;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import io.grpc.CallCredentials;
import io.grpc.Channel;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContext;
import io.grpc.netty.shaded.io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.harness.event.EventPublisherGrpc;
import io.harness.event.EventPublisherGrpc.EventPublisherBlockingStub;
import io.harness.grpc.auth.DelegateAuthCallCredentials;
import io.harness.grpc.auth.EventServiceTokenGenerator;
import lombok.SneakyThrows;
import net.openhft.chronicle.queue.ChronicleQueue;
import net.openhft.chronicle.queue.RollCycles;
import net.openhft.chronicle.queue.impl.RollingChronicleQueue;

import java.util.Objects;

public class PublisherModule extends AbstractModule {
  private final String publishTarget;
  private final String accountId;
  private final String queueFilePath;

  public PublisherModule(String publishTarget, String accountId, String queueFilePath) {
    this.publishTarget = Objects.requireNonNull(publishTarget);
    this.accountId = Objects.requireNonNull(accountId);
    this.queueFilePath = Objects.requireNonNull(queueFilePath);
  }

  @Override
  protected void configure() {
    bind(EventPublisher.class).to(EventPublisherChronicleImpl.class);
    bind(FileDeletionManager.class);
  }

  @Provides
  @Singleton
  RollingChronicleQueue chronicleQueue(FileDeletionManager fileDeletionManager) {
    return ChronicleQueue.singleBuilder(queueFilePath)
        .rollCycle(RollCycles.MINUTELY)
        .storeFileListener(fileDeletionManager)
        .build();
  }

  @Provides
  CallCredentials callCredentials(EventServiceTokenGenerator eventServiceTokenGenerator) {
    return new DelegateAuthCallCredentials(eventServiceTokenGenerator, accountId, true);
  }

  @Provides
  @Singleton
  @SneakyThrows
  Channel channel() {
    SslContext sslContext = GrpcSslContexts.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build();
    return NettyChannelBuilder.forTarget(publishTarget).sslContext(sslContext).build();
  }

  @Provides
  @Singleton
  EventPublisherBlockingStub eventPublisherBlockingStub(Channel channel, CallCredentials callCredentials) {
    return EventPublisherGrpc.newBlockingStub(channel).withCallCredentials(callCredentials);
  }
}
