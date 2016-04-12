package software.wings.core.queue;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import org.bson.types.ObjectId;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.mapping.validation.ClassConstraint;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

import java.lang.reflect.Constructor;
import java.util.*;

/**
 * Created by peeyushaggarwal on 4/11/16.
 */
public class MongoQueueImpl<T extends Queuable> implements Queue<T> {
  private Datastore datastore;
  private Class<T> klass;

  public MongoQueueImpl(Class<T> klass, final Datastore datastore) {
    Objects.requireNonNull(datastore);
    Objects.requireNonNull(klass);
    this.datastore = datastore;
    this.klass = klass;
    datastore.ensureIndexes(klass);
  }

  @Override
  public T get(final int resetDuration) {
    return get(resetDuration, 3000, 200);
  }

  @Override
  public T get(final int resetDuration, final int waitDuration) {
    return get(resetDuration, waitDuration, 200);
  }

  @Override
  public T get(final int resetDuration, final int waitDuration, long pollDuration) {
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

    final Calendar calendar = Calendar.getInstance();

    calendar.add(Calendar.SECOND, resetDuration);
    final Date resetTimestamp = calendar.getTime();

    UpdateOperations<T> updateOperations =
        datastore.createUpdateOperations(klass).set("running", true).set("resetTimestamp", resetTimestamp);

    final BasicDBObject sort = new BasicDBObject("priority", 1).append("created", 1);
    final BasicDBObject update =
        new BasicDBObject("$set", new BasicDBObject("running", true).append("resetTimestamp", resetTimestamp));
    final BasicDBObject fields = new BasicDBObject("payload", 1);

    calendar.setTimeInMillis(System.currentTimeMillis());
    calendar.add(Calendar.MILLISECOND, waitDuration);
    final Date end = calendar.getTime();

    while (true) {
      T message = datastore.findAndModify(query, updateOperations);
      if (message != null) {
        return message;
      }

      if (new Date().compareTo(end) >= 0) {
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
    final String id = message.getId();
    datastore.delete(klass, id);
  }

  @Override
  public void ackSend(final T message, final T payload) {
    Objects.requireNonNull(message);
    Objects.requireNonNull(payload);

    final String id = message.getId();

    payload.setId(id);
    payload.setRunning(false);
    payload.setResetTimestamp(new Date(Long.MAX_VALUE));
    payload.setCreated(new Date());

    datastore.save(payload);
  }

  @Override
  public void requeue(final T message) throws Exception {
    requeue(message, new Date());
  }

  @Override
  public void requeue(final T message, final Date earliestGet) throws Exception {
    requeue(message, earliestGet, 0.0);
  }

  @Override
  public void requeue(final T message, final Date earliestGet, final double priority) throws Exception {
    Objects.requireNonNull(message);
    Objects.requireNonNull(earliestGet);
    if (Double.isNaN(priority)) {
      throw new IllegalArgumentException("priority was NaN");
    }

    final String id = message.getId();

    T forRequeue = createCopy(message);
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

  public T createCopy(T item) throws Exception {
    Constructor<T> copyConstructor = klass.getConstructor(klass);
    T copy = copyConstructor.newInstance(item);
    return copy;
  }
}
