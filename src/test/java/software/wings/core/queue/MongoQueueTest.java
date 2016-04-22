package software.wings.core.queue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.google.common.base.MoreObjects;

import org.junit.Before;
import org.junit.Test;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.PrePersist;
import org.mongodb.morphia.annotations.Reference;

import software.wings.WingsBaseTest;
import software.wings.common.UUIDGenerator;

import java.net.UnknownHostException;
import java.util.Date;
import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * Created by peeyushaggarwal on 4/11/16.
 */
public class MongoQueueTest extends WingsBaseTest {
  @Inject @Named("primaryDatastore") private Datastore datastore;

  private MongoQueueImpl<TestQueuable> queue;

  @Before
  public void setup() throws UnknownHostException {
    queue = new MongoQueueImpl<TestQueuable>(TestQueuable.class, datastore);
  }

  @Test
  public void shouldGetWithNegativeWait() {
    assertThat(queue.get(Integer.MIN_VALUE)).isNull();

    queue.send(new TestQueuable(1));

    assertThat(queue.get(Integer.MIN_VALUE)).isNotNull();
  }

  @Test
  public void shouldGetWhenNegativePoll() {
    assertThat(queue.get(100, Long.MIN_VALUE)).isNull();

    queue.send(new TestQueuable(1));

    assertThat(queue.get(100, Long.MIN_VALUE)).isNotNull();
  }

  @Test
  public void shouldNotGetMessageOnceAcquired() {
    queue.send(new TestQueuable(1));

    assertThat(queue.get()).isNotNull();

    // try get message we already have before ack
    assertThat(queue.get(0)).isNull();
  }

  @Test
  public void shouldReturnMessageBasedOnPriority() {
    TestQueuable messageOne = new TestQueuable(1);
    messageOne.setPriority(0.5);
    TestQueuable messageTwo = new TestQueuable(2);
    messageTwo.setPriority(0.4);
    TestQueuable messageThree = new TestQueuable(3);
    messageThree.setPriority(0.3);

    queue.send(messageOne);
    queue.send(messageTwo);
    queue.send(messageThree);

    assertThat(queue.get()).isEqualTo(messageOne);
    assertThat(queue.get()).isEqualTo(messageTwo);
    assertThat(queue.get()).isEqualTo(messageThree);
  }

  @Test
  public void shouldReturnMessageInTimeOrder() {
    TestQueuable messageOne = new TestQueuable(1);
    TestQueuable messageTwo = new TestQueuable(2);
    TestQueuable messageThree = new TestQueuable(3);

    queue.send(messageOne);
    queue.send(messageTwo);
    queue.send(messageThree);

    assertThat(queue.get()).isEqualTo(messageOne);
    assertThat(queue.get()).isEqualTo(messageTwo);
    assertThat(queue.get()).isEqualTo(messageThree);
  }

  @Test
  public void shouldWaitForSpecifiedTimePeriodForGetWhenNoMessages() {
    Date start = new Date();
    queue.get(1000);
    long elapsed = new Date().getTime() - start.getTime();

    assertThat(elapsed).isBetween(1000L, 2000L);
  }

  @Test
  public void shouldGetMessageWhenAvailableWithinWaitPeriod() {
    Date start = new Date();

    queue.send(new TestQueuable(1));

    queue.get(3000);

    assertThat(new Date().getTime() - start.getTime()).isLessThan(2000);
  }

  @Test
  public void shouldNotGetMessageBeforeEarliestGet() throws InterruptedException {
    TestQueuable message = new TestQueuable(1);
    message.setEarliestGet(new Date(System.currentTimeMillis() + 200));
    queue.send(message);

    assertThat(queue.get(0)).isNull();

    Thread.sleep(200);

    assertThat(queue.get()).isNotNull();
  }

  @Test
  public void shouldResetStuckMessageWhenResetDurationHasExpired() {
    queue.send(new TestQueuable(1));

    queue.resetDuration(0);
    // sets resetTimestamp on messageOne
    assertThat(queue.get()).isNotNull();
    assertThat(queue.get()).isNotNull();
  }

