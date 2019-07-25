package io.harness.publisher;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;

import io.grpc.CallCredentials;
import io.grpc.Channel;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContext;
import io.grpc.netty.shaded.io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.harness.event.EventPublisherGrpc;
import io.harness.event.EventPublisherGrpc.EventPublisherBlockingStub;
import io.harness.event.client.EventPublisher;
import io.harness.event.client.EventPublisherImpl;
import io.harness.grpc.auth.DelegateAuthCallCredentials;
import io.harness.security.ServiceTokenGenerator;

import javax.net.ssl.SSLException;

public class PublisherModule extends AbstractModule {
  private final String publishTarget;
  private final String accountId;
  private final String accountSecret;

  public PublisherModule(String publishTarget, String accountId, String accountSecret) {
    this.publishTarget = publishTarget;
    this.accountId = accountId;
    this.accountSecret = accountSecret;
  }

  @Override
  protected void configure() {
    SslContext sslContext;
    try {
      sslContext = GrpcSslContexts.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build();
    } catch (SSLException e) {
      throw new RuntimeException(e);
    }
    Channel channel = NettyChannelBuilder.forTarget(publishTarget).sslContext(sslContext).build();
    ServiceTokenGenerator tokenGenerator = new ServiceTokenGenerator();
    CallCredentials callCredentials = new DelegateAuthCallCredentials(tokenGenerator, accountId, true);
    EventPublisherBlockingStub blockingStub =
        EventPublisherGrpc.newBlockingStub(channel).withCallCredentials(callCredentials);
    bind(EventPublisher.class).toProvider(() -> new EventPublisherImpl(blockingStub)).in(Singleton.class);
  }
}
