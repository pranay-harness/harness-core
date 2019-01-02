package io.harness.waiter;

import static com.google.common.collect.ImmutableMap.of;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.persistence.HQuery.excludeAuthority;
import static io.harness.waiter.NotifyEvent.Builder.aNotifyEvent;
import static java.time.Duration.ofMinutes;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.google.inject.Inject;

import io.harness.OrchestrationTest;
import io.harness.delegate.task.protocol.ResponseData;
import io.harness.maintenance.MaintenanceGuard;
import io.harness.persistence.HPersistence;
import io.harness.queue.Queue;
import io.harness.queue.Queue.Filter;
import io.harness.queue.QueueListenerController;
import io.harness.serializer.KryoUtils;
import io.harness.threading.Concurrent;
import io.harness.threading.Puller;
import org.junit.Before;
import org.junit.Test;

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

  @Inject private Queue<NotifyEvent> notifyEventQueue;

  @Inject private Notifier notifier;
  @Inject private NotifyEventListener notifyEventListener;

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

  @Test
  public void shouldClone() {
    KryoUtils.clone(new NotifyEvent());
  }

  /**
   * Should wait for correlation id.
   */
  @Test
  public void shouldWaitForCorrelationId() throws IOException {
    try (MaintenanceGuard guard = new MaintenanceGuard(false)) {
      String waitInstanceId = waitNotifyEngine.waitForAll(new TestNotifyCallback(), "123");

      assertThat(persistence.get(WaitInstance.class, waitInstanceId)).isNotNull();

      assertThat(persistence.createQuery(WaitQueue.class, excludeAuthority).asList())
          .hasSize(1)
          .extracting(WaitQueue::getWaitInstanceId, WaitQueue::getCorrelationId)
          .containsExactly(tuple(waitInstanceId, "123"));

      ResponseData data = StringNotifyResponseData.builder().data("response-123").build();
      String id = waitNotifyEngine.notify("123", data);

      assertThat(persistence.get(NotifyResponse.class, id))
          .isNotNull()
          .extracting(NotifyResponse::getResponse)
          .containsExactly(data);

      Puller.pullFor(Duration.ofSeconds(10), () -> notifyEventQueue.count(Filter.ALL) == 0);

      assertThat(responseMap).hasSize(1).isEqualTo(of("123", data));
      assertThat(callCount.get()).isEqualTo(1);
    }
  }

  @Test
  public void stressWaitForCorrelationId() throws IOException {
    try (MaintenanceGuard guard = new MaintenanceGuard(true)) {
      String waitInstanceId = waitNotifyEngine.waitForAll(new TestNotifyCallback(), "123");

      assertThat(persistence.get(WaitInstance.class, waitInstanceId)).isNotNull();

      assertThat(persistence.createQuery(WaitQueue.class, excludeAuthority).asList())
          .hasSize(1)
          .extracting(WaitQueue::getWaitInstanceId, WaitQueue::getCorrelationId)
          .containsExactly(tuple(waitInstanceId, "123"));

      ResponseData data = StringNotifyResponseData.builder().data("response-123").build();
      String id = waitNotifyEngine.notify("123", data);

      Concurrent.test(10, i -> { notifier.execute(); });

      assertThat(persistence.get(NotifyResponse.class, id))
          .isNotNull()
          .extracting(NotifyResponse::getResponse)
          .containsExactly(data);

      Concurrent.test(10, i -> { notifyEventListener.execute(); });

      assertThat(notifyEventQueue.count(Filter.ALL)).isEqualTo(0);

      assertThat(responseMap).hasSize(1).isEqualTo(of("123", data));
      assertThat(callCount.get()).isEqualTo(1);
    }
  }

  /**
   * Should wait for correlation ids.
   */
  @Test
  public void shouldWaitForCorrelationIds() throws IOException {
    try (MaintenanceGuard guard = new MaintenanceGuard(false)) {
      String waitInstanceId = waitNotifyEngine.waitForAll(new TestNotifyCallback(), "123", "456", "789");

      assertThat(persistence.get(WaitInstance.class, waitInstanceId)).isNotNull();

      assertThat(persistence.createQuery(WaitQueue.class, excludeAuthority).asList())
          .hasSize(3)
          .extracting(WaitQueue::getWaitInstanceId, WaitQueue::getCorrelationId)
          .containsExactly(tuple(waitInstanceId, "123"), tuple(waitInstanceId, "456"), tuple(waitInstanceId, "789"));

      ResponseData data1 = StringNotifyResponseData.builder().data("response-123").build();

      String id = waitNotifyEngine.notify("123", data1);

      assertThat(persistence.get(NotifyResponse.class, id))
          .isNotNull()
          .extracting(NotifyResponse::getResponse)
          .containsExactly(data1);

      Puller.pullFor(Duration.ofSeconds(10), () -> notifyEventQueue.count(Filter.ALL) == 0);

      assertThat(responseMap).hasSize(0);
      ResponseData data2 = StringNotifyResponseData.builder().data("response-456").build();

      id = waitNotifyEngine.notify("456", data2);

      assertThat(persistence.get(NotifyResponse.class, id))
          .isNotNull()
          .extracting(NotifyResponse::getResponse)
          .containsExactly(data2);

      Puller.pullFor(Duration.ofSeconds(10), () -> notifyEventQueue.count(Filter.ALL) == 0);

      assertThat(responseMap).hasSize(0);
      ResponseData data3 = StringNotifyResponseData.builder().data("response-789").build();

      id = waitNotifyEngine.notify("789", data3);

      assertThat(persistence.get(NotifyResponse.class, id))
          .isNotNull()
          .extracting(NotifyResponse::getResponse)
          .containsExactly(data3);

      Puller.pullFor(Duration.ofSeconds(10), () -> notifyEventQueue.count(Filter.ALL) == 0);

      assertThat(responseMap).hasSize(3).containsAllEntriesOf(of("123", data1, "456", data2, "789", data3));
      assertThat(callCount.get()).isEqualTo(1);
    }
  }

  /**
   * Should wait for correlation id for multiple wait instances.
   */
  @Test
  public void shouldWaitForCorrelationIdForMultipleWaitInstances() throws IOException {
    try (MaintenanceGuard guard = new MaintenanceGuard(false)) {
      String waitInstanceId1 = waitNotifyEngine.waitForAll(new TestNotifyCallback(), "123");
      String waitInstanceId2 = waitNotifyEngine.waitForAll(new TestNotifyCallback(), "123");
      String waitInstanceId3 = waitNotifyEngine.waitForAll(new TestNotifyCallback(), "123");

      assertThat(persistence.createQuery(WaitInstance.class, excludeAuthority).asList())
          .hasSize(3)
          .extracting(WaitInstance::getUuid)
          .containsExactly(waitInstanceId1, waitInstanceId2, waitInstanceId3);

      assertThat(persistence.createQuery(WaitQueue.class, excludeAuthority).asList())
          .hasSize(3)
          .extracting(WaitQueue::getWaitInstanceId, WaitQueue::getCorrelationId)
          .containsExactly(tuple(waitInstanceId1, "123"), tuple(waitInstanceId2, "123"), tuple(waitInstanceId3, "123"));

      ResponseData data = StringNotifyResponseData.builder().data("response-123").build();
      String id = waitNotifyEngine.notify("123", data);

      assertThat(persistence.get(NotifyResponse.class, id))
          .isNotNull()
          .extracting(NotifyResponse::getResponse)
          .containsExactly(data);

      while (notifyEventQueue.count(Filter.ALL) != 0) {
        Thread.yield();
      }

      assertThat(responseMap).hasSize(1).containsAllEntriesOf(of("123", data));
      assertThat(callCount.get()).isEqualTo(3);
    }
  }

  @Test
  public void shouldCleanZombieNotifyResponse() {
    final NotifyResponse notifyResponse = NotifyResponse.builder()
                                              .uuid(generateUuid())
                                              .createdAt(System.currentTimeMillis() - ofMinutes(6).toMillis())
                                              .error(false)
                                              .build();
    String notificationId = persistence.save(notifyResponse);

    notifier.executeUnderLock();

    assertThat(persistence.get(NotifyResponse.class, notificationId)).isNull();
  }

  @Test
  public void shouldCleanZombieWaitQueue() {
    final WaitQueue waitQueue = WaitQueue.builder()
                                    .uuid(generateUuid())
                                    .createdAt(System.currentTimeMillis() - ofMinutes(1).toMillis())
                                    .waitInstanceId(generateUuid())
                                    .correlationId(generateUuid())
                                    .build();
    String waitQueueId = persistence.save(waitQueue);
    assertThat(persistence.get(WaitQueue.class, waitQueueId)).isNotNull();

    notifyEventListener.onMessage(aNotifyEvent()
                                      .withWaitInstanceId(waitQueue.getWaitInstanceId())
                                      .withCorrelationIds(singletonList(waitQueue.getCorrelationId()))
                                      .build());

    assertThat(persistence.get(WaitQueue.class, waitQueueId)).isNull();
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