  @Test
  public void shouldThrowNpeWhenTryToUpdateResetDurationForNullMessage() {
    assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> queue.updateResetDuration(null));
  }

  @Test
  public void shouldNotExtendResetTimestampOfAlreadyExpiredMessage() {
    queue.send(new TestQueuable(1));
    // sets resetTimestamp on messageOne
    TestQueuable message = queue.get(0);

    queue.updateResetDuration(message);

    TestQueuable actual = datastore.get(TestQueuable.class, message.getId());

    assertThat(actual.getResetTimestamp()).isEqualTo(message.getResetTimestamp());
  }

  @Test
  public void shouldNotExtendResetTimestampOfMessageWhichIsNotRunning() {
    TestQueuable message = new TestQueuable(1);

    queue.send(message);

    queue.updateResetDuration(message);

    TestQueuable actual = datastore.get(TestQueuable.class, message.getId());

    assertThat(actual).isEqualToComparingFieldByField(message);
  }

  @Test
  public void shouldExtendResetTimestampOfMessageWhichIsRunningAndNotExpired() {
    queue.resetDuration(10);
    queue.send(new TestQueuable(1));

    Date beforeGet = new Date();
    TestQueuable message = queue.get();

    Date messageResetTimeStamp = message.getResetTimestamp();

    assertThat(messageResetTimeStamp).isAfter(beforeGet);
    queue.resetDuration(20);
    queue.updateResetDuration(message);

    TestQueuable actual = datastore.get(TestQueuable.class, message.getId());
    log().info("Actual Timestamp of message = {}", actual.getResetTimestamp());

    assertThat(actual.getResetTimestamp()).isAfter(messageResetTimeStamp);

    assertThat(actual).isEqualToComparingFieldByField(message);
  }

  @Test
  public void shouldReturnCountOfObjectsInTheQueue() {
    assertThat(queue.count(true)).isEqualTo(0);
    assertThat(queue.count(false)).isEqualTo(0);
    assertThat(queue.count()).isEqualTo(0);

    queue.send(new TestQueuable(1));

    assertThat(queue.count(true)).isEqualTo(0);
    assertThat(queue.count(false)).isEqualTo(1);
    assertThat(queue.count()).isEqualTo(1);

    queue.get();

    assertThat(queue.count(true)).isEqualTo(1);
    assertThat(queue.count(false)).isEqualTo(0);
    assertThat(queue.count()).isEqualTo(1);
  }

  @Test
  public void shouldAckMessage() {
    queue.send(new TestQueuable(0));
    queue.send(new TestQueuable(1));

    TestQueuable result = queue.get();

    assertThat(datastore.getCount(TestQueuable.class)).isEqualTo(2);

    datastore.getCollection(TestQueuable.class).find().forEach(dbObject -> log().debug("TestQueueable = {}", dbObject));
    queue.ack(result);
    assertThat(datastore.getCount(TestQueuable.class)).isEqualTo(1);
  }

  @Test
  public void shouldThrowNpeWhenAckingNullMessage() {
    assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> queue.ack(null));
  }

  @Test
  public void shouldReplaceMessageKeepingSameIdOnAckSend() {
    TestQueuable message = new TestQueuable(0);

    queue.send(message);

    assertThat(datastore.getCount(TestQueuable.class)).isEqualTo(1);

    TestQueuable resultOne = queue.get();

    Date expectedEarliestGet = new Date();
    double expectedPriority = 0.8;
    Date timeBeforeAckSend = new Date();
    TestQueuable toBeSent = new TestQueuable(1);
    toBeSent.setEarliestGet(expectedEarliestGet);
    toBeSent.setPriority(expectedPriority);
    queue.ackSend(resultOne, toBeSent);

    assertThat(datastore.getCount(TestQueuable.class)).isEqualTo(1);

    TestQueuable actual = datastore.find(TestQueuable.class).get();

    Date actualCreated = actual.getCreated();
    assertThat(actualCreated).isAfterOrEqualsTo(timeBeforeAckSend).isBeforeOrEqualsTo(new Date());

    TestQueuable expected = new TestQueuable(1);
    expected.setEarliestGet(expectedEarliestGet);
    expected.setPriority(expectedPriority);
    expected.setCreated(actualCreated);

    assertThat(actual).isEqualToIgnoringGivenFields(expected, "id");
  }

  @Test
  public void shouldThrowNpeWhenAckSendIsCalledWithNullMessage() {
    assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> queue.ackSend(null, new TestQueuable(1)));
  }

  @Test
  public void shouldThrowNpeWhenAckSendIsCalledWithNullReplacementMessage() {
    assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> queue.ackSend(new TestQueuable(1), null));
  }

  @Test
  public void shouldRequeueMessage() throws Exception {
    TestQueuable message = new TestQueuable(0);

    queue.send(message);

    TestQueuable resultOne = queue.get();

    Date expectedEarliestGet = new Date();
    double expectedPriority = 0.8;
    Date timeBeforeRequeue = new Date();
    queue.requeue(resultOne, expectedEarliestGet, expectedPriority);

    assertThat(datastore.getCount(TestQueuable.class)).isEqualTo(1);

    TestQueuable actual = datastore.find(TestQueuable.class).get();

    Date actualCreated = actual.getCreated();
    assertThat(actualCreated).isAfterOrEqualsTo(timeBeforeRequeue).isBeforeOrEqualsTo(new Date());

    TestQueuable expected = new TestQueuable(0);
    expected.setEarliestGet(expectedEarliestGet);
    expected.setPriority(expectedPriority);
    expected.setCreated(actualCreated);

    assertThat(actual).isEqualToIgnoringGivenFields(expected, "id");
  }

  @Test
  public void shouldThrowIllegalArgumentExceptionWhenRequeuedWithPriorityNaN() throws Exception {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> queue.requeue(new TestQueuable(1), new Date(), Double.NaN));
  }

  @Test
  public void shouldThrowNpeWhenRequeueNullMessage() {
    assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> queue.requeue(null));
  }

  @Test
  public void shouldThrowNpeWhenRequeuingWithNullEarliestGet() throws Exception {
    assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> queue.requeue(new TestQueuable(1), null));
  }

  @Test
  public void shouldThrowNpeWhenSendIsCalledWithNullMessage() {
    assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> queue.send(null));
  }

  @Test
  public void shouldSendMessage() {
    TestQueuable message = new TestQueuable(1);

    Date expectedEarliestGet = new Date();
    double expectedPriority = 0.8;
    Date timeBeforeSend = new Date();
    message.setPriority(expectedPriority);
    message.setEarliestGet(expectedEarliestGet);
    queue.send(message);

    assertThat(datastore.getCount(TestQueuable.class)).isEqualTo(1);

    TestQueuable actual = datastore.find(TestQueuable.class).get();

    Date actualCreated = actual.getCreated();
    assertThat(actualCreated).isAfterOrEqualsTo(timeBeforeSend).isBeforeOrEqualsTo(new Date());

    TestQueuable expected = new TestQueuable(1);
    expected.setEarliestGet(expectedEarliestGet);
    expected.setPriority(expectedPriority);
    expected.setCreated(actualCreated);

    assertThat(actual).isEqualToIgnoringGivenFields(expected, "id");
  }

  @Test
  public void shouldSendAndGetMessageWithEntityReference() {
    Queue<TestQueuableWithEntity> entityQueue;
    entityQueue = new MongoQueueImpl<TestQueuableWithEntity>(TestQueuableWithEntity.class, datastore);

    TestEntity testEntity = new TestEntity(1);
    datastore.save(testEntity);

    TestQueuableWithEntity message = new TestQueuableWithEntity(testEntity);

    entityQueue.send(message);

    assertThat(datastore.getCount(TestQueuableWithEntity.class)).isEqualTo(1);

    TestQueuableWithEntity actual = entityQueue.get();

    assertThat(actual.getEntity()).isEqualTo(testEntity);
  }

  @Entity(value = "testEntity")
  private static class TestEntity {
    @Id private String id;
    private int data;

    public TestEntity() {}

    public TestEntity(int data) {
      this.data = data;
    }

    public String getId() {
      return id;
    }

    public void setId(String id) {
      this.id = id;
    }

    public int getData() {
      return data;
    }

    public void setData(int data) {
      this.data = data;
    }

    @Override
    public int hashCode() {
      return Objects.hash(id, data);
    }

    @PrePersist
    public void onUpdate() {
      if (id == null) {
        id = UUIDGenerator.getUuid();
      }
    }

    @Override
    public boolean equals(Object o) {
      if (this == o)
        return true;
      if (o == null || getClass() != o.getClass())
        return false;
      TestEntity that = (TestEntity) o;
      return data == that.data && Objects.equals(id, that.id);
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this).add("id", id).add("data", data).toString();
    }
  }

  @Entity(value = "testEntityQueue", noClassnameStored = true)
  public static class TestQueuableWithEntity extends Queuable {
    @Reference private TestEntity entity;

    public TestQueuableWithEntity() {}

    public TestQueuableWithEntity(TestQueuableWithEntity other) {
      super(other);
      this.entity = other.entity;
    }

    public TestQueuableWithEntity(TestEntity entity) {
      this.entity = entity;
    }

    public TestEntity getEntity() {
      return entity;
    }

    public void setEntity(TestEntity entity) {
      this.entity = entity;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o)
        return true;
      if (o == null || getClass() != o.getClass())
        return false;
      TestQueuableWithEntity that = (TestQueuableWithEntity) o;
      return Objects.equals(entity, that.entity);
    }

    @Override
    public int hashCode() {
      return Objects.hash(entity);
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this).add("entity", entity).toString();
    }
  }
}
