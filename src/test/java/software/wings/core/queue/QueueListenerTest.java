package software.wings.core.queue;

import com.google.inject.name.Named;
import com.ifesdjeen.timer.HashedWheelTimer;
import com.ifesdjeen.timer.WaitStrategy;
import org.junit.Before;
import org.junit.Test;
import org.mockito.internal.stubbing.answers.ThrowsException;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.logging.LoggerFactory;
import org.slf4j.Logger;
import software.wings.WingsBaseTest;

import javax.inject.Inject;
import java.net.UnknownHostException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.in;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.*;

/**
 * Created by peeyushaggarwal on 4/13/16.
 */
public class QueueListenerTest extends WingsBaseTest {
  private MongoQueueImpl<TestQueuable> queue;
  private TestQueuableListener listener;

  @Inject @Named("primaryDatastore") private Datastore datastore;

  private static class TestQueuableListener extends AbstractQueueListener<TestQueuable> {
    @Override
    protected void onMessage(TestQueuable message) throws Exception {}
  }

  @Before
  public void setup() throws UnknownHostException {
    queue = spy(new MongoQueueImpl<>(TestQueuable.class, datastore));
    listener = new TestQueuableListener();
    listener.setQueue(queue);
    listener.setRunOnce(true);
    listener.setTimer(new HashedWheelTimer(
        HashedWheelTimer.DEFAULT_RESOLUTION, HashedWheelTimer.DEFAULT_WHEEL_SIZE, new WaitStrategy.YieldingWait()));
    listener = spy(listener);
  }

  @Test
  public void shouldProcessWhenRecievedMessageFromQueue() throws Exception {
    TestQueuable message = new TestQueuable(1);
    queue.send(message);
    assertThat(queue.count()).isEqualTo(1);
    listener.run();
    assertThat(queue.count()).isEqualTo(0);
    verify(listener).onMessage(message);
  }

  @Test
  public void shouldRequeueMessageWhenOnMessageThrowsException() throws Exception {
    TestQueuable message = new TestQueuable(1);
    Exception exception = new Exception();
    doThrow(exception).when(listener).onMessage(message);
    queue.send(message);
    assertThat(queue.count()).isEqualTo(1);

    listener.run();

    assertThat(queue.count()).isEqualTo(1);
    verify(listener).onMessage(message);
    verify(listener).onException(exception, message);
    verify(queue).requeue(message);
  }

  @Test(timeout = 1000)
  public void shouldStopOnInterruptedException() throws Exception {
    listener.setRunOnce(false);

    TestQueuable message = new TestQueuable(1);
    queue.send(message);
    assertThat(queue.count()).isEqualTo(1);

    doThrow(new RuntimeException(new InterruptedException())).when(queue).get();

    listener.run();

    assertThat(queue.count()).isEqualTo(1);
    verify(listener, times(0)).onMessage(any(TestQueuable.class));
  }

  @Test(timeout = 3000)
  public void shouldExtendResetDuration() throws Exception {
    TestQueuable message = new TestQueuable(1);
    queue.send(message);
    assertThat(queue.count()).isEqualTo(1);

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

    assertThat(queue.count()).isEqualTo(0);
    verify(listener).onMessage(message);
    verify(queue, atLeast(1)).updateResetDuration(any(TestQueuable.class));
  }

  @Test(timeout = 5000)
  public void shouldContinueProcessingOnAnyOtherException() throws Exception {
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
        .get();

    Thread listenerThread = new Thread(listener);
    listenerThread.start();
    assertThat(countDownLatch.await(3000, TimeUnit.MILLISECONDS)).isTrue();
    listener.shutDown();
    listenerThread.join();

    verify(listener, times(0)).onMessage(any(TestQueuable.class));
  }
}
