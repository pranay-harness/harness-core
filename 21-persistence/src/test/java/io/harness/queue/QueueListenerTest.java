package io.harness.queue;

import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;
import static org.joor.Reflect.on;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.inject.Inject;

import io.harness.PersistenceTest;
import io.harness.category.element.UnitTests;
import io.harness.maintenance.MaintenanceGuard;
import io.harness.mongo.MongoQueue;
import io.harness.persistence.HPersistence;
import io.harness.queue.Queue.Filter;
import io.harness.version.VersionInfoManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.internal.stubbing.answers.ThrowsException;
import org.mockito.invocation.InvocationOnMock;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class QueueListenerTest extends PersistenceTest {
  private MongoQueue<TestQueuableObject> queue;
  private TestQueuableObjectListener listener;

  @Inject private HPersistence persistence;
  @Inject private VersionInfoManager versionInfoManager;

  @Inject QueueListenerController queueListenerController;
  @Inject private TimerScheduledExecutorService timer;
  @Inject private QueueController queueController;

  @Before
  public void setup() throws Exception {
    queueListenerController.stop();

    queue = spy(new MongoQueue<>(TestQueuableObject.class));
    on(queue).set("persistence", persistence);
    on(queue).set("versionInfoManager", versionInfoManager);

    listener = new TestQueuableObjectListener();
    listener.setQueue(queue);
    listener.setRunOnce(true);
    on(listener).set("timer", timer);
    on(listener).set("queueController", queueController);
    listener = spy(listener);
  }

  @Test
  @Category(UnitTests.class)
  public void shouldProcessWhenReceivedMessageFromQueue() throws IOException {
    try (MaintenanceGuard guard = new MaintenanceGuard(false)) {
      TestQueuableObject message = new TestQueuableObject(1);
      queue.send(message);
      assertThat(queue.count(Filter.ALL)).isEqualTo(1);
      listener.run();
      assertThat(queue.count(Filter.ALL)).isEqualTo(0);
      verify(listener).onMessage(message);
    }
  }

  @Test(timeout = 1000)
  @Category(UnitTests.class)
  public void shouldStopOnInterruptedException() throws Exception {
    try (MaintenanceGuard guard = new MaintenanceGuard(false)) {
      listener.setRunOnce(false);

      TestQueuableObject message = new TestQueuableObject(1);
      queue.send(message);
      assertThat(queue.count(Filter.ALL)).isEqualTo(1);

      doThrow(new RuntimeException(new InterruptedException())).when(queue).get(any(), any());

      listener.run();

      assertThat(queue.count(Filter.ALL)).isEqualTo(1);
      verify(listener, times(0)).onMessage(any(TestQueuableObject.class));
    }
  }

  @Test(timeout = 5000)
  @Category(UnitTests.class)
  public void shouldExtendResetDuration() throws Exception {
    try (MaintenanceGuard guard = new MaintenanceGuard(false)) {
      TestQueuableObject message = new TestQueuableObject(1);
      queue.send(message);
      assertThat(queue.count(Filter.ALL)).isEqualTo(1);

      listener.setRunOnce(true);
      queue.resetDuration(1);

      doAnswer(invocation -> {
        log().info("In mock executor");
        Thread.sleep(1500);
        log().info("Done with mock");
        return invocation.callRealMethod();
      })
          .when(listener)
          .onMessage(message);

      Thread runThread = new Thread(listener);
      runThread.start();
      runThread.join();

      assertThat(queue.count(Filter.ALL)).isEqualTo(0);
      verify(listener).onMessage(message);
      verify(queue, atLeast(1)).updateResetDuration(any(TestQueuableObject.class));
    }
  }

  @Test(timeout = 5000)
  @Category(UnitTests.class)
  public void shouldContinueProcessingOnAnyOtherException() throws Exception {
    try (MaintenanceGuard guard = new MaintenanceGuard(false)) {
      listener.setRunOnce(false);

      CountDownLatch countDownLatch = new CountDownLatch(2);

      doAnswer(new ThrowsException(new RuntimeException()) {
        @Override
        public Object answer(InvocationOnMock invocation) throws Throwable {
          countDownLatch.countDown();
          return super.answer(invocation);
        }
      })
          .when(queue)
          .get(ofSeconds(3), ofSeconds(1));

      Thread listenerThread = new Thread(listener);
      listenerThread.start();
      assertThat(countDownLatch.await(3000, TimeUnit.MILLISECONDS)).isTrue();
      listener.shutDown();
      listenerThread.join();

      verify(listener, times(0)).onMessage(any(TestQueuableObject.class));
    }
  }

  @Test
  @Category(UnitTests.class)
  public void shouldRequeueMessageWhenRetriesAreSet() throws Exception {
    try (MaintenanceGuard guard = new MaintenanceGuard(false)) {
      TestQueuableObject message = new TestQueuableObject(1);
      message.setRetries(1);
      listener.setThrowException(true);
      queue.send(message);
      assertThat(queue.count(Filter.ALL)).isEqualTo(1);

      listener.run();

      assertThat(queue.count(Filter.ALL)).isEqualTo(1);
      verify(listener).onMessage(message);
      verify(listener).onException(any(Exception.class), eq(message));
      verify(queue).requeue(message.getId(), 0);
    }
  }

  @Test
  @Category(UnitTests.class)
  public void shouldNotRequeueMessageWhenRetriesAreZero() throws Exception {
    try (MaintenanceGuard guard = new MaintenanceGuard(false)) {
      TestQueuableObject message = new TestQueuableObject(1);
      listener.setThrowException(true);
      queue.send(message);
      assertThat(queue.count(Filter.ALL)).isEqualTo(1);

      listener.run();

      assertThat(queue.count(Filter.ALL)).isEqualTo(0);
      verify(listener).onMessage(message);
      verify(listener).onException(any(Exception.class), eq(message));
    }
  }
}
