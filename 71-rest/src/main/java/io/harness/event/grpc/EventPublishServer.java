package io.harness.event.grpc;

import static java.util.Objects.requireNonNull;

import com.google.inject.Inject;

import io.grpc.Context;
import io.grpc.stub.StreamObserver;
import io.harness.event.EventPublisherGrpc;
import io.harness.event.PublishRequest;
import io.harness.event.PublishResponse;
import io.harness.event.grpc.auth.DelegateAuthCallCredentials;
import lombok.extern.slf4j.Slf4j;
import software.wings.dl.WingsPersistence;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class EventPublishServer extends EventPublisherGrpc.EventPublisherImplBase {
  private final WingsPersistence wingsPersistence;

  @Inject
  public EventPublishServer(WingsPersistence wingsPersistence) {
    this.wingsPersistence = wingsPersistence;
  }

  @Override
  public void publish(PublishRequest request, StreamObserver<PublishResponse> responseObserver) {
    String accountId = requireNonNull(DelegateAuthCallCredentials.ACCOUNT_ID_CTX_KEY.get(Context.current()));
    List<PublishedMessage> publishedMessages =
        request.getMessagesList()
            .stream()
            .map(publishMessage
                -> PublishedMessage.builder()
                       .accountId(accountId)
                       .data(publishMessage.getPayload().toByteArray())
                       .type(getClassNameFromTypeUrl(publishMessage.getPayload().getTypeUrl()))
                       .attributes(publishMessage.getAttributesMap())
                       .build())
            .collect(Collectors.toList());
    try {
      wingsPersistence.save(publishedMessages);
      responseObserver.onNext(PublishResponse.newBuilder().build());
    } catch (Exception e) {
      logger.warn("Encountered error while persisting messages", e);
      responseObserver.onError(e);
      return;
    }
    responseObserver.onCompleted();
  }

  private String getClassNameFromTypeUrl(String typeUrl) {
    return typeUrl.split("/")[1];
  }
}
