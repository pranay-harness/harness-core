package io.harness.queue;

import static io.harness.threading.Morpheus.sleep;
import static java.time.Duration.ofMillis;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.joor.Reflect.on;

import com.google.inject.Inject;

import io.harness.PersistenceTest;
import io.harness.mongo.MongoQueue;
import io.harness.persistence.HPersistence;
import io.harness.queue.Queue.Filter;
import io.harness.rule.RepeatRule.Repeat;
import io.harness.version.VersionInfoManager;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.net.UnknownHostException;
import java.util.Date;

public class MongoQueueTest extends PersistenceTest {
  @Inject private HPersistence persistence;
  @Inject private VersionInfoManager versionInfoManager;

  private MongoQueue<TestQueuableObject> queue;

  /**
   * Setup.
   *
   * @throws UnknownHostException the unknown host exception
   */
  @Before
  public void setup() throws UnknownHostException {
    queue = new MongoQueue<>(TestQueuableObject.class);
    on(queue).set("persistence", persistence);
    on(queue).set("versionInfoManager", versionInfoManager);
  }

  /**
   * Should get with negative wait.
   */
  @Test
  public void shouldGetWithNegativeWait() {
    assertThat(queue.get(Integer.MIN_VALUE)).isNull();

    queue.send(new TestQueuableObject(1));

    assertThat(queue.get(Integer.MIN_VALUE)).isNotNull();
  }

  /**
   * Should get when negative poll.
   */
  @Test
  public void shouldGetWhenNegativePoll() {
    assertThat(queue.get(100, Long.MIN_VALUE)).isNull();

    queue.send(new TestQueuableObject(1));

    assertThat(queue.get(100, Long.MIN_VALUE)).isNotNull();
  }

  /**
   * Should not get message once acquired.
   */
  @Test
  public void shouldNotGetMessageOnceAcquired() {
    queue.send(new TestQueuableObject(1));

    assertThat(queue.get()).isNotNull();

    // try get message we already have before ack
    assertThat(queue.get(0)).isNull();
  }

  /**
   * Should return message based on priority.
   */
  @Test
  public void shouldReturnMessageBasedOnPriority() {
    TestQueuableObject messageTwo = new TestQueuableObject(2);
    messageTwo.setPriority(0.4);
    TestQueuableObject messageOne = new TestQueuableObject(1);
    messageOne.setPriority(0.5);
    TestQueuableObject messageThree = new TestQueuableObject(3);
    messageThree.setPriority(0.3);

    queue.send(messageTwo);
    sleep(ofMillis(2));
    queue.send(messageOne);
    sleep(ofMillis(2));
    queue.send(messageThree);

    assertThat(queue.get()).isEqualTo(messageOne);
    assertThat(queue.get()).isEqualTo(messageTwo);
    assertThat(queue.get()).isEqualTo(messageThree);
  }

  /**
   * Should return message in time order.
   */
  @Test
  public void shouldReturnMessageInTimeOrder() {
    TestQueuableObject messageOne = new TestQueuableObject(1);
    TestQueuableObject messageTwo = new TestQueuableObject(2);
    TestQueuableObject messageThree = new TestQueuableObject(3);

    queue.send(messageOne);
    queue.send(messageTwo);
    queue.send(messageThree);

    assertThat(queue.get()).isEqualTo(messageOne);
    assertThat(queue.get()).isEqualTo(messageTwo);
    assertThat(queue.get()).isEqualTo(messageThree);
  }

  /**
   * Should wait for specified time period for get when no messages.
   */
  @Test
  public void shouldWaitForSpecifiedTimePeriodForGetWhenNoMessages() {
    Date start = new Date();
    queue.get(1000);
    long elapsed = new Date().getTime() - start.getTime();

    assertThat(elapsed).isBetween(1000L, 3000L);
  }

  /**
   * Should get message when available within wait period.
   */
  @Test
  @Repeat(times = 3, successes = 1)
  public void shouldGetMessageWhenAvailableWithinWaitPeriod() {
    Date start = new Date();

    queue.send(new TestQueuableObject(1));

    queue.get(3000);

    assertThat(new Date().getTime() - start.getTime()).isLessThan(2000);
  }

