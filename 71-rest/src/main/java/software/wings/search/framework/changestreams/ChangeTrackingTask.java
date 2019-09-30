package software.wings.search.framework.changestreams;

import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoInterruptedException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.changestream.ChangeStreamDocument;
import com.mongodb.client.model.changestream.FullDocument;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.bson.BsonDocument;
import org.bson.Document;

import java.util.concurrent.CountDownLatch;

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
  private MongoCollection<DBObject> collection;
  private ChangeStreamSubscriber changeStreamSubscriber;
  private BsonDocument resumeToken;
  private CountDownLatch latch;

  ChangeTrackingTask(ChangeStreamSubscriber changeStreamSubscriber, MongoCollection<DBObject> collection,
      String tokenParam, CountDownLatch latch) {
    this.changeStreamSubscriber = changeStreamSubscriber;
    this.collection = collection;
    if (tokenParam != null) {
      this.resumeToken =
          Document.parse(tokenParam).toBsonDocument(BsonDocument.class, MongoClient.getDefaultCodecRegistry());
    } else {
      this.resumeToken = null;
    }
    this.latch = latch;
  }

  private void handleChange(final ChangeStreamDocument<DBObject> changeStreamDocument) {
    changeStreamSubscriber.onChange(changeStreamDocument);
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

  public void run() {
    MongoCursor<ChangeStreamDocument<DBObject>> cursor = null;
    try {
      cursor = openChangeStream();
      logger.info(String.format("changeStream opened on %s", collection.getNamespace()));
      while (!Thread.interrupted()) {
        handleChange(cursor.next());
      }
    } catch (MongoInterruptedException e) {
      logger.warn(String.format("Changestream on %s interrupted", collection.getNamespace()), e);
      Thread.currentThread().interrupt();
    } catch (RuntimeException e) {
      logger.error(String.format("Unexpectedly %s changeStream shutting down", collection.getNamespace()), e);
    } finally {
      logger.warn(String.format("%s changeStream shutting down.", collection.getNamespace()));
      closeChangeStream(cursor);
      latch.countDown();
    }
  }
}
