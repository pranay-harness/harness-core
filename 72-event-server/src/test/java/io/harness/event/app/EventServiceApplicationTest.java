package io.harness.event.app;

import static io.harness.event.app.EventServiceTestRule.DEFAULT_ACCOUNT_ID;
import static io.harness.event.app.EventServiceTestRule.DEFAULT_ACCOUNT_SECRET;
import static io.harness.event.payloads.Lifecycle.EventType.EVENT_TYPE_START;
import static io.harness.rule.OwnerRule.AVMOHAN;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.event.client.EventPublisher;
import io.harness.event.grpc.PublishedMessage;
import io.harness.event.payloads.Lifecycle;
import io.harness.grpc.utils.HTimestamps;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;
import io.harness.rule.RealMongo;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.Account;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
public class EventServiceApplicationTest extends CategoryTest {
  @Rule public final EventServiceTestRule eventServiceTestRule = new EventServiceTestRule();

  @Inject private HPersistence hPersistence;

  @Inject private EventPublisher eventPublisher;

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  @RealMongo
  public void shouldEventuallyPersistPublishedEvent() throws Exception {
    hPersistence.save(
        Account.Builder.anAccount().withUuid(DEFAULT_ACCOUNT_ID).withAccountKey(DEFAULT_ACCOUNT_SECRET).build());
    Lifecycle message = Lifecycle.newBuilder()
                            .setInstanceId("instanceId-123")
                            .setType(EVENT_TYPE_START)
                            .setTimestamp(HTimestamps.fromInstant(Instant.now()))
                            .setCreatedTimestamp(HTimestamps.fromInstant(Instant.now().minus(10, ChronoUnit.HOURS)))
                            .build();
    Map<String, String> attributes = ImmutableMap.of("k1", "v1", "k2", "v2");
    eventPublisher.publishMessage(message, message.getTimestamp(), attributes);
    Awaitility.await().atMost(10, TimeUnit.SECONDS).pollInterval(100, TimeUnit.MILLISECONDS).until(() -> {
      PublishedMessage publishedMessage = hPersistence.createQuery(PublishedMessage.class).get();
      assertThat(publishedMessage).isNotNull();
      assertThat(publishedMessage.getAccountId()).isEqualTo(DEFAULT_ACCOUNT_ID);
      assertThat(publishedMessage.getAttributes()).isEqualTo(attributes);
      assertThat(publishedMessage.getMessage()).isEqualTo(message);
    });
  }
}