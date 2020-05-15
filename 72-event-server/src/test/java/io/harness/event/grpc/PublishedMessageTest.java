package io.harness.event.grpc;

import static io.harness.event.payloads.Lifecycle.EventType.EVENT_TYPE_START;
import static io.harness.rule.OwnerRule.AVMOHAN;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.protobuf.Any;
import com.google.protobuf.util.Timestamps;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.event.payloads.EcsTaskLifecycle;
import io.harness.event.payloads.Lifecycle;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.time.OffsetDateTime;
import java.util.Date;

public class PublishedMessageTest extends CategoryTest {
  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void testPostLoad() {
    EcsTaskLifecycle ecsTaskLifecycle =
        EcsTaskLifecycle.newBuilder()
            .setLifecycle(Lifecycle.newBuilder()
                              .setInstanceId("instanceId-123")
                              .setType(EVENT_TYPE_START)
                              .setTimestamp(Timestamps.fromMillis(System.currentTimeMillis())))
            .build();
    Any payload = Any.pack(ecsTaskLifecycle);
    String accountId = "accountId-123";
    PublishedMessage publishedMessage = PublishedMessage.builder()
                                            .accountId(accountId)
                                            .data(payload.toByteArray())
                                            .type(ecsTaskLifecycle.getClass().getName())
                                            .build();
    assertThat(publishedMessage.getMessage()).isEqualTo(ecsTaskLifecycle);
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldSetValidUntil() throws Exception {
    Date expected = Date.from(OffsetDateTime.now().plusDays(14).toInstant());
    PublishedMessage message = PublishedMessage.builder().build();
    assertThat(message.getValidUntil()).isNotNull().isAfter(expected);
  }
}
