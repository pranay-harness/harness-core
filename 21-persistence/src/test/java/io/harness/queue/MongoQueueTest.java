package io.harness.queue;

import static io.harness.rule.OwnerRule.BRETT;
import static io.harness.rule.OwnerRule.GEORGE;
import static io.harness.rule.OwnerRule.PUNEET;
import static io.harness.rule.OwnerRule.UNKNOWN;
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
import io.harness.mongo.queue.MongoQueue;
import io.harness.persistence.HPersistence;
import io.harness.queue.Queuable.QueuableKeys;
import io.harness.queue.Queue.Filter;
import io.harness.rule.OwnerRule.Owner;
import io.harness.version.VersionInfoManager;
import org.junit.Before;
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

  @Inject private Queue<TestVersionedQueuableObject> versionedQueue;

  private MongoQueue<TestVersionedQueuableObject> queue;

  @Before
  public void setup() throws UnknownHostException {
    queue = (MongoQueue<TestVersionedQueuableObject>) versionedQueue;
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void shouldNotGetMessageOnceAcquired() {
    queue.send(new TestVersionedQueuableObject(1));

    assertThat(queue.get(DEFAULT_WAIT, DEFAULT_POLL)).isNotNull();

    // try get message we already have before ack
    assertThat(queue.get(Duration.ZERO, Duration.ZERO)).isNull();
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void shouldReturnMessageInTimeOrder() {
    TestVersionedQueuableObject messageOne = new TestVersionedQueuableObject(1);
    TestVersionedQueuableObject messageTwo = new TestVersionedQueuableObject(2);
    TestVersionedQueuableObject messageThree = new TestVersionedQueuableObject(3);

    queue.send(messageOne);
    queue.send(messageTwo);
    queue.send(messageThree);

    assertThat(queue.get(DEFAULT_WAIT, DEFAULT_POLL)).isEqualTo(messageOne);
    assertThat(queue.get(DEFAULT_WAIT, DEFAULT_POLL)).isEqualTo(messageTwo);
    assertThat(queue.get(DEFAULT_WAIT, DEFAULT_POLL)).isEqualTo(messageThree);
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void shouldWaitForSpecifiedTimePeriodForGetWhenNoMessages() {
    Date start = new Date();
    queue.get(ofSeconds(1), DEFAULT_POLL);
    long elapsed = new Date().getTime() - start.getTime();

    assertThat(elapsed).isBetween(1000L, 3000L);
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void shouldGetMessageWhenAvailableWithinWaitPeriod() {
    Date start = new Date();

    queue.send(new TestVersionedQueuableObject(1));

    queue.get(DEFAULT_WAIT, DEFAULT_POLL);

    assertThat(new Date().getTime() - start.getTime()).isLessThan(2000);
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void shouldNotGetMessageBeforeEarliestGet() throws InterruptedException {
    TestVersionedQueuableObject message = new TestVersionedQueuableObject(1);
    message.setEarliestGet(new Date(System.currentTimeMillis() + 200));
    queue.send(message);

    assertThat(queue.get(Duration.ZERO, Duration.ZERO)).isNull();

    sleep(ofMillis(200));

    assertThat(queue.get(DEFAULT_WAIT, DEFAULT_POLL)).isNotNull();
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void shouldObtainStuckMessageWhenEarliestGetHasExpired() {
    queue.send(new TestVersionedQueuableObject(1));

    queue.setHeartbeat(ZERO);

    assertThat(queue.get(DEFAULT_WAIT, DEFAULT_POLL)).isNotNull();
    assertThat(queue.get(DEFAULT_WAIT, DEFAULT_POLL)).isNotNull();
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void shouldThrowNpeWhenTryToUpdateHeartbeatForNullMessage() {
    assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> queue.updateHeartbeat(null));
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void shouldNotUpdateHeartbeatOfAlreadyExpiredMessage() {
    queue.send(new TestVersionedQueuableObject(1));
    TestVersionedQueuableObject message = queue.get(Duration.ZERO, Duration.ZERO);

    queue.updateHeartbeat(message);

    TestVersionedQueuableObject actual = getDatastore().get(TestVersionedQueuableObject.class, message.getId());

    assertThat(actual.getEarliestGet()).isEqualTo(message.getEarliestGet());
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void shouldNotUpdateHeartbeatOfMessageWhichIsNotRunning() {
    TestVersionedQueuableObject message = new TestVersionedQueuableObject(1);

    queue.send(message);

    queue.updateHeartbeat(message);

    TestVersionedQueuableObject actual = getDatastore().get(TestVersionedQueuableObject.class, message.getId());

    assertThat(actual).isEqualToComparingFieldByField(message);
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void shouldUpdateHeartbeatOfMessageWhileRunning() {
    queue.setHeartbeat(ofSeconds(10));
    queue.send(new TestVersionedQueuableObject(1));

    Date beforeGet = new Date();
    TestVersionedQueuableObject message = queue.get(DEFAULT_WAIT, DEFAULT_POLL);

    Date messageEarliestGet = message.getEarliestGet();

    assertThat(messageEarliestGet).isAfter(beforeGet);
    queue.setHeartbeat(ofSeconds(20));
    queue.updateHeartbeat(message);

    TestVersionedQueuableObject actual = getDatastore().get(TestVersionedQueuableObject.class, message.getId());
    log().info("Actual Timestamp of message = {}", actual.getEarliestGet());

    assertThat(actual.getEarliestGet()).isAfter(messageEarliestGet);

    assertThat(actual).isEqualToComparingFieldByField(message);
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void shouldReturnCountOfObjectsInTheQueue() {
    assertThat(queue.count(Filter.RUNNING)).isEqualTo(0);
    assertThat(queue.count(Filter.NOT_RUNNING)).isEqualTo(0);
    assertThat(queue.count(Filter.ALL)).isEqualTo(0);

    queue.send(new TestVersionedQueuableObject(1));

    assertThat(queue.count(Filter.RUNNING)).isEqualTo(0);
    assertThat(queue.count(Filter.NOT_RUNNING)).isEqualTo(1);
    assertThat(queue.count(Filter.ALL)).isEqualTo(1);

    queue.get(DEFAULT_WAIT, DEFAULT_POLL);

    assertThat(queue.count(Filter.RUNNING)).isEqualTo(1);
    assertThat(queue.count(Filter.NOT_RUNNING)).isEqualTo(0);
    assertThat(queue.count(Filter.ALL)).isEqualTo(1);
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void shouldAckMessage() {
    queue.send(new TestVersionedQueuableObject(0));
    queue.send(new TestVersionedQueuableObject(1));

    TestVersionedQueuableObject result = queue.get(DEFAULT_WAIT, DEFAULT_POLL);

    assertThat(getDatastore().getCount(TestVersionedQueuableObject.class)).isEqualTo(2);

    getDatastore()
        .getCollection(TestVersionedQueuableObject.class)
        .find()
        .forEach(dbObject -> log().debug("TestQueueable = {}", dbObject));
    queue.ack(result);
    assertThat(getDatastore().getCount(TestVersionedQueuableObject.class)).isEqualTo(1);
  }

  @Test
  @Owner(developers = UNKNOWN)
  @Category(UnitTests.class)
  public void shouldThrowNpeWhenAckingNullMessage() {
    assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> queue.ack(null));
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void shouldRequeueMessage() {
    TestVersionedQueuableObject message = new TestVersionedQueuableObject(0);

    queue.send(message);

    TestVersionedQueuableObject resultOne = queue.get(DEFAULT_WAIT, DEFAULT_POLL);

    Date expectedEarliestGet = new Date();
    Date timeBeforeRequeue = new Date();
    queue.requeue(resultOne.getId(), 0, expectedEarliestGet);

    assertThat(getDatastore().getCount(TestVersionedQueuableObject.class)).isEqualTo(1);

    TestVersionedQueuableObject actual = getDatastore().find(TestVersionedQueuableObject.class).get();

    TestVersionedQueuableObject expected = new TestVersionedQueuableObject(0);
    expected.setVersion(versionInfoManager.getVersionInfo().getVersion());
    expected.setEarliestGet(expectedEarliestGet);

    assertThat(actual).isEqualToIgnoringGivenFields(expected, QueuableKeys.id, QueuableKeys.earliestGet);
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void shouldThrowNpeWhenRequeuingWithNullEarliestGet() {
    assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> queue.requeue(QueuableKeys.id, 0, null));
  }

  @Test
  @Owner(developers = UNKNOWN)
  @Category(UnitTests.class)
  public void shouldThrowNpeWhenSendIsCalledWithNullMessage() {
    assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> queue.send(null));
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void shouldSendMessage() {
    TestVersionedQueuableObject message = new TestVersionedQueuableObject(1);

    Date expectedEarliestGet = new Date();
    Date timeBeforeSend = new Date();
    message.setEarliestGet(expectedEarliestGet);
    queue.send(message);

    assertThat(getDatastore().getCount(TestVersionedQueuableObject.class)).isEqualTo(1);

    TestVersionedQueuableObject actual = getDatastore().find(TestVersionedQueuableObject.class).get();

    TestVersionedQueuableObject expected = new TestVersionedQueuableObject(1);
    expected.setVersion(versionInfoManager.getVersionInfo().getVersion());
    expected.setEarliestGet(expectedEarliestGet);

    assertThat(actual).isEqualToIgnoringGivenFields(expected, QueuableKeys.id);
  }

  @Test
  @Owner(developers = GEORGE)
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
  @Owner(developers = PUNEET)
  @Category(UnitTests.class)
  public void shouldFilterWithVersion() {
    Queue<TestVersionedQueuableObject> versionQueue;
    versionQueue = new MongoQueue<>(TestVersionedQueuableObject.class, ofSeconds(5), true);
    on(versionQueue).set("persistence", persistence);
    on(versionQueue).set("versionInfoManager", new VersionInfoManager("version   : 1.0.0"));
    TestVersionedQueuableObject message = new TestVersionedQueuableObject(1);
    versionQueue.send(message);
    on(versionQueue).set("versionInfoManager", new VersionInfoManager("version   : 2.0.0"));
    assertThat(versionQueue.get(DEFAULT_WAIT, DEFAULT_POLL)).isNull();
  }

  @Test
  @Owner(developers = PUNEET)
  @Category(UnitTests.class)
  public void shouldNotFilterWithVersion() {
    Queue<TestVersionedQueuableObject> versionQueue;
    versionQueue = new MongoQueue<>(TestVersionedQueuableObject.class, ofSeconds(5), false);
    on(versionQueue).set("persistence", persistence);
    on(versionQueue).set("versionInfoManager", new VersionInfoManager("version   : 1.0.0"));
    TestVersionedQueuableObject message = new TestVersionedQueuableObject(1);
    versionQueue.send(message);
    on(versionQueue).set("versionInfoManager", new VersionInfoManager("version   : 2.0.0"));
    assertThat(versionQueue.get(DEFAULT_WAIT, DEFAULT_POLL)).isNotNull();
  }
}
