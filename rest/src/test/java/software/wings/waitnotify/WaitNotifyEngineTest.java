package software.wings.waitnotify;

import static com.google.common.collect.ImmutableMap.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static software.wings.waitnotify.StringNotifyResponseData.Builder.aStringNotifyResponseData;

import com.google.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import software.wings.WingsBaseTest;
import software.wings.core.queue.Queue;
import software.wings.dl.WingsPersistence;
import software.wings.rules.Listeners;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The Class WaitNotifyEngineTest.
 */
@Listeners(NotifyEventListener.class)
public class WaitNotifyEngineTest extends WingsBaseTest {
  private static AtomicInteger callCount;
  private static Map<String, NotifyResponseData> responseMap;

  @Inject private WaitNotifyEngine waitNotifyEngine;

  @Inject private WingsPersistence wingsPersistence;

  @Inject private Queue<NotifyEvent> notifyEventQueue;

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

    assertThat(wingsPersistence.list(WaitQueue.class))
        .hasSize(1)
        .extracting(WaitQueue::getWaitInstanceId, WaitQueue::getCorrelationId)
        .containsExactly(tuple(waitInstanceId, "123"));

    NotifyResponseData data = aStringNotifyResponseData().withData("response-123").build();
    String id = waitNotifyEngine.notify("123", data);

    assertThat(wingsPersistence.get(NotifyResponse.class, id))
        .isNotNull()
        .extracting(NotifyResponse::getResponse)
        .containsExactly(data);

    while (notifyEventQueue.count() != 0) {
      Thread.yield();
    }

    assertThat(responseMap).hasSize(1).isEqualTo(of("123", data));
    assertThat(callCount.get()).isEqualTo(1);
  } // realmongo

  /**
   * Should wait for correlation ids.
   */
  @Test
  public void shouldWaitForCorrelationIds() {
    String waitInstanceId = waitNotifyEngine.waitForAll(new TestNotifyCallback(), "123", "456", "789");

    assertThat(wingsPersistence.get(WaitInstance.class, waitInstanceId)).isNotNull();

    assertThat(wingsPersistence.list(WaitQueue.class))
        .hasSize(3)
        .extracting(WaitQueue::getWaitInstanceId, WaitQueue::getCorrelationId)
        .containsExactly(tuple(waitInstanceId, "123"), tuple(waitInstanceId, "456"), tuple(waitInstanceId, "789"));

    NotifyResponseData data1 = aStringNotifyResponseData().withData("response-123").build();

    String id = waitNotifyEngine.notify("123", data1);

    assertThat(wingsPersistence.get(NotifyResponse.class, id))
        .isNotNull()
        .extracting(NotifyResponse::getResponse)
        .containsExactly(data1);

    while (notifyEventQueue.count() != 0) {
      Thread.yield();
    }

    assertThat(responseMap).hasSize(0);
    NotifyResponseData data2 = aStringNotifyResponseData().withData("response-456").build();

    id = waitNotifyEngine.notify("456", data2);

    assertThat(wingsPersistence.get(NotifyResponse.class, id))
        .isNotNull()
        .extracting(NotifyResponse::getResponse)
        .containsExactly(data2);

    while (notifyEventQueue.count() != 0) {
      Thread.yield();
    }

    assertThat(responseMap).hasSize(0);
    NotifyResponseData data3 = aStringNotifyResponseData().withData("response-789").build();

    id = waitNotifyEngine.notify("789", data3);

    assertThat(wingsPersistence.get(NotifyResponse.class, id))
        .isNotNull()
        .extracting(NotifyResponse::getResponse)
        .containsExactly(data3);

    while (notifyEventQueue.count() != 0) {
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

    assertThat(wingsPersistence.list(WaitInstance.class))
        .hasSize(3)
        .extracting(WaitInstance::getUuid)
        .containsExactly(waitInstanceId1, waitInstanceId2, waitInstanceId3);

    assertThat(wingsPersistence.list(WaitQueue.class))
        .hasSize(3)
        .extracting(WaitQueue::getWaitInstanceId, WaitQueue::getCorrelationId)
        .containsExactly(tuple(waitInstanceId1, "123"), tuple(waitInstanceId2, "123"), tuple(waitInstanceId3, "123"));

    NotifyResponseData data = aStringNotifyResponseData().withData("response-123").build();
    String id = waitNotifyEngine.notify("123", data);

    assertThat(wingsPersistence.get(NotifyResponse.class, id))
        .isNotNull()
        .extracting(NotifyResponse::getResponse)
        .containsExactly(data);

    while (notifyEventQueue.count() != 0) {
      Thread.yield();
    }

    assertThat(responseMap).hasSize(1).containsAllEntriesOf(of("123", data));
    assertThat(callCount.get()).isEqualTo(3);
  }

  /**
   * Created by peeyushaggarwal on 4/5/16.
   */
  public static class TestNotifyCallback implements NotifyCallback {
    /* (non-Javadoc)
     * @see software.wings.waitnotify.NotifyCallback#notify(java.util.Map)
     */
    @Override
    public void notify(Map<String, NotifyResponseData> response) {
      callCount.incrementAndGet();
      responseMap.putAll(response);
    }

    @Override
    public void notifyError(Map<String, NotifyResponseData> response) {
      // Do Nothing.
    }
  }
}
