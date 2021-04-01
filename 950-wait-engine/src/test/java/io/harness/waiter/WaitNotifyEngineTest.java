package io.harness.waiter;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.persistence.HQuery.excludeAuthority;
import static io.harness.rule.OwnerRule.GEORGE;
import static io.harness.rule.OwnerRule.SANJA;
import static io.harness.waiter.TestNotifyEventListener.TEST_PUBLISHER;

import static com.google.common.collect.ImmutableMap.of;
import static java.time.Duration.ofMillis;
import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.WaitEngineTestBase;
import io.harness.category.element.UnitTests;
import io.harness.maintenance.MaintenanceGuard;
import io.harness.persistence.HPersistence;
import io.harness.queue.QueueConsumer;
import io.harness.queue.QueueConsumer.Filter;
import io.harness.queue.QueueListenerController;
import io.harness.rule.Owner;
import io.harness.serializer.KryoSerializer;
import io.harness.tasks.ProgressData;
import io.harness.tasks.ResponseData;
import io.harness.threading.Concurrent;
import io.harness.threading.Poller;
import io.harness.waiter.ProgressUpdate.ProgressUpdateKeys;

import com.google.inject.Inject;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mongodb.morphia.query.Sort;

@Slf4j
public class WaitNotifyEngineTest extends WaitEngineTestBase {
  private static AtomicInteger callCount;
  private static AtomicInteger progressCallCount;
  private static Map<String, ResponseData> responseMap;
  private static List<ProgressData> progressDataList;

  @Inject private WaitNotifyEngine waitNotifyEngine;
  @Inject private HPersistence persistence;
  @Inject private QueueConsumer<NotifyEvent> notifyConsumer;
  @Inject private NotifyResponseCleaner notifyResponseCleaner;
  @Inject private TestNotifyEventListener notifyEventListener;
  @Inject private QueueListenerController queueListenerController;
  @Inject private KryoSerializer kryoSerializer;