  /**
   * Should not get message before earliest get.
   *
   * @throws InterruptedException the interrupted exception
   */
  @Test
  @Repeat(times = 3, successes = 1)
  public void shouldNotGetMessageBeforeEarliestGet() throws InterruptedException {
    TestQueuableObject message = new TestQueuableObject(1);
    message.setEarliestGet(new Date(System.currentTimeMillis() + 200));
    queue.send(message);

    assertThat(queue.get(0)).isNull();

    Thread.sleep(200);

    assertThat(queue.get()).isNotNull();
  }

  /**
   * Should reset stuck message when reset duration has expired.
   */
  @Test
  public void shouldResetStuckMessageWhenResetDurationHasExpired() {
    queue.send(new TestQueuableObject(1));

    queue.resetDuration(0);
    // sets resetTimestamp on messageOne
    assertThat(queue.get()).isNotNull();
    assertThat(queue.get()).isNotNull();
  }

  /**
   * Should throw npe when try to update reset duration for null message.
   */
  @Test
  public void shouldThrowNpeWhenTryToUpdateResetDurationForNullMessage() {
    assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> queue.updateResetDuration(null));
  }

  /**
   * Should not extend reset timestamp of already expired message.
   */
  @Test
  public void shouldNotExtendResetTimestampOfAlreadyExpiredMessage() {
    queue.send(new TestQueuableObject(1));
    // sets resetTimestamp on messageOne
    TestQueuableObject message = queue.get(0);

    queue.updateResetDuration(message);

    TestQueuableObject actual = getDatastore().get(TestQueuableObject.class, message.getId());

    assertThat(actual.getResetTimestamp()).isEqualTo(message.getResetTimestamp());
  }

  /**
   * Should not extend reset timestamp of message which is not running.
   */
  @Test
  public void shouldNotExtendResetTimestampOfMessageWhichIsNotRunning() {
    TestQueuableObject message = new TestQueuableObject(1);

    queue.send(message);

    queue.updateResetDuration(message);

    TestQueuableObject actual = getDatastore().get(TestQueuableObject.class, message.getId());

    assertThat(actual).isEqualToComparingFieldByField(message);
  }

  /**
   * Should extend reset timestamp of message which is running and not expired.
   */
  @Test
  public void shouldExtendResetTimestampOfMessageWhichIsRunningAndNotExpired() {
    queue.resetDuration(10);
    queue.send(new TestQueuableObject(1));

    Date beforeGet = new Date();
    TestQueuableObject message = queue.get();

    Date messageResetTimeStamp = message.getResetTimestamp();

    assertThat(messageResetTimeStamp).isAfter(beforeGet);
    queue.resetDuration(20);
    queue.updateResetDuration(message);

    TestQueuableObject actual = getDatastore().get(TestQueuableObject.class, message.getId());
    log().info("Actual Timestamp of message = {}", actual.getResetTimestamp());

    assertThat(actual.getResetTimestamp()).isAfter(messageResetTimeStamp);

    assertThat(actual).isEqualToComparingFieldByField(message);
  }

  /**
   * Should return count of objects in the queue.
   */
  @Test
  public void shouldReturnCountOfObjectsInTheQueue() {
    assertThat(queue.count(Filter.RUNNING)).isEqualTo(0);
    assertThat(queue.count(Filter.NOT_RUNNING)).isEqualTo(0);
    assertThat(queue.count(Filter.ALL)).isEqualTo(0);

    queue.send(new TestQueuableObject(1));

    assertThat(queue.count(Filter.RUNNING)).isEqualTo(0);
    assertThat(queue.count(Filter.NOT_RUNNING)).isEqualTo(1);
    assertThat(queue.count(Filter.ALL)).isEqualTo(1);

    queue.get();

    assertThat(queue.count(Filter.RUNNING)).isEqualTo(1);
    assertThat(queue.count(Filter.NOT_RUNNING)).isEqualTo(0);
    assertThat(queue.count(Filter.ALL)).isEqualTo(1);
  }

  /**
   * Should ack message.
   */
  @Test
  public void shouldAckMessage() {
    queue.send(new TestQueuableObject(0));
    queue.send(new TestQueuableObject(1));

    TestQueuableObject result = queue.get();

    assertThat(getDatastore().getCount(TestQueuableObject.class)).isEqualTo(2);

    getDatastore()
        .getCollection(TestQueuableObject.class)
        .find()
        .forEach(dbObject -> log().debug("TestQueueable = {}", dbObject));
    queue.ack(result);
    assertThat(getDatastore().getCount(TestQueuableObject.class)).isEqualTo(1);
  }

  /**
   * Should throw npe when acking null message.
   */
  @Test
  public void shouldThrowNpeWhenAckingNullMessage() {
    assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> queue.ack(null));
  }

  /**
   * Should replace message keeping same id on ack send.
   */
  @Test
  public void shouldReplaceMessageKeepingSameIdOnAckSend() {
    TestQueuableObject message = new TestQueuableObject(0);

    queue.send(message);

    assertThat(getDatastore().getCount(TestQueuableObject.class)).isEqualTo(1);

    TestQueuableObject resultOne = queue.get();

    Date expectedEarliestGet = new Date();
    double expectedPriority = 0.8;
    Date timeBeforeAckSend = new Date();
    TestQueuableObject toBeSent = new TestQueuableObject(1);
    toBeSent.setEarliestGet(expectedEarliestGet);
    toBeSent.setPriority(expectedPriority);
    queue.ackSend(resultOne, toBeSent);

    assertThat(getDatastore().getCount(TestQueuableObject.class)).isEqualTo(1);

    TestQueuableObject actual = getDatastore().find(TestQueuableObject.class).get();

    Date actualCreated = actual.getCreated();
    assertThat(actualCreated).isAfterOrEqualsTo(timeBeforeAckSend).isBeforeOrEqualsTo(new Date());

    TestQueuableObject expected = new TestQueuableObject(1);
    expected.setEarliestGet(expectedEarliestGet);
    expected.setPriority(expectedPriority);
    expected.setCreated(actualCreated);

    assertThat(actual).isEqualToIgnoringGivenFields(expected, "id");
  }

  /**
   * Should throw npe when ack send is called with null message.
   */
  @Test
  public void shouldThrowNpeWhenAckSendIsCalledWithNullMessage() {
    assertThatExceptionOfType(NullPointerException.class)
        .isThrownBy(() -> queue.ackSend(null, new TestQueuableObject(1)));
  }

  /**
   * Should throw npe when ack send is called with null replacement message.
   */
  @Test
  public void shouldThrowNpeWhenAckSendIsCalledWithNullReplacementMessage() {
    assertThatExceptionOfType(NullPointerException.class)
        .isThrownBy(() -> queue.ackSend(new TestQueuableObject(1), null));
  }

  /**
   * Should requeue message.
   */
  @Test
  public void shouldRequeueMessage() {
    TestQueuableObject message = new TestQueuableObject(0);

    queue.send(message);

    TestQueuableObject resultOne = queue.get();

    Date expectedEarliestGet = new Date();
    double expectedPriority = 0.8;
    Date timeBeforeRequeue = new Date();
    queue.requeue(resultOne, expectedEarliestGet, expectedPriority);

    assertThat(getDatastore().getCount(TestQueuableObject.class)).isEqualTo(1);

    TestQueuableObject actual = getDatastore().find(TestQueuableObject.class).get();

    Date actualCreated = actual.getCreated();
    assertThat(actualCreated).isAfterOrEqualsTo(timeBeforeRequeue).isBeforeOrEqualsTo(new Date());

    TestQueuableObject expected = new TestQueuableObject(0);
    expected.setVersion(versionInfoManager.getVersionInfo().getVersion());
    expected.setEarliestGet(expectedEarliestGet);
    expected.setPriority(expectedPriority);
    expected.setCreated(actualCreated);

    assertThat(actual).isEqualToIgnoringGivenFields(expected, "id");
  }

  /**
   * Should throw illegal argument exception when requeued with priority na n.
   */
  @Test
  public void shouldThrowIllegalArgumentExceptionWhenRequeuedWithPriorityNaN() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> queue.requeue(new TestQueuableObject(1), new Date(), Double.NaN));
  }

  /**
   * Should throw npe when requeue null message.
   */
  @Test
  public void shouldThrowNpeWhenRequeueNullMessage() {
    assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> queue.requeue(null));
  }

  /**
   * Should throw npe when requeuing with null earliest get.
   */
  @Test
  public void shouldThrowNpeWhenRequeuingWithNullEarliestGet() {
    assertThatExceptionOfType(NullPointerException.class)
        .isThrownBy(() -> queue.requeue(new TestQueuableObject(1), null));
  }

  /**
   * Should throw npe when send is called with null message.
   */
  @Test
  public void shouldThrowNpeWhenSendIsCalledWithNullMessage() {
    assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> queue.send(null));
  }

  /**
   * Should send message.
   */
  @Test
  @Ignore // this test is intermittently failing
  public void shouldSendMessage() {
    TestQueuableObject message = new TestQueuableObject(1);

    Date expectedEarliestGet = new Date();
    double expectedPriority = 0.8;
    Date timeBeforeSend = new Date();
    message.setPriority(expectedPriority);
    message.setEarliestGet(expectedEarliestGet);
    queue.send(message);

    assertThat(getDatastore().getCount(TestQueuableObject.class)).isEqualTo(1);

    TestQueuableObject actual = getDatastore().find(TestQueuableObject.class).get();

    Date actualCreated = actual.getCreated();
    assertThat(actualCreated).isAfterOrEqualsTo(timeBeforeSend).isBeforeOrEqualsTo(new Date());

    TestQueuableObject expected = new TestQueuableObject(1);
    expected.setVersion(versionInfoManager.getVersionInfo().getVersion());
    expected.setEarliestGet(expectedEarliestGet);
    expected.setPriority(expectedPriority);
    expected.setCreated(actualCreated);

    assertThat(actual).isEqualToIgnoringGivenFields(expected, "id");
  }

  /**
   * Should send and get message with entity reference.
   */
  @Test
  public void shouldSendAndGetMessageWithEntityReference() {
    Queue<TestQueuableWithEntity> entityQueue;
    entityQueue = new MongoQueue<>(TestQueuableWithEntity.class);
    on(entityQueue).set("persistence", persistence);
    on(entityQueue).set("versionInfoManager", versionInfoManager);

    TestInternalEntity testEntity = TestInternalEntity.builder().id("1").build();
    getDatastore().save(testEntity);

    TestQueuableWithEntity message = new TestQueuableWithEntity(testEntity);

    entityQueue.send(message);

    assertThat(getDatastore().getCount(TestQueuableWithEntity.class)).isEqualTo(1);

    TestQueuableWithEntity actual = entityQueue.get();

    assertThat(actual.getEntity()).isEqualTo(testEntity);
  }

  @Test
  public void shouldFilterWithVersion() {
    Queue<TestQueuableObject> versionQueue;
    versionQueue = new MongoQueue<>(TestQueuableObject.class, 5, true);
    on(versionQueue).set("persistence", persistence);
    on(versionQueue).set("versionInfoManager", new VersionInfoManager("version   : 1.0.0"));
    TestQueuableObject message = new TestQueuableObject(1);
    versionQueue.send(message);
    on(versionQueue).set("versionInfoManager", new VersionInfoManager("version   : 2.0.0"));
    assertThat(versionQueue.get()).isNull();
  }

  @Test
  public void shouldNotFilterWithVersion() {
    Queue<TestQueuableObject> versionQueue;
    versionQueue = new MongoQueue<>(TestQueuableObject.class, 5, false);
    on(versionQueue).set("persistence", persistence);
    on(versionQueue).set("versionInfoManager", new VersionInfoManager("version   : 1.0.0"));
    TestQueuableObject message = new TestQueuableObject(1);
    versionQueue.send(message);
    on(versionQueue).set("versionInfoManager", new VersionInfoManager("version   : 2.0.0"));
    assertThat(versionQueue.get()).isNotNull();
  }
}
