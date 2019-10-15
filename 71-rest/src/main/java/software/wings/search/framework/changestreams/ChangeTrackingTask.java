package software.wings.search.framework.changestreams;

import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoInterruptedException;
import com.mongodb.client.ChangeStreamIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.changestream.ChangeStreamDocument;
import com.mongodb.client.model.changestream.FullDocument;
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

  private void openChangeStream(Consumer<ChangeStreamDocument<DBObject>> changeStreamDocumentConsumer) {
    ChangeStreamIterable<DBObject> changeStreamIterable =
        collection.watch().fullDocument(FullDocument.UPDATE_LOOKUP).maxAwaitTime(1, TimeUnit.MINUTES);
    if (resumeToken == null) {
      logger.info("Opening changeStream without resumeToken");
      changeStreamIterable.forEach(changeStreamDocumentConsumer);
    } else {
      logger.info("Opening changeStream with resumeToken");
      changeStreamIterable.resumeAfter(resumeToken).forEach(changeStreamDocumentConsumer);
    }
  }

  public void run() {
    try {
      logger.info(String.format("changeStream opened on %s", collection.getNamespace()));
      openChangeStream(this ::handleChange);
    } catch (MongoInterruptedException e) {
      logger.warn(String.format("Changestream on %s interrupted", collection.getNamespace()), e);
      Thread.currentThread().interrupt();
    } catch (RuntimeException e) {
      logger.error(String.format("Unexpectedly %s changeStream shutting down", collection.getNamespace()), e);
    } finally {
      logger.warn(String.format("%s changeStream shutting down.", collection.getNamespace()));
      latch.countDown();
    }
  }
}
