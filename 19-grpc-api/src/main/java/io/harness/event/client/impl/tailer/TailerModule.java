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
import io.harness.event.client.impl.EventPublisherConstants;
import io.harness.flow.BackoffScheduler;
import io.harness.govern.ProviderModule;
import io.harness.grpc.auth.DelegateAuthCallCredentials;
import io.harness.security.TokenGenerator;
import lombok.Builder;
import lombok.SneakyThrows;
import lombok.Value;
import net.openhft.chronicle.queue.ChronicleQueue;
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
  RollingChronicleQueue chronicleQueue() {
    return ChronicleQueue.singleBuilder(config.queueFilePath)
        .rollCycle(EventPublisherConstants.QUEUE_ROLL_CYCLE)
        .timeoutMS(EventPublisherConstants.QUEUE_TIMEOUT_MS)
        .build();
  }

  @Provides
  @Singleton
  CallCredentials callCredentials() {
    return new DelegateAuthCallCredentials(
        new TokenGenerator(config.accountId, config.accountSecret), config.accountId, true);
  }

  @Provides
  @Singleton
  @Named("tailer")
  BackoffScheduler backoffScheduler() {
    Duration minDelay = Duration.ofSeconds(1);
    Duration maxDelay = Duration.ofMinutes(5);
    return new BackoffScheduler(ChronicleEventTailer.class.getSimpleName(), minDelay, maxDelay);
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
    String publishTarget;
    String publishAuthority;
    String accountId;
    String accountSecret;
    String queueFilePath;
  }
}
