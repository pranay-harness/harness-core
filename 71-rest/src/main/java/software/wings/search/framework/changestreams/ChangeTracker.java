package software.wings.search.framework.changestreams;

import static software.wings.dl.exportimport.WingsMongoExportImport.getCollectionName;

import com.google.inject.Inject;

import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoClientURI;
import com.mongodb.ReadConcern;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import io.harness.manage.ManagedExecutorService;
import io.harness.mongo.MongoModule;
import io.harness.persistence.PersistentEntity;
import lombok.extern.slf4j.Slf4j;
import software.wings.app.MainConfiguration;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;

/**
 * Mongo Change Stream Manager, provides the functionality
 * to open change streams on multiple collections, pass a handler
 * for individual collections and close the change streams.
 *
 * @author Utkarsh
 */

@Slf4j
public class ChangeTracker {
  @Inject private MainConfiguration mainConfiguration;
  @Inject private ManagedExecutorService managedExecutorService;
  @Inject private ChangeEventFactory changeEventFactory;
  private Set<ChangeTrackingTask> changeTrackingTasks;
  private Set<Future<?>> changeTrackingTasksFuture;
  private MongoDatabase mongoDatabase;
  private MongoClient mongoClient;

  private MongoClientURI mongoClientUri() {
    final String mongoClientUrl = mainConfiguration.getMongoConnectionFactory().getUri();
    return new MongoClientURI(mongoClientUrl, MongoClientOptions.builder(MongoModule.mongoClientOptions));
  }

  private void connectToMongoDatabase() {
    MongoClientURI uri = mongoClientUri();
    mongoClient = new MongoClient(uri);
    final String databaseName = uri.getDatabase();
    logger.info(String.format("Database is %s", databaseName));
    mongoDatabase = mongoClient.getDatabase(databaseName).withReadConcern(ReadConcern.MAJORITY);
  }

  private void createChangeStreamTasks(Set<ChangeTrackingInfo<?>> changeTrackingInfos, CountDownLatch latch) {
    changeTrackingTasks = new HashSet<>();
    for (ChangeTrackingInfo<?> changeTrackingInfo : changeTrackingInfos) {
      MongoCollection<DBObject> collection =
          mongoDatabase.getCollection(getCollectionName(changeTrackingInfo.getMorphiaClass()))
              .withDocumentClass(DBObject.class);

      ChangeStreamSubscriber changeStreamSubscriber = getChangeStreamSubscriber(changeTrackingInfo);
      ChangeTrackingTask changeTrackingTask =
          new ChangeTrackingTask(changeStreamSubscriber, collection, changeTrackingInfo.getResumeToken(), latch);
      changeTrackingTasks.add(changeTrackingTask);
    }
  }

  private Future<?> openChangeStreams(Set<ChangeTrackingInfo<?>> changeTrackingInfos) {
    CountDownLatch latch = new CountDownLatch(changeTrackingInfos.size());
    createChangeStreamTasks(changeTrackingInfos, latch);
    changeTrackingTasksFuture = new HashSet<>();

    for (ChangeTrackingTask changeTrackingTask : changeTrackingTasks) {
      Future f = managedExecutorService.submit(changeTrackingTask);
      changeTrackingTasksFuture.add(f);
    }

    return managedExecutorService.submit(() -> {
      try {
        latch.await();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        logger.error("Change tracker stopped", e);
      }
    });
  }

  private <T extends PersistentEntity> ChangeStreamSubscriber getChangeStreamSubscriber(
      ChangeTrackingInfo<T> changeTrackingInfo) {
    return changeStreamDocument -> {
      ChangeEvent<T> changeEvent =
          changeEventFactory.fromChangeStreamDocument(changeStreamDocument, changeTrackingInfo.getMorphiaClass());
      changeTrackingInfo.getChangeSubscriber().onChange(changeEvent);
    };
  }

  public Future start(Set<ChangeTrackingInfo<?>> changeTrackingInfos) {
    connectToMongoDatabase();
    return openChangeStreams(changeTrackingInfos);
  }

  public void stop() {
    logger.info("Trying to close changeTrackingTasks");
    for (Future<?> f : changeTrackingTasksFuture) {
      f.cancel(true);
    }
    mongoClient.close();
  }
}