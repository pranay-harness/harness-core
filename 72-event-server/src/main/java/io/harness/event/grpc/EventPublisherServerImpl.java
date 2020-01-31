package io.harness.event.grpc;

import static java.util.Objects.requireNonNull;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.grpc.Context;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import io.harness.event.EventPublisherGrpc;
import io.harness.event.PublishRequest;
import io.harness.event.PublishResponse;
import io.harness.event.service.intfc.LastReceivedPublishedMessageRepository;
import io.harness.grpc.auth.DelegateAuthServerInterceptor;
import io.harness.grpc.utils.AnyUtils;
import io.harness.grpc.utils.HTimestamps;
import io.harness.persistence.HPersistence;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Singleton
public class EventPublisherServerImpl extends EventPublisherGrpc.EventPublisherImplBase {
  private final HPersistence hPersistence;
  private final LastReceivedPublishedMessageRepository lastReceivedPublishedMessageRepository;

  @Inject
  public EventPublisherServerImpl(
      HPersistence hPersistence, LastReceivedPublishedMessageRepository lastReceivedPublishedMessageRepository) {
    this.hPersistence = hPersistence;
    this.lastReceivedPublishedMessageRepository = lastReceivedPublishedMessageRepository;
  }

  @Override
  public void publish(PublishRequest request, StreamObserver<PublishResponse> responseObserver) {
    logger.info("Received publish request");
    String accountId = requireNonNull(DelegateAuthServerInterceptor.ACCOUNT_ID_CTX_KEY.get(Context.current()));
    List<PublishedMessage> publishedMessages =
        request.getMessagesList()
            .stream()
            .map(publishMessage
                -> PublishedMessage.builder()
                       .accountId(accountId)
                       .data(publishMessage.getPayload().toByteArray())
                       .type(AnyUtils.toFqcn(publishMessage.getPayload()))
                       .attributes(publishMessage.getAttributesMap())
                       .occurredAt(HTimestamps.toMillis(publishMessage.getOccurredAt()))
                       .build())
            .collect(Collectors.toList());
    try {
      hPersistence.save(publishedMessages);
    } catch (Exception e) {
      logger.warn("Encountered error while persisting messages", e);
      responseObserver.onError(Status.INTERNAL.withCause(e).asException());
      return;
    }
    try {
      lastReceivedPublishedMessageRepository.updateLastReceivedPublishedMessages(publishedMessages);
    } catch (Exception e) {
      logger.warn("Error while persisting last received data", e);
    }
    logger.info("Published messages persisted");
    responseObserver.onNext(PublishResponse.newBuilder().build());
    responseObserver.onCompleted();
  }
}
