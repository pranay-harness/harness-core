package io.harness.publisher;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import com.squareup.tape2.ObjectQueue;
import com.squareup.tape2.QueueFile;
import io.grpc.CallCredentials;
import io.grpc.Channel;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContext;
import io.grpc.netty.shaded.io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.harness.event.EventPublisherGrpc;
import io.harness.event.EventPublisherGrpc.EventPublisherBlockingStub;
import io.harness.event.PublishMessage;
import io.harness.event.client.EventPublisher;
import io.harness.event.client.EventPublisherPersistentImpl;
import io.harness.event.client.PublishMessageConverter;
import io.harness.grpc.auth.DelegateAuthCallCredentials;
import io.harness.security.ServiceTokenGenerator;
import lombok.SneakyThrows;

import java.io.File;
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
  protected void configure() {}

  @Provides
  @Singleton
  @SneakyThrows
  ObjectQueue<PublishMessage> objectQueue() {
    File file = new File(queueFilePath);
    QueueFile queueFile = new QueueFile.Builder(file).build();
    return ObjectQueue.create(queueFile, new PublishMessageConverter());
  }

  @Provides
  CallCredentials callCredentials(ServiceTokenGenerator tokenGenerator) {
    return new DelegateAuthCallCredentials(tokenGenerator, accountId, true);
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

  @Provides
  @Singleton
  EventPublisher eventPublisher(EventPublisherBlockingStub blockingStub, ObjectQueue<PublishMessage> objectQueue) {
    EventPublisherPersistentImpl eventPublisher = new EventPublisherPersistentImpl(blockingStub, objectQueue);
    Runtime.getRuntime().addShutdownHook(new Thread(eventPublisher::shutdown));
    return eventPublisher;
  }
}
