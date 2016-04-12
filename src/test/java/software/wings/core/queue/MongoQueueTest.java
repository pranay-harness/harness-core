package software.wings.core.queue;

import com.google.common.base.MoreObjects;
import com.google.inject.name.Named;
import org.junit.Before;
import org.junit.Test;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.PrePersist;
import org.mongodb.morphia.annotations.Reference;
import org.omg.CORBA.INTERNAL;
import software.wings.WingsBaseTest;
import software.wings.common.UUIDGenerator;

import javax.inject.Inject;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Created by peeyushaggarwal on 4/11/16.
 */
public class MongoQueueTest extends WingsBaseTest {
  @Inject @Named("primaryDatastore") private Datastore datastore;

  @Entity(value = "testQueue", noClassnameStored = true)
  public static class TestQueuable extends Queuable {
    private int data;

    public TestQueuable() {
      super();
    }

    public TestQueuable(int data) {
      super();
      this.data = data;
    }
    public TestQueuable(TestQueuable other) {
      super(other);
    }

    public int getData() {
      return data;
    }

    public void setData(int data) {
      this.data = data;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o)
        return true;
      if (o == null || getClass() != o.getClass())
        return false;
      TestQueuable that = (TestQueuable) o;
      return data == that.data;
    }

    @Override
    public int hashCode() {
      return Objects.hash(data);
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this).add("data", data).toString();
    }
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

    public int getData() {
      return data;
    }

    public void setData(int data) {
      this.data = data;
    }

    public void setId(String id) {
      this.id = id;
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
    public int hashCode() {
      return Objects.hash(id, data);
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this).add("id", id).add("data", data).toString();
    }

    @PrePersist
    public void onUpdate() {
      if (id == null) {
        id = UUIDGenerator.getUUID();
      }
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

  private Queue<TestQueuable> queue;

  @Before
  public void setup() throws UnknownHostException {
    queue = new MongoQueueImpl<TestQueuable>(TestQueuable.class, datastore);
  }

  @Test
  public void shouldGetWithNegativeWait() {
    assertThat(queue.get(Integer.MAX_VALUE, Integer.MIN_VALUE)).isNull();

    queue.send(new TestQueuable(1));

    assertThat(queue.get(Integer.MAX_VALUE, Integer.MIN_VALUE)).isNotNull();
  }

  @Test
  public void shouldGetWhenNegativePoll() {
    assertThat(queue.get(Integer.MAX_VALUE, 100, Long.MIN_VALUE)).isNull();

    queue.send(new TestQueuable(1));

    assertThat(queue.get(Integer.MAX_VALUE, 100, Long.MIN_VALUE)).isNotNull();
  }

  @Test
  public void shouldNotGetMessageOnceAcquired() {
    queue.send(new TestQueuable(1));

    assertThat(queue.get(Integer.MAX_VALUE)).isNotNull();

    // try get message we already have before ack
    assertThat(queue.get(Integer.MAX_VALUE, 0)).isNull();
  }

  @Test
  public void shouldReturnMessageBasedONPriority() {
    final TestQueuable messageOne = new TestQueuable(1);
    messageOne.setPriority(0.5);
    final TestQueuable messageTwo = new TestQueuable(2);
    messageTwo.setPriority(0.4);
    final TestQueuable messageThree = new TestQueuable(3);
    messageThree.setPriority(0.3);

    queue.send(messageOne);
    queue.send(messageTwo);
    queue.send(messageThree);

    assertThat(queue.get(Integer.MAX_VALUE)).isEqualTo(messageOne);
    assertThat(queue.get(Integer.MAX_VALUE)).isEqualTo(messageTwo);
    assertThat(queue.get(Integer.MAX_VALUE)).isEqualTo(messageThree);
  }

  @Test
  public void shouldReturnMessageInTimeOrder() {
    final TestQueuable messageOne = new TestQueuable(1);
    final TestQueuable messageTwo = new TestQueuable(2);
    final TestQueuable messageThree = new TestQueuable(3);

    queue.send(messageOne);
    queue.send(messageTwo);
    queue.send(messageThree);

    assertThat(queue.get(Integer.MAX_VALUE)).isEqualTo(messageOne);
    assertThat(queue.get(Integer.MAX_VALUE)).isEqualTo(messageTwo);
    assertThat(queue.get(Integer.MAX_VALUE)).isEqualTo(messageThree);
  }

  @Test
  public void shouldWaitForSpecifiedTimePeriodForGetWhenNoMessages() {
    final Date start = new Date();
    queue.get(Integer.MAX_VALUE, 200);
    final long elapsed = new Date().getTime() - start.getTime();

    assertThat(elapsed).isBetween(200L, 400L);
  }

  @Test
  public void shouldGetMessageWhenAvailableWithinWaitPeriod() {
    final Date start = new Date();

    queue.send(new TestQueuable(1));

    queue.get(Integer.MAX_VALUE, 3000);

    assertThat(new Date().getTime() - start.getTime()).isLessThan(2000);
  }

  @Test
  public void shouldNotGetMessageBeforeEarliestGet() throws InterruptedException {
    TestQueuable message = new TestQueuable(1);
    message.setEarliestGet(new Date(System.currentTimeMillis() + 200));
    queue.send(message);

    assertThat(queue.get(Integer.MAX_VALUE, 0)).isNull();

    Thread.sleep(200);

    assertThat(queue.get(Integer.MAX_VALUE)).isNotNull();
  }

  @Test
  public void shouldResetStuckMessageWhenResetDurationHasExpired() {
    queue.send(new TestQueuable(1));

    // sets resetTimestamp on messageOne
    assertThat(queue.get(0)).isNotNull();

    assertThat(queue.get(Integer.MAX_VALUE)).isNotNull();
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

    queue.get(Integer.MAX_VALUE);

    assertThat(queue.count(true)).isEqualTo(1);
    assertThat(queue.count(false)).isEqualTo(0);
    assertThat(queue.count()).isEqualTo(1);
  }

  @Test
  public void shouldAckMessage() {
    queue.send(new TestQueuable(0));
    queue.send(new TestQueuable(1));

    final TestQueuable result = queue.get(Integer.MAX_VALUE);

    assertThat(datastore.getCount(TestQueuable.class)).isEqualTo(2);

    datastore.getCollection(TestQueuable.class).find().forEach(System.out::println);
    queue.ack(result);
    assertThat(datastore.getCount(TestQueuable.class)).isEqualTo(1);
  }

  @Test
  public void shouldThrowNPEWhenAckingNullMessage() {
    assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> queue.ack(null));
  }

  @Test
  public void shouldReplaceMessageKeepingSameIdOnAckSend() {
    final TestQueuable message = new TestQueuable(0);

    queue.send(message);

    assertThat(datastore.getCount(TestQueuable.class)).isEqualTo(1);

    final TestQueuable resultOne = queue.get(Integer.MAX_VALUE);

    final Date expectedEarliestGet = new Date();
    final double expectedPriority = 0.8;
    final Date timeBeforeAckSend = new Date();
    final TestQueuable toBeSent = new TestQueuable(1);
    toBeSent.setEarliestGet(expectedEarliestGet);
    toBeSent.setPriority(expectedPriority);
    queue.ackSend(resultOne, toBeSent);

    assertThat(datastore.getCount(TestQueuable.class)).isEqualTo(1);

    final TestQueuable actual = datastore.find(TestQueuable.class).get();

    final Date actualCreated = actual.getCreated();
    assertThat(actualCreated).isAfterOrEqualsTo(timeBeforeAckSend).isBeforeOrEqualsTo(new Date());

    final TestQueuable expected = new TestQueuable(1);
    expected.setEarliestGet(expectedEarliestGet);
    expected.setPriority(expectedPriority);
    expected.setCreated(actualCreated);

    assertThat(actual).isEqualToIgnoringGivenFields(expected, "id");
  }

  @Test
  public void shouldThrowNPEWhenAckSendIsCalledWithNullMessage() {
    assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> queue.ackSend(null, new TestQueuable(1)));
  }

  @Test
  public void shouldThrowNPEWhenAckSendIsCalledWithNullReplacementMessage() {
    assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> queue.ackSend(new TestQueuable(1), null));
  }

  @Test
  public void shouldRequeueMessage() throws Exception {
    final TestQueuable message = new TestQueuable(0);

    queue.send(message);

    final TestQueuable resultOne = queue.get(Integer.MAX_VALUE);

    final Date expectedEarliestGet = new Date();
    final double expectedPriority = 0.8;
    final Date timeBeforeRequeue = new Date();
    queue.requeue(resultOne, expectedEarliestGet, expectedPriority);

    assertThat(datastore.getCount(TestQueuable.class)).isEqualTo(1);

    final TestQueuable actual = datastore.find(TestQueuable.class).get();

    final Date actualCreated = actual.getCreated();
    assertThat(actualCreated).isAfterOrEqualsTo(timeBeforeRequeue).isBeforeOrEqualsTo(new Date());

    final TestQueuable expected = new TestQueuable(0);
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
  public void shouldThrowNPEWhenRequeueNullMessage() {
    assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> queue.requeue(null));
  }

  @Test
  public void shouldThrowNPEWhenRequeuingWithNullEarliestGet() throws Exception {
    assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> queue.requeue(new TestQueuable(1), null));
  }

  @Test
  public void shouldThrowNPEWhenSendIsCalledWithNullMessage() {
    assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> queue.send(null));
  }

  @Test
  public void shouldSendMessage() {
    final TestQueuable message = new TestQueuable(1);

    final Date expectedEarliestGet = new Date();
    final double expectedPriority = 0.8;
    final Date timeBeforeSend = new Date();
    message.setPriority(expectedPriority);
    message.setEarliestGet(expectedEarliestGet);
    queue.send(message);

    assertThat(datastore.getCount(TestQueuable.class)).isEqualTo(1);

    final TestQueuable actual = datastore.find(TestQueuable.class).get();

    final Date actualCreated = actual.getCreated();
    assertThat(actualCreated).isAfterOrEqualsTo(timeBeforeSend).isBeforeOrEqualsTo(new Date());

    final TestQueuable expected = new TestQueuable(1);
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

    TestQueuableWithEntity actual = entityQueue.get(Integer.MAX_VALUE);

    assertThat(actual.getEntity()).isEqualTo(testEntity);
  }
}
