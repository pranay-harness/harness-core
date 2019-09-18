package software.wings.search.framework.changestreams;

import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoInterruptedException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.changestream.ChangeStreamDocument;
import com.mongodb.client.model.changestream.FullDocument;
import io.harness.persistence.PersistentEntity;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.bson.BsonDocument;
import org.bson.Document;

import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

/**
 * The change tracking task collection class which handles
 * the resumeToken for each changeStream collection.
 * It also opens the changeStream and manages it lifecycle.
 *
 * @author utkarsh
 */

@Value
@Slf4j
public class ChangeTrackingTask implements Runnable {
  private Class<? extends PersistentEntity> morphiaClass;
  private MongoCollection<DBObject> collection;
  private Consumer<ChangeEvent> changeEventConsumer;
  private BsonDocument resumeToken;
  private CountDownLatch latch;

  public ChangeTrackingTask(Class<? extends PersistentEntity> morphiaClass, Consumer<ChangeEvent> changeEventConsumer,
      MongoCollection<DBObject> collection, String tokenParam, CountDownLatch latch) {
    this.morphiaClass = morphiaClass;
    this.changeEventConsumer = changeEventConsumer;
    this.collection = collection;
    if (tokenParam != null) {
      this.resumeToken =
          Document.parse(tokenParam).toBsonDocument(BsonDocument.class, MongoClient.getDefaultCodecRegistry());
    } else {
      this.resumeToken = null;
    }
    this.latch = latch;
  }

  public void handleChangeStreamTask(final ChangeStreamDocument<DBObject> changeStreamDocument) {
    ChangeEvent changeEvent = new ChangeEvent(changeStreamDocument, morphiaClass);
    changeEventConsumer.accept(changeEvent);
  }

  public void run() {
    MongoCursor<ChangeStreamDocument<DBObject>> cursor = null;
    try {
      cursor = openChangeStream();
      logger.info(String.format("changeStream opened on %s", morphiaClass.getCanonicalName()));
      while (!Thread.interrupted()) {
        try {
          handleChangeStreamTask(cursor.next());
        } catch (MongoInterruptedException e) {
          logger.warn(String.format("Changestream on %s interrupted", morphiaClass.getCanonicalName()), e);
          Thread.currentThread().interrupt();
        }
      }
      logger.warn(String.format("%s changeStream shutting down.", morphiaClass.getCanonicalName()));
    } catch (RuntimeException e) {
      logger.error(String.format("Unexpectedly %s changeStream shutting down", morphiaClass.getCanonicalName()), e);
    } finally {
      closeChangeStream(cursor);
      latch.countDown();
    }
  }

  private MongoCursor<ChangeStreamDocument<DBObject>> openChangeStream() {
    MongoCursor<ChangeStreamDocument<DBObject>> cursor;
    if (resumeToken == null) {
      logger.info("Opening changeStream without resumeToken");
      cursor = collection.watch().fullDocument(FullDocument.UPDATE_LOOKUP).iterator();
    } else {
      logger.info("Opening changeStream with resumeToken");
      cursor = collection.watch().resumeAfter(resumeToken).fullDocument(FullDocument.UPDATE_LOOKUP).iterator();
    }
    return cursor;
  }

  private void closeChangeStream(MongoCursor<ChangeStreamDocument<DBObject>> cursor) {
    if (cursor != null) {
      cursor.close();
    }
  }
}
