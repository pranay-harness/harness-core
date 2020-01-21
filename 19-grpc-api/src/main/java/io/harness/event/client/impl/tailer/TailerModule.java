package io.harness.event.client.impl.tailer;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import io.grpc.CallCredentials;
import io.grpc.Channel;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContext;
import io.grpc.netty.shaded.io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.harness.event.EventPublisherGrpc;
import io.harness.event.EventPublisherGrpc.EventPublisherBlockingStub;
import io.harness.govern.ProviderModule;
import io.harness.grpc.auth.DelegateAuthCallCredentials;
import io.harness.security.TokenGenerator;
import lombok.Builder;
import lombok.SneakyThrows;
import lombok.Value;
import net.openhft.chronicle.queue.ChronicleQueue;
import net.openhft.chronicle.queue.RollCycles;
import net.openhft.chronicle.queue.impl.RollingChronicleQueue;

import java.time.Duration;

public class TailerModule extends ProviderModule {
  private final Config config;

  public TailerModule(Config config) {
    this.config = config;
  }

  @Provides
  @Singleton
  @Named("tailer")
  RollingChronicleQueue chronicleQueue(FileDeletionManager fileDeletionManager) {
    return ChronicleQueue.singleBuilder(config.queueFilePath)
        .rollCycle(RollCycles.MINUTELY)
        .timeoutMS(Duration.ofSeconds(30).toMillis())
        .storeFileListener(fileDeletionManager)
        .build();
  }

  @Provides
  @Singleton
  CallCredentials callCredentials() {
    return new DelegateAuthCallCredentials(
        new TokenGenerator(config.accountId, config.accountSecret), config.accountId, true);
  }

  @Named("event-server-channel")
  @Provides
  @Singleton
  @SneakyThrows
  Channel channel() {
    SslContext sslContext = GrpcSslContexts.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build();
    return NettyChannelBuilder.forTarget(config.publishTarget)
        .overrideAuthority(config.publishAuthority)
        .sslContext(sslContext)
        .build();
  }

  @Provides
  @Singleton
  EventPublisherBlockingStub eventPublisherBlockingStub(
      @Named("event-server-channel") Channel channel, CallCredentials callCredentials) {
    return EventPublisherGrpc.newBlockingStub(channel).withCallCredentials(callCredentials);
  }

  @Value
  @Builder
  public static class Config {
    private final String publishTarget;
    private final String publishAuthority;
    private final String accountId;
    private final String accountSecret;
    private final String queueFilePath;
  }
}
