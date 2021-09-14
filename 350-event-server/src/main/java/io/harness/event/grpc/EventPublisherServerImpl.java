/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.event.grpc;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.grpc.IdentifierKeys.DELEGATE_ID;
import static io.harness.grpc.auth.DelegateAuthServerInterceptor.ACCOUNT_ID_CTX_KEY;
import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;

import static java.util.Objects.requireNonNull;

import io.harness.ccm.commons.entities.events.PublishedMessage;
import io.harness.delegate.task.DelegateLogContext;
import io.harness.event.EventPublisherGrpc;
import io.harness.event.MessageProcessorType;
import io.harness.event.PublishMessage;
import io.harness.event.PublishRequest;
import io.harness.event.PublishResponse;
import io.harness.event.service.intfc.LastReceivedPublishedMessageRepository;
import io.harness.grpc.utils.AnyUtils;
import io.harness.grpc.utils.HTimestamps;
import io.harness.logging.AccountLogContext;
import io.harness.logging.AutoLogContext;
import io.harness.persistence.HPersistence;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.grpc.Context;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

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
      log.info("Received publish request with {} messages", request.getMessagesCount());

      List<io.harness.ccm.commons.entities.events.PublishedMessage> withoutCategory = new ArrayList<>();
      List<io.harness.ccm.commons.entities.events.PublishedMessage> withCategory = new ArrayList<>();
      request.getMessagesList()
          .stream()
          .map(publishMessage -> toPublishedMessage(accountId, publishMessage))
          .filter(Objects::nonNull)
          .forEach(publishedMessage -> {
            if (isEmpty(publishedMessage.getCategory())) {
              withoutCategory.add(publishedMessage);
            } else {
              withCategory.add(publishedMessage);
            }
          });

      if (isNotEmpty(withoutCategory)) {
        try {
          hPersistence.saveIgnoringDuplicateKeys(withoutCategory);
        } catch (Exception e) {
          log.warn("Encountered error while persisting messages", e);
          responseObserver.onError(Status.INTERNAL.withCause(e).asException());
          return;
        }
      }

      try {
        lastReceivedPublishedMessageRepository.updateLastReceivedPublishedMessages(withoutCategory);
      } catch (Exception e) {
        log.warn("Error while persisting last received data", e);
      }

      try {
        withCategory.forEach(publishedMessage -> {
          MessageProcessor processor =
              messageProcessorRegistry.getProcessor(MessageProcessorType.valueOf(publishedMessage.getCategory()));
          processor.process(publishedMessage);
        });
      } catch (Exception e) {
        log.warn("Error while processing messages", e);
      }

      log.info("Published messages persisted");
      responseObserver.onNext(PublishResponse.newBuilder().build());
      responseObserver.onCompleted();
    }
  }

  public io.harness.ccm.commons.entities.events.PublishedMessage toPublishedMessage(
      String accountId, PublishMessage publishMessage) {
    try {
      return PublishedMessage.builder()
          .uuid(StringUtils.defaultIfEmpty(publishMessage.getMessageId(), generateUuid()))
          .accountId(accountId)
          .data(publishMessage.getPayload().toByteArray())
          .type(AnyUtils.toFqcn(publishMessage.getPayload()))
          .attributes(publishMessage.getAttributesMap())
          .category(publishMessage.getCategory())
          .occurredAt(HTimestamps.toMillis(publishMessage.getOccurredAt()))
          .build();
    } catch (Exception e) {
      log.error("Error persisting message {}", publishMessage, e);
      return null;
    }
  }
}
