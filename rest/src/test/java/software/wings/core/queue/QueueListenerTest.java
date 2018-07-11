package software.wings.core.queue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import io.harness.rule.RepeatRule.Repeat;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.internal.stubbing.answers.ThrowsException;
import org.mockito.invocation.InvocationOnMock;
import org.mongodb.morphia.AdvancedDatastore;
import software.wings.WingsBaseTest;
import software.wings.core.managerConfiguration.ConfigurationController;

import java.net.UnknownHostException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by peeyushaggarwal on 4/13/16.
 */
public class QueueListenerTest extends WingsBaseTest {
  private MongoQueueImpl<QueuableObject> queue;
  private QueuableObjectListener listener;

  @Inject @Named("primaryDatastore") private AdvancedDatastore datastore;

  /**
   * Setup.
   *
   * @throws UnknownHostException the unknown host exception
   */
  @Before
  public void setup() throws UnknownHostException {
    queue = spy(new MongoQueueImpl<>(QueuableObject.class, datastore));
    listener = new QueuableObjectListener();
    listener.setQueue(queue);
    listener.setRunOnce(true);
    listener.setTimer(new ScheduledThreadPoolExecutor(1));
    listener.setConfigurationController(new ConfigurationController(1));
    listener = spy(listener);
  }

  /**
   * Should process when recieved message from queue.
   *
   * @throws Exception the exception
   */
  @Test
  @Ignore
  public void shouldProcessWhenRecievedMessageFromQueue() {
    QueuableObject message = new QueuableObject(1);
    queue.send(message);
    assertThat(queue.count()).isEqualTo(1);
    listener.run();
    assertThat(queue.count()).isEqualTo(0);
    verify(listener).onMessage(message);
  }

  /**
   * Should stop on interrupted exception.
   *
   * @throws Exception the exception
   */
  @Test(timeout = 1000)
  public void shouldStopOnInterruptedException() throws Exception {
    listener.setRunOnce(false);

    QueuableObject message = new QueuableObject(1);
    queue.send(message);
    assertThat(queue.count()).isEqualTo(1);

    doThrow(new RuntimeException(new InterruptedException())).when(queue).get();

    listener.run();

    assertThat(queue.count()).isEqualTo(1);
    verify(listener, times(0)).onMessage(any(QueuableObject.class));
  }

  /**
   * Should extend reset duration.
   *
   * @throws Exception the exception
   */
  @Test(timeout = 5000)
  @Repeat(times = 3, successes = 1)
  public void shouldExtendResetDuration() throws Exception {
    QueuableObject message = new QueuableObject(1);
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
    verify(queue, atLeast(1)).updateResetDuration(any(QueuableObject.class));
  }

  /**
   * Should continue processing on any other exception.
   *
   * @throws Exception the exception
   */
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

    verify(listener, times(0)).onMessage(any(QueuableObject.class));
  }

  /**
   * Should requeue message when retries are set.
   *
   * @throws Exception the exception
   */
  @Test
  public void shouldRequeueMessageWhenRetriesAreSet() throws Exception {
    QueuableObject message = new QueuableObject(1);
    message.setRetries(1);
    listener.setThrowException(true);
    queue.send(message);
    assertThat(queue.count()).isEqualTo(1);

    listener.run();

    assertThat(queue.count()).isEqualTo(1);
    verify(listener).onMessage(message);
    verify(listener).onException(any(Exception.class), eq(message));
    verify(queue).requeue(message);
  }

  /**
   * Should not requeue message when retries are zero.
   *
   * @throws Exception the exception
   */
  @Test
  public void shouldNotRequeueMessageWhenRetriesAreZero() throws Exception {
    QueuableObject message = new QueuableObject(1);
    listener.setThrowException(true);
    queue.send(message);
    assertThat(queue.count()).isEqualTo(1);

    listener.run();

    assertThat(queue.count()).isEqualTo(1);
    verify(listener).onMessage(message);
    verify(listener).onException(any(Exception.class), eq(message));
    verify(queue, times(0)).requeue(message);
  }

  private static class QueuableObjectListener extends AbstractQueueListener<QueuableObject> {
    private boolean throwException;

    /**
     * Sets throw exception.
     *
     * @param throwException the throw exception
     */
    public void setThrowException(boolean throwException) {
      this.throwException = throwException;
    }

    @Override
    protected void onMessage(QueuableObject message) {
      if (throwException) {
        throw new RuntimeException("Expected Exception In Test.");
      }
    }
  }
}
