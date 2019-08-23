package io.harness.event.grpc;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.protobuf.Any;
import com.google.protobuf.util.Timestamps;

import io.harness.category.element.UnitTests;
import io.harness.event.payloads.EcsTaskLifecycle;
import io.harness.event.payloads.Lifecycle;
import io.harness.event.payloads.Lifecycle.EventType;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class PublishedMessageTest {
  @Test
  @Category(UnitTests.class)
  public void testPostLoad() {
    EcsTaskLifecycle ecsTaskLifecycle =
        EcsTaskLifecycle.newBuilder()
            .setLifecycle(Lifecycle.newBuilder()
                              .setInstanceId("instanceId-123")
                              .setType(EventType.START)
                              .setTimestamp(Timestamps.fromMillis(System.currentTimeMillis())))
            .build();
    Any payload = Any.pack(ecsTaskLifecycle);
    String accountId = "accountId-123";
    PublishedMessage publishedMessage = PublishedMessage.builder()
                                            .accountId(accountId)
                                            .data(payload.toByteArray())
                                            .type(ecsTaskLifecycle.getClass().getName())
                                            .build();
    publishedMessage.postLoad();
    assertThat(publishedMessage.getMessage()).isEqualTo(ecsTaskLifecycle);
  }
}