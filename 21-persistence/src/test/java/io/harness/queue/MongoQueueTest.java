package io.harness.queue;

import static io.harness.rule.OwnerRule.GEORGE;
import static io.harness.threading.Morpheus.sleep;
import static java.time.Duration.ZERO;
import static java.time.Duration.ofMillis;
import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.joor.Reflect.on;

import com.google.inject.Inject;

import io.harness.PersistenceTest;
import io.harness.category.element.UnitTests;
import io.harness.mongo.MongoQueue;
import io.harness.persistence.HPersistence;
import io.harness.queue.Queue.Filter;
import io.harness.rule.OwnerRule.Owner;
import io.harness.version.VersionInfoManager;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.net.UnknownHostException;
import java.time.Duration;
import java.util.Date;

public class MongoQueueTest extends PersistenceTest {
  private final Duration DEFAULT_WAIT = ofSeconds(3);
  private final Duration DEFAULT_POLL = ofSeconds(1);

  @Inject private HPersistence persistence;
  @Inject private VersionInfoManager versionInfoManager;

  private MongoQueue<TestQueuableObject> queue;

  @Before
  public void setup() throws UnknownHostException {
    queue = new MongoQueue<>(TestQueuableObject.class);
    on(queue).set("persistence", persistence);
    on(queue).set("versionInfoManager", versionInfoManager);
  }

  @Test
  @Category(UnitTests.class)
  public void shouldNotGetMessageOnceAcquired() {
    queue.send(new TestQueuableObject(1));

    assertThat(queue.get(DEFAULT_WAIT, DEFAULT_POLL)).isNotNull();

    // try get message we already have before ack
    assertThat(queue.get(Duration.ZERO, Duration.ZERO)).isNull();
  }

  @Test
  @Category(UnitTests.class)
  public void shouldReturnMessageInTimeOrder() {
    TestQueuableObject messageOne = new TestQueuableObject(1);
    TestQueuableObject messageTwo = new TestQueuableObject(2);
    TestQueuableObject messageThree = new TestQueuableObject(3);

    queue.send(messageOne);
    queue.send(messageTwo);
    queue.send(messageThree);

    assertThat(queue.get(DEFAULT_WAIT, DEFAULT_POLL)).isEqualTo(messageOne);
    assertThat(queue.get(DEFAULT_WAIT, DEFAULT_POLL)).isEqualTo(messageTwo);
    assertThat(queue.get(DEFAULT_WAIT, DEFAULT_POLL)).isEqualTo(messageThree);
  }

  @Test
  @Category(UnitTests.class)
  public void shouldWaitForSpecifiedTimePeriodForGetWhenNoMessages() {
    Date start = new Date();
    queue.get(ofSeconds(1), DEFAULT_POLL);
    long elapsed = new Date().getTime() - start.getTime();

    assertThat(elapsed).isBetween(1000L, 3000L);
  }

  @Test
  @Category(UnitTests.class)
  public void shouldGetMessageWhenAvailableWithinWaitPeriod() {
    Date start = new Date();

    queue.send(new TestQueuableObject(1));

    queue.get(DEFAULT_WAIT, DEFAULT_POLL);

    assertThat(new Date().getTime() - start.getTime()).isLessThan(2000);
  }

  @Test
  @Category(UnitTests.class)
  public void shouldNotGetMessageBeforeEarliestGet() throws InterruptedException {
    TestQueuableObject message = new TestQueuableObject(1);
    message.setEarliestGet(new Date(System.currentTimeMillis() + 200));
    queue.send(message);

    assertThat(queue.get(Duration.ZERO, Duration.ZERO)).isNull();

    sleep(ofMillis(200));

    assertThat(queue.get(DEFAULT_WAIT, DEFAULT_POLL)).isNotNull();
  }

  @Test
  @Category(UnitTests.class)
  public void shouldResetStuckMessageWhenResetDurationHasExpired() {
    queue.send(new TestQueuableObject(1));

    queue.setHeartbeat(ZERO);
    // sets resetTimestamp on messageOne
    assertThat(queue.get(DEFAULT_WAIT, DEFAULT_POLL)).isNotNull();
    assertThat(queue.get(DEFAULT_WAIT, DEFAULT_POLL)).isNotNull();
  }