  /**
   * Setup response map.
   */
  @Before
  public void setupResponseMap() {
    callCount = new AtomicInteger(0);
    responseMap = new HashMap<>();
    progressCallCount = new AtomicInteger(0);
    progressDataList = new ArrayList<>();
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
      NotifyResponse notifyResponse = persistence.get(NotifyResponse.class, id);
      assertThat(notifyResponse).isNotNull();
      ResponseData responseDataResult =
          (ResponseData) kryoSerializer.asInflatedObject(notifyResponse.getResponseData());
      assertThat(responseDataResult).isEqualTo(data);

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

      NotifyResponse notifyResponse = persistence.get(NotifyResponse.class, id);
      assertThat(notifyResponse).isNotNull();
      ResponseData responseDataResult =
          (ResponseData) kryoSerializer.asInflatedObject(notifyResponse.getResponseData());
      assertThat(responseDataResult).isEqualTo(data);

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

      NotifyResponse notifyResponse = persistence.get(NotifyResponse.class, id);
      assertThat(notifyResponse).isNotNull();
      ResponseData responseDataResult =
          (ResponseData) kryoSerializer.asInflatedObject(notifyResponse.getResponseData());
      assertThat(responseDataResult).isEqualTo(data);

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

      NotifyResponse notifyResponse1 = persistence.get(NotifyResponse.class, id);
      assertThat(notifyResponse1).isNotNull();
      ResponseData responseDataResult1 =
          (ResponseData) kryoSerializer.asInflatedObject(notifyResponse1.getResponseData());
      assertThat(responseDataResult1).isEqualTo(data1);

      Poller.pollFor(Duration.ofSeconds(10), ofMillis(100), () -> notifyConsumer.count(Filter.ALL) == 0);

      assertThat(responseMap).hasSize(0);
      ResponseData data2 = StringNotifyResponseData.builder().data("response-" + uuid2).build();

      id = waitNotifyEngine.doneWith(uuid2, data2);

      NotifyResponse notifyResponse2 = persistence.get(NotifyResponse.class, id);
      assertThat(notifyResponse2).isNotNull();
      ResponseData responseDataResult2 =
          (ResponseData) kryoSerializer.asInflatedObject(notifyResponse2.getResponseData());
      assertThat(responseDataResult2).isEqualTo(data2);

      Poller.pollFor(Duration.ofSeconds(10), ofMillis(100), () -> notifyConsumer.count(Filter.ALL) == 0);

      assertThat(responseMap).hasSize(0);
      ResponseData data3 = StringNotifyResponseData.builder().data("response-" + uuid3).build();

      id = waitNotifyEngine.doneWith(uuid3, data3);

      NotifyResponse notifyResponse3 = persistence.get(NotifyResponse.class, id);
      assertThat(notifyResponse3).isNotNull();
      ResponseData responseDataResult =
          (ResponseData) kryoSerializer.asInflatedObject(notifyResponse3.getResponseData());
      assertThat(responseDataResult).isEqualTo(data3);

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

      NotifyResponse notifyResponse = persistence.get(NotifyResponse.class, id);
      assertThat(notifyResponse).isNotNull();
      ResponseData responseDataResult =
          (ResponseData) kryoSerializer.asInflatedObject(notifyResponse.getResponseData());
      assertThat(responseDataResult).isEqualTo(data);

      while (notifyConsumer.count(Filter.ALL) != 0) {
        Thread.yield();
      }

      assertThat(responseMap).hasSize(1).containsAllEntriesOf(of(uuid, data));
      assertThat(callCount.get()).isEqualTo(3);
    }
  }

  /**
   * Should wait for progress on correlation id.
   */
  @Test
  @Owner(developers = SANJA)
  @Category(UnitTests.class)
  public void shouldWaitForProgressOnCorrelationId() throws InterruptedException {
    String uuid = generateUuid();
    try (MaintenanceGuard guard = new MaintenanceGuard(false)) {
      String waitInstanceId =
          waitNotifyEngine.waitForAllOn(TEST_PUBLISHER, new TestNotifyCallback(), new TestProgressCallback(), uuid);

      assertThat(persistence.get(WaitInstance.class, waitInstanceId)).isNotNull();
      StringNotifyProgressData data1 = StringNotifyProgressData.builder().data("progress1-" + uuid).build();
      waitNotifyEngine.progressOn(uuid, data1);

      ProgressUpdate progressUpdate = persistence.createQuery(ProgressUpdate.class, excludeAuthority)
                                          .filter(ProgressUpdateKeys.correlationId, uuid)
                                          .get();
      assertThat(progressUpdate).isNotNull();
      ProgressData progressDataResult =
          (ProgressData) kryoSerializer.asInflatedObject(progressUpdate.getProgressData());
      assertThat(progressDataResult).isEqualTo(data1);

      StringNotifyProgressData data2 = StringNotifyProgressData.builder().data("progress2-" + uuid).build();
      waitNotifyEngine.progressOn(uuid, data2);

      List<ProgressUpdate> progressUpdate2 = persistence.createQuery(ProgressUpdate.class, excludeAuthority)
                                                 .filter(ProgressUpdateKeys.correlationId, uuid)
                                                 .order(Sort.ascending(ProgressUpdateKeys.createdAt))
                                                 .asList();
      assertThat(progressUpdate2).hasSize(2);
      ProgressData progressDataResult2 =
          (ProgressData) kryoSerializer.asInflatedObject(progressUpdate2.get(1).getProgressData());
      assertThat(progressDataResult2).isEqualTo(data2);

      while (progressCallCount.get() < 2) {
        Thread.yield();
      }

      StringNotifyProgressData result1 = (StringNotifyProgressData) progressDataList.get(0);
      StringNotifyProgressData result2 = (StringNotifyProgressData) progressDataList.get(1);

      assertThat(progressDataList).hasSize(2);
      assertThat(Arrays.asList(result1.getData(), result2.getData()))
          .containsExactlyInAnyOrder(data1.getData(), data2.getData());
      assertThat(progressCallCount.get()).isEqualTo(2);
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

  public static class TestNotifyCallback implements OldNotifyCallback {
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

  public static class TestProgressCallback implements ProgressCallback {
    @Override
    public void notify(String correlationId, ProgressData progressData) {
      progressCallCount.incrementAndGet();
      progressDataList.add(progressData);
    }
  }
}
