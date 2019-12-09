package software.wings.search.framework.changestreams;

import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoInterruptedException;
import com.mongodb.client.ChangeStreamIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.changestream.ChangeStreamDocument;
import com.mongodb.client.model.changestream.FullDocument;
import com.mongodb.session.ClientSession;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.bson.BsonDocument;
import org.bson.Document;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
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
class ChangeTrackingTask implements Runnable {
  private ChangeStreamSubscriber changeStreamSubscriber;
  private MongoCollection<DBObject> collection;
  private ClientSession clientSession;
  private CountDownLatch latch;
  private BsonDocument resumeToken;

  ChangeTrackingTask(ChangeStreamSubscriber changeStreamSubscriber, MongoCollection<DBObject> collection,
      ClientSession clientSession, CountDownLatch latch, String tokenParam) {
    this.changeStreamSubscriber = changeStreamSubscriber;
    this.collection = collection;
    this.clientSession = clientSession;
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

  private void openChangeStream(Consumer<ChangeStreamDocument<DBObject>> changeStreamDocumentConsumer) {
    ChangeStreamIterable<DBObject> changeStreamIterable =
        clientSession == null ? collection.watch() : collection.watch(clientSession);
    changeStreamIterable =
        changeStreamIterable.fullDocument(FullDocument.UPDATE_LOOKUP).maxAwaitTime(1, TimeUnit.MINUTES);
    MongoCursor<ChangeStreamDocument<DBObject>> mongoCursor = null;
    try {
      if (resumeToken == null) {
        logger.info("Opening changeStream without resumeToken");
        mongoCursor = changeStreamIterable.iterator();
      } else {
        logger.info("Opening changeStream with resumeToken");
        mongoCursor = changeStreamIterable.resumeAfter(resumeToken).iterator();
      }
      logger.info("Connection details for mongo cursor {}", mongoCursor.getServerCursor());
      mongoCursor.forEachRemaining(changeStreamDocumentConsumer);
    } finally {
      if (mongoCursor != null) {
        logger.info("Closing mongo cursor");
        mongoCursor.close();
      }
    }
  }

  public void run() {
    try {
      logger.info("changeStream opened on {}", collection.getNamespace());
      openChangeStream(this ::handleChange);
    } catch (MongoInterruptedException e) {
      Thread.currentThread().interrupt();
      logger.warn("Changestream on {} interrupted", collection.getNamespace(), e);
    } catch (RuntimeException e) {
      logger.error("Unexpectedly {} changeStream shutting down", collection.getNamespace(), e);
    } finally {
      logger.warn("{} changeStream shutting down.", collection.getNamespace());
      latch.countDown();
    }
  }
}