  @Test
  @Category(UnitTests.class)
  public void shouldThrowNpeWhenTryToUpdateResetTimestampForNullMessage() {
    assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> queue.updateHeartbeat(null));
  }

  @Test
  @Category(UnitTests.class)
  public void shouldNotExtendResetTimestampOfAlreadyExpiredMessage() {
    queue.send(new TestQueuableObject(1));
    // sets resetTimestamp on messageOne
    TestQueuableObject message = queue.get(Duration.ZERO, Duration.ZERO);

    queue.updateHeartbeat(message);

    TestQueuableObject actual = getDatastore().get(TestQueuableObject.class, message.getId());

    assertThat(actual.getResetTimestamp()).isEqualTo(message.getResetTimestamp());
  }

  @Test
  @Category(UnitTests.class)
  public void shouldNotExtendResetTimestampOfMessageWhichIsNotRunning() {
    TestQueuableObject message = new TestQueuableObject(1);

    queue.send(message);

    queue.updateHeartbeat(message);

    TestQueuableObject actual = getDatastore().get(TestQueuableObject.class, message.getId());

    assertThat(actual).isEqualToComparingFieldByField(message);
  }

  @Test
  @Category(UnitTests.class)
  public void shouldExtendResetTimestampOfMessageWhichIsRunningAndNotExpired() {
    queue.setHeartbeat(ofSeconds(10));
    queue.send(new TestQueuableObject(1));

    Date beforeGet = new Date();
    TestQueuableObject message = queue.get(DEFAULT_WAIT, DEFAULT_POLL);

    Date messageResetTimeStamp = message.getResetTimestamp();

    assertThat(messageResetTimeStamp).isAfter(beforeGet);
    queue.setHeartbeat(ofSeconds(20));
    queue.updateHeartbeat(message);

    TestQueuableObject actual = getDatastore().get(TestQueuableObject.class, message.getId());
    log().info("Actual Timestamp of message = {}", actual.getResetTimestamp());

    assertThat(actual.getResetTimestamp()).isAfter(messageResetTimeStamp);

    assertThat(actual).isEqualToComparingFieldByField(message);
  }

  @Test
  @Category(UnitTests.class)
  public void shouldReturnCountOfObjectsInTheQueue() {
    assertThat(queue.count(Filter.RUNNING)).isEqualTo(0);
    assertThat(queue.count(Filter.NOT_RUNNING)).isEqualTo(0);
    assertThat(queue.count(Filter.ALL)).isEqualTo(0);

    queue.send(new TestQueuableObject(1));

    assertThat(queue.count(Filter.RUNNING)).isEqualTo(0);
    assertThat(queue.count(Filter.NOT_RUNNING)).isEqualTo(1);
    assertThat(queue.count(Filter.ALL)).isEqualTo(1);

    queue.get(DEFAULT_WAIT, DEFAULT_POLL);

    assertThat(queue.count(Filter.RUNNING)).isEqualTo(1);
    assertThat(queue.count(Filter.NOT_RUNNING)).isEqualTo(0);
    assertThat(queue.count(Filter.ALL)).isEqualTo(1);
  }

  @Test
  @Category(UnitTests.class)
  public void shouldAckMessage() {
    queue.send(new TestQueuableObject(0));
    queue.send(new TestQueuableObject(1));

    TestQueuableObject result = queue.get(DEFAULT_WAIT, DEFAULT_POLL);

    assertThat(getDatastore().getCount(TestQueuableObject.class)).isEqualTo(2);

    getDatastore()
        .getCollection(TestQueuableObject.class)
        .find()
        .forEach(dbObject -> log().debug("TestQueueable = {}", dbObject));
    queue.ack(result);
    assertThat(getDatastore().getCount(TestQueuableObject.class)).isEqualTo(1);
  }

  @Test
  @Category(UnitTests.class)
  public void shouldThrowNpeWhenAckingNullMessage() {
    assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> queue.ack(null));
  }

  @Test
  @Category(UnitTests.class)
  public void shouldRequeueMessage() {
    TestQueuableObject message = new TestQueuableObject(0);

    queue.send(message);

    TestQueuableObject resultOne = queue.get(DEFAULT_WAIT, DEFAULT_POLL);

    Date expectedEarliestGet = new Date();
    Date timeBeforeRequeue = new Date();
    queue.requeue(resultOne.getId(), 0, expectedEarliestGet);

    assertThat(getDatastore().getCount(TestQueuableObject.class)).isEqualTo(1);

    TestQueuableObject actual = getDatastore().find(TestQueuableObject.class).get();

    TestQueuableObject expected = new TestQueuableObject(0);
    expected.setVersion(versionInfoManager.getVersionInfo().getVersion());
    expected.setEarliestGet(expectedEarliestGet);
    expected.setCreated(message.getCreated());

    assertThat(actual).isEqualToIgnoringGivenFields(expected, "id", "resetTimestamp");
  }

  @Test
  @Category(UnitTests.class)
  public void shouldThrowNpeWhenRequeuingWithNullEarliestGet() {
    assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> queue.requeue("id", 0, null));
  }

  @Test
  @Category(UnitTests.class)
  public void shouldThrowNpeWhenSendIsCalledWithNullMessage() {
    assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> queue.send(null));
  }

  @Test
  @Owner(emails = GEORGE)
  @Category(UnitTests.class)
  @Ignore("TODO: please provide clear motivation why this test is ignored")
  public void shouldSendMessage() {
    TestQueuableObject message = new TestQueuableObject(1);

    Date expectedEarliestGet = new Date();
    Date timeBeforeSend = new Date();
    message.setEarliestGet(expectedEarliestGet);
    queue.send(message);

    assertThat(getDatastore().getCount(TestQueuableObject.class)).isEqualTo(1);

    TestQueuableObject actual = getDatastore().find(TestQueuableObject.class).get();

    Date actualCreated = actual.getCreated();
    assertThat(actualCreated).isAfterOrEqualTo(timeBeforeSend).isBeforeOrEqualTo(new Date());

    TestQueuableObject expected = new TestQueuableObject(1);
    expected.setVersion(versionInfoManager.getVersionInfo().getVersion());
    expected.setEarliestGet(expectedEarliestGet);
    expected.setCreated(actualCreated);

    assertThat(actual).isEqualToIgnoringGivenFields(expected, "id");
  }

  @Test
  @Category(UnitTests.class)
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

    TestQueuableWithEntity actual = entityQueue.get(DEFAULT_WAIT, DEFAULT_POLL);

    assertThat(actual.getEntity()).isEqualTo(testEntity);
  }

  @Test
  @Category(UnitTests.class)
  public void shouldFilterWithVersion() {
    Queue<TestQueuableObject> versionQueue;
    versionQueue = new MongoQueue<>(TestQueuableObject.class, ofSeconds(5), true);
    on(versionQueue).set("persistence", persistence);
    on(versionQueue).set("versionInfoManager", new VersionInfoManager("version   : 1.0.0"));
    TestQueuableObject message = new TestQueuableObject(1);
    versionQueue.send(message);
    on(versionQueue).set("versionInfoManager", new VersionInfoManager("version   : 2.0.0"));
    assertThat(versionQueue.get(DEFAULT_WAIT, DEFAULT_POLL)).isNull();
  }

  @Test
  @Category(UnitTests.class)
  public void shouldNotFilterWithVersion() {
    Queue<TestQueuableObject> versionQueue;
    versionQueue = new MongoQueue<>(TestQueuableObject.class, ofSeconds(5), false);
    on(versionQueue).set("persistence", persistence);
    on(versionQueue).set("versionInfoManager", new VersionInfoManager("version   : 1.0.0"));
    TestQueuableObject message = new TestQueuableObject(1);
    versionQueue.send(message);
    on(versionQueue).set("versionInfoManager", new VersionInfoManager("version   : 2.0.0"));
    assertThat(versionQueue.get(DEFAULT_WAIT, DEFAULT_POLL)).isNotNull();
  }
}
