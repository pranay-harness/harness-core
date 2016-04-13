package software.wings.core.queue;

import com.google.inject.name.Named;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.mongodb.morphia.Datastore;
import software.wings.WingsBaseTest;

import javax.inject.Inject;
import java.net.UnknownHostException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.joor.Reflect.on;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.*;

/**
 * Created by peeyushaggarwal on 4/13/16.
 */
public class QueueListenerTest extends WingsBaseTest {
  private Queue<TestQueuable> queue;
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
  public void shouldStopOnInterruptedExeception() throws Exception {
    listener.setRunOnce(false);

    TestQueuable message = new TestQueuable(1);
    queue.send(message);
    assertThat(queue.count()).isEqualTo(1);

    doThrow(new RuntimeException(new InterruptedException())).when(queue).get(anyInt());

    listener.run();

    assertThat(queue.count()).isEqualTo(1);
    verify(listener, times(0)).onMessage(any(TestQueuable.class));
  }
}
