package software.wings.waitnotify;

import static com.google.common.collect.ImmutableMap.of;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.persistence.HQuery.excludeAuthority;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static software.wings.waitnotify.NotifyEvent.Builder.aNotifyEvent;
import static software.wings.waitnotify.StringNotifyResponseData.Builder.aStringNotifyResponseData;

import com.google.inject.Inject;

import io.harness.delegate.task.protocol.ResponseData;
import io.harness.maintenance.MaintenanceController;
import io.harness.queue.Queue;
import io.harness.queue.Queue.Filter;
import io.harness.threading.Concurrent;
import org.junit.Before;
import org.junit.Test;
import software.wings.WingsBaseTest;
import software.wings.dl.WingsPersistence;
import software.wings.rules.Listeners;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The Class WaitNotifyEngineTest.
 */
@Listeners(NotifyEventListener.class)
public class WaitNotifyEngineTest extends WingsBaseTest {
  private static AtomicInteger callCount;
  private static Map<String, ResponseData> responseMap;

  @Inject private WaitNotifyEngine waitNotifyEngine;

  @Inject private WingsPersistence wingsPersistence;

  @Inject private Queue<NotifyEvent> notifyEventQueue;

  @Inject private Notifier notifier;
  @Inject private NotifyEventListener notifyEventListener;

  /**
   * Setup response map.
   */
  @Before
  public void setupResponseMap() {
    callCount = new AtomicInteger(0);
    responseMap = new HashMap<>();
  }

  /**
   * Should wait for correlation id.
   */
  @Test
  public void shouldWaitForCorrelationId() {
    String waitInstanceId = waitNotifyEngine.waitForAll(new TestNotifyCallback(), "123");

    assertThat(wingsPersistence.get(WaitInstance.class, waitInstanceId)).isNotNull();

    assertThat(wingsPersistence.createQuery(WaitQueue.class, excludeAuthority).asList())
        .hasSize(1)
        .extracting(WaitQueue::getWaitInstanceId, WaitQueue::getCorrelationId)
        .containsExactly(tuple(waitInstanceId, "123"));

    ResponseData data = aStringNotifyResponseData().withData("response-123").build();
    String id = waitNotifyEngine.notify("123", data);

    assertThat(wingsPersistence.get(NotifyResponse.class, id))
        .isNotNull()
        .extracting(NotifyResponse::getResponse)
        .containsExactly(data);

    while (notifyEventQueue.count(Filter.ALL) != 0) {
      Thread.yield();
    }

    assertThat(responseMap).hasSize(1).isEqualTo(of("123", data));
    assertThat(callCount.get()).isEqualTo(1);
  } // realmongo

  @Test
  public void stressWaitForCorrelationId() {
    MaintenanceController.forceMaintenance(true);

    try {
      String waitInstanceId = waitNotifyEngine.waitForAll(new TestNotifyCallback(), "123");

      assertThat(wingsPersistence.get(WaitInstance.class, waitInstanceId)).isNotNull();

      assertThat(wingsPersistence.createQuery(WaitQueue.class, excludeAuthority).asList())
          .hasSize(1)
          .extracting(WaitQueue::getWaitInstanceId, WaitQueue::getCorrelationId)
          .containsExactly(tuple(waitInstanceId, "123"));

      ResponseData data = aStringNotifyResponseData().withData("response-123").build();
      String id = waitNotifyEngine.notify("123", data);

      Concurrent.test(10, i -> { notifier.execute(); });

      assertThat(wingsPersistence.get(NotifyResponse.class, id))
          .isNotNull()
          .extracting(NotifyResponse::getResponse)
          .containsExactly(data);

      Concurrent.test(10, i -> { notifyEventListener.execute(); });

      assertThat(notifyEventQueue.count(Filter.ALL)).isEqualTo(0);

      assertThat(responseMap).hasSize(1).isEqualTo(of("123", data));
      assertThat(callCount.get()).isEqualTo(1);
    } finally {
      MaintenanceController.forceMaintenance(false);
    }
  }

