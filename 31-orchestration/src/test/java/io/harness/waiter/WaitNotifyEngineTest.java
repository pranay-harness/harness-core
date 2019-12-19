package io.harness.waiter;

import static com.google.common.collect.ImmutableMap.of;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.persistence.HQuery.excludeAuthority;
import static io.harness.rule.OwnerRule.GEORGE;
import static io.harness.waiter.TestNotifyEventListener.TEST_PUBLISHER;
import static java.time.Duration.ofMillis;
import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.OrchestrationTest;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.ResponseData;
import io.harness.maintenance.MaintenanceGuard;
import io.harness.persistence.HPersistence;
import io.harness.queue.QueueConsumer;
import io.harness.queue.QueueConsumer.Filter;
import io.harness.queue.QueueListenerController;
import io.harness.rule.OwnerRule.Owner;
import io.harness.threading.Concurrent;
import io.harness.threading.Poller;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class WaitNotifyEngineTest extends OrchestrationTest {
  private static AtomicInteger callCount;
  private static Map<String, ResponseData> responseMap;

  @Inject private WaitNotifyEngine waitNotifyEngine;
  @Inject private HPersistence persistence;
  @Inject private QueueConsumer<NotifyEvent> notifyConsumer;
  @Inject private NotifyResponseCleaner notifyResponseCleaner;
  @Inject private TestNotifyEventListener notifyEventListener;
  @Inject private QueueListenerController queueListenerController;

  /**
   * Setup response map.
   */
  @Before
  public void setupResponseMap() {
    callCount = new AtomicInteger(0);
    responseMap = new HashMap<>();
    queueListenerController.register(notifyEventListener, 1);
  }

  /**
   * Should wait for correlation id.
   */
  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void shouldWaitForCorrelationId() throws IOException {
    String uuid = generateUuid();
    try (MaintenanceGuard guard = new MaintenanceGuard(false)) {
      String waitInstanceId = waitNotifyEngine.waitForAllOn(TEST_PUBLISHER, new TestNotifyCallback(), uuid);

      assertThat(persistence.get(WaitInstance.class, waitInstanceId)).isNotNull();

      ResponseData data = StringNotifyResponseData.builder().data("response-" + uuid).build();
      String id = waitNotifyEngine.doneWith(uuid, data);

      assertThat(persistence.get(NotifyResponse.class, id))
          .isNotNull()
          .extracting(NotifyResponse::getResponse)
          .isEqualTo(data);

      Poller.pollFor(Duration.ofSeconds(10), ofMillis(100), () -> notifyConsumer.count(Filter.ALL) == 0);

      assertThat(responseMap).hasSize(1).isEqualTo(of(uuid, data));
      assertThat(callCount.get()).isEqualTo(1);
    }
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void stressWaitForCorrelationId() throws IOException {
    String uuid = generateUuid();
    try (MaintenanceGuard guard = new MaintenanceGuard(true)) {
      String waitInstanceId = waitNotifyEngine.waitForAllOn(TEST_PUBLISHER, new TestNotifyCallback(), uuid);

      assertThat(persistence.get(WaitInstance.class, waitInstanceId)).isNotNull();

      ResponseData data = StringNotifyResponseData.builder().data("response-" + uuid).build();
      String id = waitNotifyEngine.doneWith(uuid, data);

      assertThat(persistence.get(NotifyResponse.class, id))
          .isNotNull()
          .extracting(NotifyResponse::getResponse)
          .isEqualTo(data);

      Concurrent.test(10, i -> { notifyEventListener.execute(); });

      assertThat(notifyConsumer.count(Filter.ALL)).isEqualTo(0);

      assertThat(responseMap).hasSize(1).isEqualTo(of(uuid, data));
      assertThat(callCount.get()).isEqualTo(1);
    }
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testNotifyBeforeWait() throws IOException {
    String uuid = generateUuid();
    try (MaintenanceGuard guard = new MaintenanceGuard(true)) {
      ResponseData data = StringNotifyResponseData.builder().data("response-" + uuid).build();
      String id = waitNotifyEngine.doneWith(uuid, data);

      String waitInstanceId = waitNotifyEngine.waitForAllOn(TEST_PUBLISHER, new TestNotifyCallback(), uuid);

      assertThat(persistence.get(WaitInstance.class, waitInstanceId)).isNotNull();

      assertThat(persistence.get(NotifyResponse.class, id))
          .isNotNull()
          .extracting(NotifyResponse::getResponse)
          .isEqualTo(data);

      notifyEventListener.execute();

      assertThat(notifyConsumer.count(Filter.ALL)).isEqualTo(0);

      assertThat(responseMap).hasSize(1).isEqualTo(of(uuid, data));
      assertThat(callCount.get()).isEqualTo(1);
    }
  }

  /**
   * Should wait for correlation ids.
   */
  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void shouldWaitForCorrelationIds() throws IOException {
    String uuid1 = generateUuid();
    String uuid2 = generateUuid();
    String uuid3 = generateUuid();

    try (MaintenanceGuard guard = new MaintenanceGuard(false)) {
      String waitInstanceId =
          waitNotifyEngine.waitForAllOn(TEST_PUBLISHER, new TestNotifyCallback(), uuid1, uuid2, uuid3);

      assertThat(persistence.get(WaitInstance.class, waitInstanceId)).isNotNull();

      ResponseData data1 = StringNotifyResponseData.builder().data("response-" + uuid1).build();

      String id = waitNotifyEngine.doneWith(uuid1, data1);

      assertThat(persistence.get(NotifyResponse.class, id))
          .isNotNull()
          .extracting(NotifyResponse::getResponse)
          .isEqualTo(data1);

      Poller.pollFor(Duration.ofSeconds(10), ofMillis(100), () -> notifyConsumer.count(Filter.ALL) == 0);

      assertThat(responseMap).hasSize(0);
      ResponseData data2 = StringNotifyResponseData.builder().data("response-" + uuid2).build();

      id = waitNotifyEngine.doneWith(uuid2, data2);

      assertThat(persistence.get(NotifyResponse.class, id))
          .isNotNull()
          .extracting(NotifyResponse::getResponse)
          .isEqualTo(data2);

      Poller.pollFor(Duration.ofSeconds(10), ofMillis(100), () -> notifyConsumer.count(Filter.ALL) == 0);

      assertThat(responseMap).hasSize(0);
      ResponseData data3 = StringNotifyResponseData.builder().data("response-" + uuid3).build();

      id = waitNotifyEngine.doneWith(uuid3, data3);

      assertThat(persistence.get(NotifyResponse.class, id))
          .isNotNull()
          .extracting(NotifyResponse::getResponse)
          .isEqualTo(data3);

      Poller.pollFor(Duration.ofSeconds(10), ofMillis(100), () -> notifyConsumer.count(Filter.ALL) == 0);

      assertThat(responseMap).hasSize(3).containsAllEntriesOf(of(uuid1, data1, uuid2, data2, uuid3, data3));
      assertThat(callCount.get()).isEqualTo(1);
    }
  }

  /**
   * Should wait for correlation id for multiple wait instances.
   */
  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void shouldWaitForCorrelationIdForMultipleWaitInstances() throws IOException {
    String uuid = generateUuid();

    try (MaintenanceGuard guard = new MaintenanceGuard(false)) {
      String waitInstanceId1 = waitNotifyEngine.waitForAllOn(TEST_PUBLISHER, new TestNotifyCallback(), uuid);
      String waitInstanceId2 = waitNotifyEngine.waitForAllOn(TEST_PUBLISHER, new TestNotifyCallback(), uuid);
      String waitInstanceId3 = waitNotifyEngine.waitForAllOn(TEST_PUBLISHER, new TestNotifyCallback(), uuid);

      assertThat(persistence.createQuery(WaitInstance.class, excludeAuthority).asList())
          .hasSize(3)
          .extracting(WaitInstance::getUuid)
          .containsExactly(waitInstanceId1, waitInstanceId2, waitInstanceId3);

      ResponseData data = StringNotifyResponseData.builder().data("response-" + uuid).build();
      String id = waitNotifyEngine.doneWith(uuid, data);

      assertThat(persistence.get(NotifyResponse.class, id))
          .isNotNull()
          .extracting(NotifyResponse::getResponse)
          .isEqualTo(data);

      while (notifyConsumer.count(Filter.ALL) != 0) {
        Thread.yield();
      }

      assertThat(responseMap).hasSize(1).containsAllEntriesOf(of(uuid, data));
      assertThat(callCount.get()).isEqualTo(3);
    }
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void shouldCleanZombieNotifyResponse() {
    final NotifyResponse notifyResponse = NotifyResponse.builder()
                                              .uuid(generateUuid())
                                              .createdAt(System.currentTimeMillis() - ofSeconds(20).toMillis())
                                              .error(false)
                                              .build();
    String notificationId = persistence.save(notifyResponse);

    notifyResponseCleaner.executeInternal();

    assertThat(persistence.get(NotifyResponse.class, notificationId)).isNull();
  }

  public static class TestNotifyCallback implements NotifyCallback {
    @Override
    public void notify(Map<String, ResponseData> response) {
      callCount.incrementAndGet();
      responseMap.putAll(response);
    }

    @Override
    public void notifyError(Map<String, ResponseData> response) {
      // Do Nothing.
    }
  }
}
