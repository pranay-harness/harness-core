package io.harness.event.grpc;

import static java.util.Objects.requireNonNull;

import com.google.inject.Inject;

import io.grpc.Context;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import io.harness.event.EventPublisherGrpc;
import io.harness.event.PublishRequest;
import io.harness.event.PublishResponse;
import io.harness.grpc.auth.DelegateAuthCallCredentials;
import io.harness.grpc.utils.AnyUtils;
import io.harness.persistence.HPersistence;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class EventPublisherServerImpl extends EventPublisherGrpc.EventPublisherImplBase {
  private final HPersistence hPersistence;

  @Inject
  public EventPublisherServerImpl(HPersistence hPersistence) {
    this.hPersistence = hPersistence;
  }

  @Override
  public void publish(PublishRequest request, StreamObserver<PublishResponse> responseObserver) {
    logger.info("Received publish request");
    String accountId = requireNonNull(DelegateAuthCallCredentials.ACCOUNT_ID_CTX_KEY.get(Context.current()));
    List<PublishedMessage> publishedMessages = request.getMessagesList()
                                                   .stream()
                                                   .map(publishMessage
                                                       -> PublishedMessage.builder()
                                                              .accountId(accountId)
                                                              .data(publishMessage.getPayload().toByteArray())
                                                              .type(AnyUtils.toFqcn(publishMessage.getPayload()))
                                                              .attributes(publishMessage.getAttributesMap())
                                                              .build())
                                                   .collect(Collectors.toList());
    try {
      hPersistence.save(publishedMessages);
    } catch (Exception e) {
      logger.warn("Encountered error while persisting messages", e);
      responseObserver.onError(Status.INTERNAL.withCause(e).asException());
      return;
    }
    logger.info("Published messages persisted");
    responseObserver.onNext(PublishResponse.newBuilder().build());
    responseObserver.onCompleted();
  }
}
