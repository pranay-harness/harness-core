
package io.harness.changestreamsframework;

import com.mongodb.DBObject;
import com.mongodb.MongoInterruptedException;
import com.mongodb.client.ChangeStreamIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.changestream.ChangeStreamDocument;
import com.mongodb.client.model.changestream.FullDocument;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

@Value
@Slf4j
class ChangeTrackingTask implements Runnable {
  private ChangeStreamSubscriber changeStreamSubscriber;
  private MongoCollection<DBObject> collection;

  ChangeTrackingTask(ChangeStreamSubscriber changeStreamSubscriber, MongoCollection<DBObject> collection) {
    this.changeStreamSubscriber = changeStreamSubscriber;
    this.collection = collection;
  }

  private void handleChange(final ChangeStreamDocument<DBObject> changeStreamDocument) {
    changeStreamSubscriber.onChange(changeStreamDocument);
  }

  private void openChangeStream(Consumer<ChangeStreamDocument<DBObject>> changeStreamDocumentConsumer) {
    ChangeStreamIterable<DBObject> changeStreamIterable = collection.watch();
    changeStreamIterable =
        changeStreamIterable.fullDocument(FullDocument.UPDATE_LOOKUP).maxAwaitTime(1, TimeUnit.MINUTES);

    MongoCursor<ChangeStreamDocument<DBObject>> mongoCursor = null;
    try {
      mongoCursor = changeStreamIterable.iterator();
      log.info("Connection details for mongo cursor {}", mongoCursor.getServerCursor());
      mongoCursor.forEachRemaining(changeStreamDocumentConsumer);
    } finally {
      if (mongoCursor != null) {
        log.info("Closing mongo cursor");
        mongoCursor.close();
      }
    }
  }

  @Override
  public void run() {
    try {
      log.info("changeStream opened on {}", collection.getNamespace());
      openChangeStream(this::handleChange);
    } catch (MongoInterruptedException e) {
      Thread.currentThread().interrupt();
      log.warn("Changestream on {} interrupted", collection.getNamespace(), e);
    } catch (RuntimeException e) {
      log.error("Unexpectedly {} changeStream shutting down", collection.getNamespace(), e);
    }
  }
}
