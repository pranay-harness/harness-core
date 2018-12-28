package io.harness.mongo;

import static io.harness.govern.Switch.unhandled;
import static java.lang.String.format;

import com.google.inject.Inject;

import io.harness.persistence.HPersistence;
import io.harness.persistence.ReadPref;
import io.harness.queue.Queuable;
import io.harness.queue.Queue;
import io.harness.version.VersionInfoManager;
import org.joor.Reflect;
import org.mongodb.morphia.AdvancedDatastore;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.Sort;
import org.mongodb.morphia.query.UpdateOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class MongoQueue<T extends Queuable> implements Queue<T> {
  private static final Logger logger = LoggerFactory.getLogger(MongoQueue.class);

  private final Class<T> klass;
  private int resetDurationInSeconds;
  private final boolean filterWithVersion;

  @Inject private HPersistence persistence;
  @Inject private VersionInfoManager versionInfoManager;

  /**
   * Instantiates a new mongo queue impl.
   *
   * @param klass     the klass
   * @param datastore the datastore
   */
  public MongoQueue(Class<T> klass) {
    this(klass, 5);
  }

  /**
   * Instantiates a new mongo queue impl.
   *
   * @param klass                  the klass
   * @param resetDurationInSeconds the reset duration in seconds
   */
  public MongoQueue(Class<T> klass, int resetDurationInSeconds) {
    this(klass, resetDurationInSeconds, false);
  }

  /**
   * Instantiates a new mongo queue impl.
   *
   * @param klass                  the klass
   * @param resetDurationInSeconds the reset duration in seconds
   * @param filterWithVersion      the filterWithVersion
   */
  public MongoQueue(Class<T> klass, int resetDurationInSeconds, boolean filterWithVersion) {
    Objects.requireNonNull(klass);
    this.klass = klass;
    this.resetDurationInSeconds = resetDurationInSeconds;
    this.filterWithVersion = filterWithVersion;
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
    final AdvancedDatastore datastore = persistence.getDatastore(klass, ReadPref.CRITICAL);

    // reset stuck messages
    datastore.update(createQuery().filter("running", true).field("resetTimestamp").lessThanOrEq(new Date()),
        datastore.createUpdateOperations(klass).set("running", false));

    Query<T> query = createQuery()
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
    final AdvancedDatastore datastore = persistence.getDatastore(klass, ReadPref.CRITICAL);

    Objects.requireNonNull(message);

    Query<T> query = createQuery()
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
   * @see software.wings.core.queue.Queue#count(boolean)
   */
  @Override
  public long count(final Filter filter) {
    final AdvancedDatastore datastore = persistence.getDatastore(klass, ReadPref.CRITICAL);

    switch (filter) {
      case ALL:
        return datastore.getCount(klass);
      case RUNNING:
        return datastore.getCount(createQuery().filter("running", true));
      case NOT_RUNNING:
        return datastore.getCount(createQuery().filter("running", false));
      default:
        unhandled(filter);
    }
    throw new RuntimeException(format("Unknown filter type %s", filter));
  }

  /* (non-Javadoc)
   * @see software.wings.core.queue.Queue#ack(java.lang.Object)
   */
  @Override
  public void ack(final T message) {
    Objects.requireNonNull(message);
    String id = message.getId();

    persistence.getDatastore(klass, ReadPref.CRITICAL).delete(klass, id);
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

    persistence.getDatastore(klass, ReadPref.CRITICAL).save(payload);
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

    T forRequeue = Reflect.on(klass).create(message).get();
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
    payload.setVersion(versionInfoManager.getVersionInfo().getVersion());

    persistence.getDatastore(klass, ReadPref.CRITICAL).save(payload);
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
    final AdvancedDatastore datastore = persistence.getDatastore(klass, ReadPref.CRITICAL);
    return datastore.getCollection(klass).getName();
  }

  /**
   * Reset duration.
   *
   * @param resetDurationInSeconds the reset duration in seconds
   */
  // package protected
  public void resetDuration(int resetDurationInSeconds) {
    this.resetDurationInSeconds = resetDurationInSeconds;
  }

  private Query<T> createQuery() {
    final AdvancedDatastore datastore = persistence.getDatastore(klass, ReadPref.CRITICAL);
    return filterWithVersion
        ? datastore.createQuery(klass).filter("version", versionInfoManager.getVersionInfo().getVersion())
        : datastore.createQuery(klass);
  }
}
