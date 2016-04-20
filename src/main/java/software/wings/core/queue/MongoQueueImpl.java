package software.wings.core.queue;

import static org.joor.Reflect.on;

import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

import java.util.Date;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Created by peeyushaggarwal on 4/11/16.
 */
public class MongoQueueImpl<T extends Queuable> implements Queue<T> {
  private Datastore datastore;
  private Class<T> klass;
  private int resetDurationInSeconds;

  public MongoQueueImpl(Class<T> klass, final Datastore datastore) {
    this(klass, datastore, 5);
  }

  public MongoQueueImpl(Class<T> klass, final Datastore datastore, int resetDurationInSeconds) {
    Objects.requireNonNull(datastore);
    Objects.requireNonNull(klass);
    this.datastore = datastore;
    this.klass = klass;
    this.resetDurationInSeconds = resetDurationInSeconds;
    datastore.ensureIndexes(klass);
  }

  @Override
  public T get() {
    return get(3000, 1000);
  }

  @Override
  public T get(final int waitDuration) {
    return get(waitDuration, 1000);
  }

  @Override
  public T get(final int waitDuration, long pollDuration) {
    // reset stuck messages
    datastore.update(
        datastore.createQuery(klass).field("running").equal(true).field("resetTimestamp").lessThanOrEq(new Date()),
        datastore.createUpdateOperations(klass).set("running", false));

    Query<T> query = datastore.createQuery(klass)
                         .field("running")
                         .equal(false)
                         .field("earliestGet")
                         .lessThanOrEq(new Date())
                         .order("priority")
                         .order("created");

    Date resetTimestamp = new Date(System.currentTimeMillis() + resetDurationMillis());

    UpdateOperations<T> updateOperations =
        datastore.createUpdateOperations(klass).set("running", true).set("resetTimestamp", resetTimestamp);

    long endTime = System.currentTimeMillis() + waitDuration;

    while (true) {
      T message = datastore.findAndModify(query, updateOperations);
      if (message != null) {
        return message;
      }

      if (System.currentTimeMillis() >= endTime) {
        return null;
      }

      try {
        Thread.sleep(pollDuration);
      } catch (final InterruptedException ex) {
        throw new RuntimeException(ex);
      } catch (final IllegalArgumentException ex) {
        pollDuration = 0;
      }
    }
  }

  @Override
  public void updateResetDuration(T message) {
    Objects.requireNonNull(message);

    Query<T> query = datastore.createQuery(klass)
                         .field("_id")
                         .equal(message.getId())
                         .field("resetTimestamp")
                         .greaterThan(new Date())
                         .field("running")
                         .equal(true);

    Date resetTimestamp = new Date(System.currentTimeMillis() + resetDurationMillis());

    UpdateOperations<T> updateOperations =
        datastore.createUpdateOperations(klass).set("resetTimestamp", resetTimestamp);

    if (datastore.findAndModify(query, updateOperations) != null) {
      message.setResetTimestamp(resetTimestamp);
    } else {
      System.out.println("Reset failed");
    }
  }

  @Override
  public long count() {
    return datastore.getCount(klass);
  }

  @Override
  public long count(final boolean running) {
    return datastore.getCount(datastore.createQuery(klass).field("running").equal(running));
  }

  @Override
  public void ack(final T message) {
    Objects.requireNonNull(message);
    String id = message.getId();
    datastore.delete(klass, id);
  }

  @Override
  public void ackSend(final T message, final T payload) {
    Objects.requireNonNull(message);
    Objects.requireNonNull(payload);

    String id = message.getId();

    payload.setId(id);
    payload.setRunning(false);
    payload.setResetTimestamp(new Date(Long.MAX_VALUE));
    payload.setCreated(new Date());

    datastore.save(payload);
  }

  @Override
  public void requeue(final T message) {
    requeue(message, new Date());
  }

  @Override
  public void requeue(final T message, final Date earliestGet) {
    requeue(message, earliestGet, 0.0);
  }

  @Override
  public void requeue(final T message, final Date earliestGet, final double priority) {
    Objects.requireNonNull(message);
    Objects.requireNonNull(earliestGet);
    if (Double.isNaN(priority)) {
      throw new IllegalArgumentException("priority was NaN");
    }

    T forRequeue = on(klass).create(message).get();
    forRequeue.setId(null);
    forRequeue.setPriority(priority);
    forRequeue.setEarliestGet(earliestGet);
    ackSend(message, forRequeue);
  }

  @Override
  public void send(final T payload) {
    Objects.requireNonNull(payload);
    datastore.save(payload);
  }

  @Override
  public long resetDurationMillis() {
    return TimeUnit.SECONDS.toMillis(resetDurationInSeconds);
  }

  @Override
  public String name() {
    return datastore.getCollection(klass).getName();
  }

  // package protected
  void resetDuration(int resetDurationInSeconds) {
    this.resetDurationInSeconds = resetDurationInSeconds;
  }
}
