/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.event.grpc;

import static io.harness.event.payloads.Lifecycle.EventType.EVENT_TYPE_START;
import static io.harness.event.payloads.Lifecycle.EventType.EVENT_TYPE_STOP;
import static io.harness.rule.OwnerRule.AVMOHAN;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.ccm.commons.entities.events.PublishedMessage;
import io.harness.event.PublishMessage;
import io.harness.event.PublishRequest;
import io.harness.event.PublishResponse;
import io.harness.event.payloads.Lifecycle;
import io.harness.event.service.intfc.LastReceivedPublishedMessageRepository;
import io.harness.grpc.auth.DelegateAuthServerInterceptor;
import io.harness.grpc.utils.HTimestamps;
import io.harness.metrics.service.api.MetricService;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Streams;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import io.grpc.Context;
import io.grpc.Status.Code;
import io.grpc.StatusException;
import io.grpc.stub.StreamObserver;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class EventPublisherServerImplTest extends CategoryTest {
  private static final String TEST_ACC_ID = UUID.randomUUID().toString();

  @Mock private HPersistence hPersistence;
  @Mock private StreamObserver<PublishResponse> observer;
  @Mock private LastReceivedPublishedMessageRepository lastReceivedPublishedMessageRepository;
  @Mock private MessageProcessorRegistry messageProcessorRegistry;
  @Mock private MetricService metricService;

  @InjectMocks private EventPublisherServerImpl publisherServer;

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldFailIfAccountIdIsNotSet() {
    assertThatExceptionOfType(NullPointerException.class)
        .isThrownBy(() -> publisherServer.publish(PublishRequest.newBuilder().build(), observer));
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldPersistMessages() {
    Instant occurredAt = Instant.now().minus(20, ChronoUnit.HOURS).truncatedTo(ChronoUnit.MILLIS);
    Context.current().withValue(DelegateAuthServerInterceptor.ACCOUNT_ID_CTX_KEY, TEST_ACC_ID).run(() -> {
      @SuppressWarnings("unchecked") // Casting as we can't use List<PublishedMessage> as the class type.
      ArgumentCaptor<List<io.harness.ccm.commons.entities.events.PublishedMessage>> captor =
          ArgumentCaptor.forClass((Class) List.class);
      PublishRequest publishRequest =
          PublishRequest.newBuilder()
              .addAllMessages(streamWithIndex(testMessages().stream())
                                  .map(pair
                                      -> PublishMessage.newBuilder()
                                             .setMessageId("id-" + pair.getLeft())
                                             .putAttributes("key1", "val1")
                                             .putAttributes("key2", pair.getRight().toString())
                                             .setCategory("")
                                             .setPayload(Any.pack(pair.getRight()))
                                             .setOccurredAt(HTimestamps.fromInstant(occurredAt))
                                             .build())
                                  .collect(toList()))
              .build();
      publisherServer.publish(publishRequest, observer);
      verify(hPersistence).saveIgnoringDuplicateKeys(captor.capture());
      List<io.harness.ccm.commons.entities.events.PublishedMessage> captured = captor.getValue();
      assertThat(captured).containsExactlyElementsOf(
          streamWithIndex(testMessages().stream())
              .map(pair
                  -> io.harness.ccm.commons.entities.events.PublishedMessage.builder()
                         .uuid("id-" + pair.getLeft())
                         .type(Lifecycle.class.getName())
                         .data(Any.pack(pair.getRight()).toByteArray())
                         .accountId(TEST_ACC_ID)
                         .attributes(ImmutableMap.of("key1", "val1", "key2", pair.getRight().toString()))
                         .category("")
                         .occurredAt(occurredAt.toEpochMilli())
                         .build())
              .collect(toList()));
    });
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldRespondErrorWhenPersistFail() {
    RuntimeException exception = new RuntimeException("Persistence error");
    doThrow(exception).when(hPersistence).saveIgnoringDuplicateKeys(anyListOf(PublishedMessage.class));
    ArgumentCaptor<StatusException> captor = ArgumentCaptor.forClass(StatusException.class);
    Context.current().withValue(DelegateAuthServerInterceptor.ACCOUNT_ID_CTX_KEY, TEST_ACC_ID).run(() -> {
      publisherServer.publish(
          PublishRequest.newBuilder()
              .addAllMessages(testMessages()
                                  .stream()
                                  .map(x -> PublishMessage.newBuilder().setPayload(Any.pack(x)).build())
                                  .collect(toList()))
              .build(),
          observer);
      verify(observer).onError(captor.capture());
      assertThat(captor.getValue().getStatus().getCode()).isEqualTo(Code.INTERNAL);
      assertThat(captor.getValue().getStatus().getCause()).isSameAs(exception);
      verify(observer, never()).onNext(any());
      verify(observer, never()).onCompleted();
    });
  }

  private List<Message> testMessages() {
    return Arrays.asList(Lifecycle.newBuilder().setType(EVENT_TYPE_START).setInstanceId("instance-1").build(),
        Lifecycle.newBuilder().setType(EVENT_TYPE_START).setInstanceId("instance-2").build(),
        Lifecycle.newBuilder().setType(EVENT_TYPE_STOP).setInstanceId("instance-2").build(),
        Lifecycle.newBuilder().setType(EVENT_TYPE_STOP).setInstanceId("instance-1").build());
  }

  public <T> Stream<ImmutablePair<Integer, T>> streamWithIndex(Stream<T> stream) {
    return Streams.zip(IntStream.iterate(0, i -> i + 1).boxed(), stream, (i, item) -> ImmutablePair.of(i, item));
  }
}
