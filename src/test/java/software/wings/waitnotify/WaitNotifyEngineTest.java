package software.wings.waitnotify;

import static com.google.common.collect.ImmutableMap.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import org.junit.Before;
import org.junit.Test;
import software.wings.WingsBaseTest;
import software.wings.core.queue.Queue;
import software.wings.dl.WingsPersistence;
import software.wings.rules.Listeners;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import javax.inject.Inject;

// TODO: Auto-generated Javadoc

/**
 * The Class WaitNotifyEngineTest.
 */
@Listeners(NotifyEventListener.class)
public class WaitNotifyEngineTest extends WingsBaseTest {
  private static AtomicInteger callCount;
  private static Map<String, Serializable> responseMap;

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

    String id = waitNotifyEngine.notify("123", "response-123");

    assertThat(wingsPersistence.get(NotifyResponse.class, id))
        .isNotNull()
        .extracting(NotifyResponse::getResponse)
        .containsExactly("response-123");

    while (notifyEventQueue.count() != 0) {
      Thread.yield();
    }

    assertThat(responseMap).hasSize(1).isEqualTo(of("123", "response-123"));
    assertThat(callCount.get()).isEqualTo(1);
  }

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

    String id = waitNotifyEngine.notify("123", "response-123");

    assertThat(wingsPersistence.get(NotifyResponse.class, id))
        .isNotNull()
        .extracting(NotifyResponse::getResponse)
        .containsExactly("response-123");

    while (notifyEventQueue.count() != 0) {
      Thread.yield();
    }

    assertThat(responseMap).hasSize(0);

    id = waitNotifyEngine.notify("456", "response-456");

    assertThat(wingsPersistence.get(NotifyResponse.class, id))
        .isNotNull()
        .extracting(NotifyResponse::getResponse)
        .containsExactly("response-456");

    while (notifyEventQueue.count() != 0) {
      Thread.yield();
    }

    assertThat(responseMap).hasSize(0);

    id = waitNotifyEngine.notify("789", "response-789");

    assertThat(wingsPersistence.get(NotifyResponse.class, id))
        .isNotNull()
        .extracting(NotifyResponse::getResponse)
        .containsExactly("response-789");

    while (notifyEventQueue.count() != 0) {
      Thread.yield();
    }

    assertThat(responseMap)
        .hasSize(3)
        .containsAllEntriesOf(of("123", "response-123", "456", "response-456", "789", "response-789"));
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

    String id = waitNotifyEngine.notify("123", "response-123");

    assertThat(wingsPersistence.get(NotifyResponse.class, id))
        .isNotNull()
        .extracting(NotifyResponse::getResponse)
        .containsExactly("response-123");

    while (notifyEventQueue.count() != 0) {
      Thread.yield();
    }

    assertThat(responseMap).hasSize(1).containsAllEntriesOf(of("123", "response-123"));
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
    public void notify(Map<String, ? extends Serializable> response) {
      callCount.incrementAndGet();
      responseMap.putAll(response);
    }
  }
}
