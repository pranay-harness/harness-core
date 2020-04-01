package io.harness.event.grpc;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.grpc.IdentifierKeys.DELEGATE_ID;
import static io.harness.grpc.auth.DelegateAuthServerInterceptor.ACCOUNT_ID_CTX_KEY;
import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;
import static java.util.Objects.requireNonNull;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.grpc.Context;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import io.harness.delegate.task.DelegateLogContext;
import io.harness.event.EventPublisherGrpc;
import io.harness.event.MessageProcessorType;
import io.harness.event.PublishMessage;
import io.harness.event.PublishRequest;
import io.harness.event.PublishResponse;
import io.harness.event.service.intfc.LastReceivedPublishedMessageRepository;
import io.harness.grpc.utils.AnyUtils;
import io.harness.grpc.utils.HTimestamps;
import io.harness.logging.AutoLogContext;
import io.harness.persistence.AccountLogContext;
import io.harness.persistence.HPersistence;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Singleton
public class EventPublisherServerImpl extends EventPublisherGrpc.EventPublisherImplBase {
  private final HPersistence hPersistence;
  private final LastReceivedPublishedMessageRepository lastReceivedPublishedMessageRepository;
  private final MessageProcessorRegistry messageProcessorRegistry;

  @Inject
  public EventPublisherServerImpl(HPersistence hPersistence,
      LastReceivedPublishedMessageRepository lastReceivedPublishedMessageRepository,
      MessageProcessorRegistry messageProcessorRegistry) {
    this.hPersistence = hPersistence;
    this.lastReceivedPublishedMessageRepository = lastReceivedPublishedMessageRepository;
    this.messageProcessorRegistry = messageProcessorRegistry;
  }

  @Override
  public void publish(PublishRequest request, StreamObserver<PublishResponse> responseObserver) {
    String accountId = requireNonNull(ACCOUNT_ID_CTX_KEY.get(Context.current()));
    String delegateId = request.getMessages(0).getAttributesMap().getOrDefault(DELEGATE_ID, "");
    try (AutoLogContext ignore = new AccountLogContext(accountId, OVERRIDE_ERROR);
         AutoLogContext ignore1 = new DelegateLogContext(delegateId, OVERRIDE_ERROR)) {
      logger.info("Received publish request with {} messages", request.getMessagesCount());

      List<PublishedMessage> withoutCategory = new ArrayList<>();
      List<PublishedMessage> withCategory = new ArrayList<>();
      request.getMessagesList()
          .stream()
          .map(publishMessage -> toPublishedMessage(accountId, publishMessage))
          .forEach(publishedMessage -> {
            if (isEmpty(publishedMessage.getCategory())) {
              withoutCategory.add(publishedMessage);
            } else {
              withCategory.add(publishedMessage);
            }
          });

      try {
        hPersistence.saveIgnoringDuplicateKeys(withoutCategory);
      } catch (Exception e) {
        logger.warn("Encountered error while persisting messages", e);
        responseObserver.onError(Status.INTERNAL.withCause(e).asException());
        return;
      }

      try {
        lastReceivedPublishedMessageRepository.updateLastReceivedPublishedMessages(withoutCategory);
      } catch (Exception e) {
        logger.warn("Error while persisting last received data", e);
      }

      try {
        withCategory.forEach(publishedMessage -> {
          MessageProcessor processor =
              messageProcessorRegistry.getProcessor(MessageProcessorType.valueOf(publishedMessage.getCategory()));
          processor.process(publishedMessage);
        });
      } catch (Exception e) {
        logger.warn("Error while processing messages", e);
      }

      logger.info("Published messages persisted");
      responseObserver.onNext(PublishResponse.newBuilder().build());
      responseObserver.onCompleted();
    }
  }

  public PublishedMessage toPublishedMessage(String accountId, PublishMessage publishMessage) {
    return PublishedMessage.builder()
        .uuid(StringUtils.defaultIfEmpty(publishMessage.getMessageId(), generateUuid()))
        .accountId(accountId)
        .data(publishMessage.getPayload().toByteArray())
        .type(AnyUtils.toFqcn(publishMessage.getPayload()))
        .attributes(publishMessage.getAttributesMap())
        .category(publishMessage.getCategory())
        .occurredAt(HTimestamps.toMillis(publishMessage.getOccurredAt()))
        .build();
  }
}
