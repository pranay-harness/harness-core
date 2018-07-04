package software.wings.core.queue;

import static org.joor.Reflect.on;

import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.Sort;
import org.mongodb.morphia.query.UpdateOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Created by peeyushaggarwal on 4/11/16.
 *
 * @param <T> the generic type
 */
public class MongoQueueImpl<T extends Queuable> implements Queue<T> {
  private static final Logger logger = LoggerFactory.getLogger(MongoQueueImpl.class);

  private Datastore datastore;
  private Class<T> klass;
  private int resetDurationInSeconds;

  /**
   * Instantiates a new mongo queue impl.
   *
   * @param klass     the klass
   * @param datastore the datastore
   */
  public MongoQueueImpl(Class<T> klass, final Datastore datastore) {
    this(klass, datastore, 5);
  }

  /**
   * Instantiates a new mongo queue impl.
   *
   * @param klass                  the klass
   * @param datastore              the datastore
   * @param resetDurationInSeconds the reset duration in seconds
   */
  public MongoQueueImpl(Class<T> klass, final Datastore datastore, int resetDurationInSeconds) {
    Objects.requireNonNull(datastore);
    Objects.requireNonNull(klass);
    this.datastore = datastore;
    this.klass = klass;
    this.resetDurationInSeconds = resetDurationInSeconds;
  }

  /* (non-Javadoc)
   * @see software.wings.core.queue.Queue#get()
   */
  @Override
  public T get() {
    return get(3000, 1000);
  }

  /* (non-Javadoc)
   * @see software.wings.core.queue.Queue#get(int)
   */
  @Override
  public T get(final int waitDuration) {
    return get(waitDuration, 1000);
  }

  /* (non-Javadoc)
   * @see software.wings.core.queue.Queue#get(int, long)
   */
  @Override
  public T get(final int waitDuration, long pollDuration) {
    // reset stuck messages
    datastore.update(
        datastore.createQuery(klass).filter("running", true).field("resetTimestamp").lessThanOrEq(new Date()),
        datastore.createUpdateOperations(klass).set("running", false));

    Query<T> query = datastore.createQuery(klass)
                         .filter("running", false)
                         .field("earliestGet")
                         .lessThanOrEq(new Date())
                         .order(Sort.descending("priority"), Sort.ascending("created"));

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
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      } catch (final IllegalArgumentException ex) {
        pollDuration = 0;
      }
    }
  }

  /* (non-Javadoc)
   * @see software.wings.core.queue.Queue#updateResetDuration(java.lang.Object)
   */
  @Override
  public void updateResetDuration(T message) {
    Objects.requireNonNull(message);

    Query<T> query = datastore.createQuery(klass)
                         .filter("_id", message.getId())
                         .field("resetTimestamp")
                         .greaterThan(new Date())
                         .filter("running", true);

    Date resetTimestamp = new Date(System.currentTimeMillis() + resetDurationMillis());

    UpdateOperations<T> updateOperations =
        datastore.createUpdateOperations(klass).set("resetTimestamp", resetTimestamp);

    if (datastore.findAndModify(query, updateOperations) != null) {
      message.setResetTimestamp(resetTimestamp);
    } else {
      logger.error("Reset duration failed");
    }
  }

  /* (non-Javadoc)
   * @see software.wings.core.queue.Queue#count()
   */
  @Override
  public long count() {
    return datastore.getCount(klass);
  }

  /* (non-Javadoc)
   * @see software.wings.core.queue.Queue#count(boolean)
   */
  @Override
  public long count(final boolean running) {
    return datastore.getCount(datastore.createQuery(klass).filter("running", running));
  }

  /* (non-Javadoc)
   * @see software.wings.core.queue.Queue#ack(java.lang.Object)
   */
  @Override
  public void ack(final T message) {
    Objects.requireNonNull(message);
    String id = message.getId();
    datastore.delete(klass, id);
  }

  /* (non-Javadoc)
   * @see software.wings.core.queue.Queue#ackSend(java.lang.Object, java.lang.Object)
   */
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

  /* (non-Javadoc)
   * @see software.wings.core.queue.Queue#requeue(java.lang.Object)
   */
  @Override
  public void requeue(final T message) {
    requeue(message, new Date());
  }

  /* (non-Javadoc)
   * @see software.wings.core.queue.Queue#requeue(java.lang.Object, java.util.Date)
   */
  @Override
  public void requeue(final T message, final Date earliestGet) {
    requeue(message, earliestGet, 0.0);
  }

  /* (non-Javadoc)
   * @see software.wings.core.queue.Queue#requeue(java.lang.Object, java.util.Date, double)
   */
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

  /* (non-Javadoc)
   * @see software.wings.core.queue.Queue#send(java.lang.Object)
   */
  @Override
  public void send(final T payload) {
    Objects.requireNonNull(payload);
    datastore.save(payload);
  }

  /* (non-Javadoc)
   * @see software.wings.core.queue.Queue#resetDurationMillis()
   */
  @Override
  public long resetDurationMillis() {
    return TimeUnit.SECONDS.toMillis(resetDurationInSeconds);
  }

  /* (non-Javadoc)
   * @see software.wings.core.queue.Queue#name()
   */
  @Override
  public String name() {
    return datastore.getCollection(klass).getName();
  }

  /**
   * Reset duration.
   *
   * @param resetDurationInSeconds the reset duration in seconds
   */
  // package protected
  void resetDuration(int resetDurationInSeconds) {
    this.resetDurationInSeconds = resetDurationInSeconds;
  }
}
