package software.wings.core.queue;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.google.common.base.MoreObjects;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import io.harness.rule.RepeatRule.Repeat;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mongodb.morphia.AdvancedDatastore;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.PrePersist;
import org.mongodb.morphia.annotations.Reference;
import software.wings.WingsBaseTest;

import java.net.UnknownHostException;
import java.util.Date;
import java.util.Objects;

/**
 * Created by peeyushaggarwal on 4/11/16.
 */
public class MongoQueueTest extends WingsBaseTest {
  @Inject @Named("primaryDatastore") private AdvancedDatastore datastore;

  private MongoQueueImpl<QueuableObject> queue;

  /**
   * Setup.
   *
   * @throws UnknownHostException the unknown host exception
   */
  @Before
  public void setup() throws UnknownHostException {
    queue = new MongoQueueImpl<>(QueuableObject.class, datastore);
  }

  /**
   * Should get with negative wait.
   */
  @Test
  public void shouldGetWithNegativeWait() {
    assertThat(queue.get(Integer.MIN_VALUE)).isNull();

    queue.send(new QueuableObject(1));

    assertThat(queue.get(Integer.MIN_VALUE)).isNotNull();
  }

  /**
   * Should get when negative poll.
   */
  @Test
  public void shouldGetWhenNegativePoll() {
    assertThat(queue.get(100, Long.MIN_VALUE)).isNull();

    queue.send(new QueuableObject(1));

    assertThat(queue.get(100, Long.MIN_VALUE)).isNotNull();
  }

  /**
   * Should not get message once acquired.
   */
  @Test
  public void shouldNotGetMessageOnceAcquired() {
    queue.send(new QueuableObject(1));

    assertThat(queue.get()).isNotNull();

    // try get message we already have before ack
    assertThat(queue.get(0)).isNull();
  }

  /**
   * Should return message based on priority.
   */
  @Test
  public void shouldReturnMessageBasedOnPriority() {
    QueuableObject messageTwo = new QueuableObject(2);
    messageTwo.setPriority(0.4);
    QueuableObject messageOne = new QueuableObject(1);
    messageOne.setPriority(0.5);
    QueuableObject messageThree = new QueuableObject(3);
    messageThree.setPriority(0.3);

    queue.send(messageOne);
    queue.send(messageTwo);
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
    QueuableObject messageOne = new QueuableObject(1);
    QueuableObject messageTwo = new QueuableObject(2);
    QueuableObject messageThree = new QueuableObject(3);

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

    queue.send(new QueuableObject(1));

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
    QueuableObject message = new QueuableObject(1);
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
    queue.send(new QueuableObject(1));

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
    queue.send(new QueuableObject(1));
    // sets resetTimestamp on messageOne
    QueuableObject message = queue.get(0);

    queue.updateResetDuration(message);

    QueuableObject actual = datastore.get(QueuableObject.class, message.getId());

    assertThat(actual.getResetTimestamp()).isEqualTo(message.getResetTimestamp());
  }

  /**
   * Should not extend reset timestamp of message which is not running.
   */
  @Test
  public void shouldNotExtendResetTimestampOfMessageWhichIsNotRunning() {
    QueuableObject message = new QueuableObject(1);

    queue.send(message);

    queue.updateResetDuration(message);

    QueuableObject actual = datastore.get(QueuableObject.class, message.getId());

    assertThat(actual).isEqualToComparingFieldByField(message);
  }

  /**
   * Should extend reset timestamp of message which is running and not expired.
   */
  @Test
  public void shouldExtendResetTimestampOfMessageWhichIsRunningAndNotExpired() {
    queue.resetDuration(10);
    queue.send(new QueuableObject(1));

    Date beforeGet = new Date();
    QueuableObject message = queue.get();

    Date messageResetTimeStamp = message.getResetTimestamp();

    assertThat(messageResetTimeStamp).isAfter(beforeGet);
    queue.resetDuration(20);
    queue.updateResetDuration(message);

    QueuableObject actual = datastore.get(QueuableObject.class, message.getId());
    log().info("Actual Timestamp of message = {}", actual.getResetTimestamp());

    assertThat(actual.getResetTimestamp()).isAfter(messageResetTimeStamp);

    assertThat(actual).isEqualToComparingFieldByField(message);
  }

  /**
   * Should return count of objects in the queue.
   */
  @Test
  public void shouldReturnCountOfObjectsInTheQueue() {
    assertThat(queue.count(true)).isEqualTo(0);
    assertThat(queue.count(false)).isEqualTo(0);
    assertThat(queue.count()).isEqualTo(0);

    queue.send(new QueuableObject(1));

    assertThat(queue.count(true)).isEqualTo(0);
    assertThat(queue.count(false)).isEqualTo(1);
    assertThat(queue.count()).isEqualTo(1);

    queue.get();

    assertThat(queue.count(true)).isEqualTo(1);
    assertThat(queue.count(false)).isEqualTo(0);
    assertThat(queue.count()).isEqualTo(1);
  }

