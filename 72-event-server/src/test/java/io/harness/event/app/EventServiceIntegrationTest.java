package io.harness.event.app;

import static io.harness.event.payloads.Lifecycle.EventType.EVENT_TYPE_START;
import static io.harness.rule.OwnerRule.AVMOHAN;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import io.harness.category.element.IntegrationTests;
import io.harness.event.client.EventPublisher;
import io.harness.event.grpc.PublishedMessage;
import io.harness.event.payloads.Lifecycle;
import io.harness.grpc.utils.HTimestamps;
import io.harness.persistence.HPersistence;
import io.harness.rule.OwnerRule.Owner;
import io.harness.rule.RealMongo;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
public class EventServiceIntegrationTest {
  @Rule public final EventServiceTestRule eventServiceTestRule = new EventServiceTestRule();

  @Inject HPersistence hPersistence;

  @Inject private EventPublisher eventPublisher;

  @Test
  @Owner(emails = AVMOHAN)
  @Category(IntegrationTests.class)
  @RealMongo
  public void shouldEventuallyPersistPublishedEvent() throws Exception {
    Lifecycle message = Lifecycle.newBuilder()
                            .setInstanceId("instanceId-123")
                            .setType(EVENT_TYPE_START)
                            .setTimestamp(HTimestamps.fromInstant(Instant.now()))
                            .setCreatedTimestamp(HTimestamps.fromInstant(Instant.now().minus(10, ChronoUnit.HOURS)))
                            .build();
    Map<String, String> attributes = ImmutableMap.of("k1", "v1", "k2", "v2");
    eventPublisher.publishMessageWithAttributes(message, attributes);
    Awaitility.await().atMost(10, TimeUnit.SECONDS).pollInterval(100, TimeUnit.MILLISECONDS).until(() -> {
      PublishedMessage publishedMessage = hPersistence.createQuery(PublishedMessage.class).get();
      assertThat(publishedMessage).isNotNull();
      assertThat(publishedMessage.getAccountId()).isEqualTo(EventServiceTestRule.DEFAULT_ACCOUNT_ID);
      assertThat(publishedMessage.getAttributes()).isEqualTo(attributes);
      assertThat(publishedMessage.getMessage()).isEqualTo(message);
    });
  }
}