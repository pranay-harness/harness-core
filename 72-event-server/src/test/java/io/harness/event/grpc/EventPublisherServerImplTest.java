package io.harness.event.grpc;

import static io.harness.event.payloads.Lifecycle.EventType.EVENT_TYPE_START;
import static io.harness.event.payloads.Lifecycle.EventType.EVENT_TYPE_STOP;
import static io.harness.rule.OwnerRule.AVMOHAN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableMap;
import com.google.protobuf.Any;
import com.google.protobuf.Message;

import io.grpc.Context;
import io.grpc.Status.Code;
import io.grpc.StatusException;
import io.grpc.stub.StreamObserver;
import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.event.PublishMessage;
import io.harness.event.PublishRequest;
import io.harness.event.PublishResponse;
import io.harness.event.payloads.Lifecycle;
import io.harness.grpc.auth.DelegateAuthServerInterceptor;
import io.harness.grpc.utils.HTimestamps;
import io.harness.persistence.HPersistence;
import io.harness.rule.OwnerRule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RunWith(MockitoJUnitRunner.class)
public class EventPublisherServerImplTest extends CategoryTest {
  private static final String TEST_ACC_ID = UUID.randomUUID().toString();

  @Mock private HPersistence hPersistence;
  @Mock private StreamObserver<PublishResponse> observer;

  @InjectMocks private EventPublisherServerImpl publisherServer;

  @Test
  @Owner(emails = AVMOHAN, resent = false)
  @Category(UnitTests.class)
  public void shouldFailIfAccountIdIsNotSet() {
    assertThatExceptionOfType(NullPointerException.class)
        .isThrownBy(() -> publisherServer.publish(PublishRequest.newBuilder().build(), observer));
  }

  @Test
  @Owner(emails = AVMOHAN, resent = false)
  @Category(UnitTests.class)
  public void shouldPersistMessages() {
    Instant occurredAt = Instant.now().minus(20, ChronoUnit.HOURS).truncatedTo(ChronoUnit.MILLIS);
    Context.current().withValue(DelegateAuthServerInterceptor.ACCOUNT_ID_CTX_KEY, TEST_ACC_ID).run(() -> {
      @SuppressWarnings("unchecked") // Casting as we can't use List<PublishedMessage> as the class type.
      ArgumentCaptor<List<PublishedMessage>> captor = ArgumentCaptor.forClass((Class) List.class);
      PublishRequest publishRequest =
          PublishRequest.newBuilder()
              .addAllMessages(testMessages()
                                  .stream()
                                  .map(message
                                      -> PublishMessage.newBuilder()
                                             .putAttributes("key1", "val1")
                                             .putAttributes("key2", message.toString())
                                             .setPayload(Any.pack(message))
                                             .setOccurredAt(HTimestamps.fromInstant(occurredAt))
                                             .build())
                                  .collect(Collectors.toList()))
              .build();
      publisherServer.publish(publishRequest, observer);
      verify(hPersistence).save(captor.capture());
      List<PublishedMessage> captured = captor.getValue();
      assertThat(captured).containsExactlyElementsOf(
          testMessages()
              .stream()
              .map(message
                  -> PublishedMessage.builder()
                         .type(Lifecycle.class.getName())
                         .data(Any.pack(message).toByteArray())
                         .accountId(TEST_ACC_ID)
                         .attributes(ImmutableMap.of("key1", "val1", "key2", message.toString()))
                         .occurredAt(occurredAt.toEpochMilli())
                         .build())
              .collect(Collectors.toList()));
    });
  }

  @Test
  @Owner(emails = AVMOHAN, resent = false)
  @Category(UnitTests.class)
  public void shouldRespondErrorWhenPersistFail() {
    RuntimeException exception = new RuntimeException("Persistence error");
    when(hPersistence.save(anyListOf(PublishedMessage.class))).thenThrow(exception);
    ArgumentCaptor<StatusException> captor = ArgumentCaptor.forClass(StatusException.class);
    Context.current().withValue(DelegateAuthServerInterceptor.ACCOUNT_ID_CTX_KEY, TEST_ACC_ID).run(() -> {
      publisherServer.publish(PublishRequest.newBuilder().build(), observer);
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
}