  /**
   * Should ack message.
   */
  @Test
  public void shouldAckMessage() {
    queue.send(new QueuableObject(0));
    queue.send(new QueuableObject(1));

    QueuableObject result = queue.get();

    assertThat(datastore.getCount(QueuableObject.class)).isEqualTo(2);

    datastore.getCollection(QueuableObject.class)
        .find()
        .forEach(dbObject -> log().debug("TestQueueable = {}", dbObject));
    queue.ack(result);
    assertThat(datastore.getCount(QueuableObject.class)).isEqualTo(1);
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
    QueuableObject message = new QueuableObject(0);

    queue.send(message);

    assertThat(datastore.getCount(QueuableObject.class)).isEqualTo(1);

    QueuableObject resultOne = queue.get();

    Date expectedEarliestGet = new Date();
    double expectedPriority = 0.8;
    Date timeBeforeAckSend = new Date();
    QueuableObject toBeSent = new QueuableObject(1);
    toBeSent.setEarliestGet(expectedEarliestGet);
    toBeSent.setPriority(expectedPriority);
    queue.ackSend(resultOne, toBeSent);

    assertThat(datastore.getCount(QueuableObject.class)).isEqualTo(1);

    QueuableObject actual = datastore.find(QueuableObject.class).get();

    Date actualCreated = actual.getCreated();
    assertThat(actualCreated).isAfterOrEqualsTo(timeBeforeAckSend).isBeforeOrEqualsTo(new Date());

    QueuableObject expected = new QueuableObject(1);
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
    assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> queue.ackSend(null, new QueuableObject(1)));
  }

  /**
   * Should throw npe when ack send is called with null replacement message.
   */
  @Test
  public void shouldThrowNpeWhenAckSendIsCalledWithNullReplacementMessage() {
    assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> queue.ackSend(new QueuableObject(1), null));
  }

  /**
   * Should requeue message.
   */
  @Test
  public void shouldRequeueMessage() {
    QueuableObject message = new QueuableObject(0);

    queue.send(message);

    QueuableObject resultOne = queue.get();

    Date expectedEarliestGet = new Date();
    double expectedPriority = 0.8;
    Date timeBeforeRequeue = new Date();
    queue.requeue(resultOne, expectedEarliestGet, expectedPriority);

    assertThat(datastore.getCount(QueuableObject.class)).isEqualTo(1);

    QueuableObject actual = datastore.find(QueuableObject.class).get();

    Date actualCreated = actual.getCreated();
    assertThat(actualCreated).isAfterOrEqualsTo(timeBeforeRequeue).isBeforeOrEqualsTo(new Date());

    QueuableObject expected = new QueuableObject(0);
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
        .isThrownBy(() -> queue.requeue(new QueuableObject(1), new Date(), Double.NaN));
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
    assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> queue.requeue(new QueuableObject(1), null));
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
    QueuableObject message = new QueuableObject(1);

    Date expectedEarliestGet = new Date();
    double expectedPriority = 0.8;
    Date timeBeforeSend = new Date();
    message.setPriority(expectedPriority);
    message.setEarliestGet(expectedEarliestGet);
    queue.send(message);

    assertThat(datastore.getCount(QueuableObject.class)).isEqualTo(1);

    QueuableObject actual = datastore.find(QueuableObject.class).get();

    Date actualCreated = actual.getCreated();
    assertThat(actualCreated).isAfterOrEqualsTo(timeBeforeSend).isBeforeOrEqualsTo(new Date());

    QueuableObject expected = new QueuableObject(1);
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
    entityQueue = new MongoQueueImpl<>(TestQueuableWithEntity.class, datastore);

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

    /**
     * Instantiates a new Test entity.
     */
    TestEntity() {}

    /**
     * Instantiates a new Test entity.
     *
     * @param data the data
     */
    TestEntity(int data) {
      this.data = data;
    }

    /**
     * Gets id.
     *
     * @return the id
     */
    public String getId() {
      return id;
    }

    /**
     * Sets id.
     *
     * @param id the id
     */
    public void setId(String id) {
      this.id = id;
    }

    /**
     * Gets data.
     *
     * @return the data
     */
    public int getData() {
      return data;
    }

    /**
     * Sets data.
     *
     * @param data the data
     */
    public void setData(int data) {
      this.data = data;
    }

    @Override
    public int hashCode() {
      return Objects.hash(id, data);
    }

    /**
     * On update.
     */
    @PrePersist
    public void onUpdate() {
      if (id == null) {
        id = generateUuid();
      }
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null || getClass() != obj.getClass()) {
        return false;
      }
      TestEntity that = (TestEntity) obj;
      return data == that.data && Objects.equals(id, that.id);
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this).add("id", id).add("data", data).toString();
    }
  }

  /**
   * The Class TestQueuableWithEntity.
   */
  @Entity(value = "!!!testEntityQueue", noClassnameStored = true)
  public static class TestQueuableWithEntity extends Queuable {
    @Reference private TestEntity entity;

    /**
     * Instantiates a new test queuable with entity.
     */
    public TestQueuableWithEntity() {}

    /**
     * Instantiates a new test queuable with entity.
     *
     * @param other the other
     */
    public TestQueuableWithEntity(TestQueuableWithEntity other) {
      super(other);
      this.entity = other.entity;
    }

    /**
     * Instantiates a new test queuable with entity.
     *
     * @param entity the entity
     */
    public TestQueuableWithEntity(TestEntity entity) {
      this.entity = entity;
    }

    /**
     * Gets entity.
     *
     * @return the entity
     */
    public TestEntity getEntity() {
      return entity;
    }

    /**
     * Sets entity.
     *
     * @param entity the entity
     */
    public void setEntity(TestEntity entity) {
      this.entity = entity;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null || getClass() != obj.getClass()) {
        return false;
      }
      TestQueuableWithEntity that = (TestQueuableWithEntity) obj;
      return Objects.equals(entity, that.entity);
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
      return Objects.hash(entity);
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this).add("entity", entity).toString();
    }
  }
}