  /**
   * Should wait for correlation ids.
   */
  @Test
  public void shouldWaitForCorrelationIds() {
    String waitInstanceId = waitNotifyEngine.waitForAll(new TestNotifyCallback(), "123", "456", "789");

    assertThat(wingsPersistence.get(WaitInstance.class, waitInstanceId)).isNotNull();

    assertThat(wingsPersistence.createQuery(WaitQueue.class, excludeAuthority).asList())
        .hasSize(3)
        .extracting(WaitQueue::getWaitInstanceId, WaitQueue::getCorrelationId)
        .containsExactly(tuple(waitInstanceId, "123"), tuple(waitInstanceId, "456"), tuple(waitInstanceId, "789"));

    ResponseData data1 = aStringNotifyResponseData().withData("response-123").build();

    String id = waitNotifyEngine.notify("123", data1);

    assertThat(wingsPersistence.get(NotifyResponse.class, id))
        .isNotNull()
        .extracting(NotifyResponse::getResponse)
        .containsExactly(data1);

    while (notifyEventQueue.count(Filter.ALL) != 0) {
      Thread.yield();
    }

    assertThat(responseMap).hasSize(0);
    ResponseData data2 = aStringNotifyResponseData().withData("response-456").build();

    id = waitNotifyEngine.notify("456", data2);

    assertThat(wingsPersistence.get(NotifyResponse.class, id))
        .isNotNull()
        .extracting(NotifyResponse::getResponse)
        .containsExactly(data2);

    while (notifyEventQueue.count(Filter.ALL) != 0) {
      Thread.yield();
    }

    assertThat(responseMap).hasSize(0);
    ResponseData data3 = aStringNotifyResponseData().withData("response-789").build();

    id = waitNotifyEngine.notify("789", data3);

    assertThat(wingsPersistence.get(NotifyResponse.class, id))
        .isNotNull()
        .extracting(NotifyResponse::getResponse)
        .containsExactly(data3);

    while (notifyEventQueue.count(Filter.ALL) != 0) {
      Thread.yield();
    }

    assertThat(responseMap).hasSize(3).containsAllEntriesOf(of("123", data1, "456", data2, "789", data3));
    assertThat(callCount.get()).isEqualTo(1);
  }

  /**
   * Should wait for correlation id for multiple wait instances.
   */
  @Test
  public void shouldWaitForCorrelationIdForMultipleWaitInstances() {
    String waitInstanceId1 = waitNotifyEngine.waitForAll(new TestNotifyCallback(), "123");
    String waitInstanceId2 = waitNotifyEngine.waitForAll(new TestNotifyCallback(), "123");
    String waitInstanceId3 = waitNotifyEngine.waitForAll(new TestNotifyCallback(), "123");

    assertThat(wingsPersistence.createQuery(WaitInstance.class, excludeAuthority).asList())
        .hasSize(3)
        .extracting(WaitInstance::getUuid)
        .containsExactly(waitInstanceId1, waitInstanceId2, waitInstanceId3);

    assertThat(wingsPersistence.createQuery(WaitQueue.class, excludeAuthority).asList())
        .hasSize(3)
        .extracting(WaitQueue::getWaitInstanceId, WaitQueue::getCorrelationId)
        .containsExactly(tuple(waitInstanceId1, "123"), tuple(waitInstanceId2, "123"), tuple(waitInstanceId3, "123"));

    ResponseData data = aStringNotifyResponseData().withData("response-123").build();
    String id = waitNotifyEngine.notify("123", data);

    assertThat(wingsPersistence.get(NotifyResponse.class, id))
        .isNotNull()
        .extracting(NotifyResponse::getResponse)
        .containsExactly(data);

    while (notifyEventQueue.count(Filter.ALL) != 0) {
      Thread.yield();
    }

    assertThat(responseMap).hasSize(1).containsAllEntriesOf(of("123", data));
    assertThat(callCount.get()).isEqualTo(3);
  }

  @Test
  public void shouldCleanZombieNotifyResponse() {
    final NotifyResponse notifyResponse = new NotifyResponse(generateUuid(), null, false);
    notifyResponse.setCreatedAt(System.currentTimeMillis() - Duration.ofMinutes(6).toMillis());
    String notificationId = wingsPersistence.save(notifyResponse);

    notifier.executeUnderLock();

    assertThat(wingsPersistence.get(NotifyResponse.class, notificationId)).isNull();
  }

  @Test
  public void shouldCleanZombieWaitQueue() {
    final WaitQueue waitQueue = new WaitQueue(generateUuid(), generateUuid());
    waitQueue.setCreatedAt(System.currentTimeMillis() - Duration.ofMinutes(1).toMillis());
    String waitQueueId = wingsPersistence.save(waitQueue);

    notifyEventListener.onMessage(aNotifyEvent()
                                      .withWaitInstanceId(waitQueue.getWaitInstanceId())
                                      .withCorrelationIds(asList(waitQueue.getCorrelationId()))
                                      .build());

    assertThat(wingsPersistence.get(WaitQueue.class, waitQueueId)).isNull();
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
