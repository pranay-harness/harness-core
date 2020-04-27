package software.wings.search.framework.changestreams;

import static software.wings.dl.exportimport.WingsMongoExportImport.getCollectionName;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;

import com.mongodb.ClientSessionOptions;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoClientURI;
import com.mongodb.ReadConcern;
import com.mongodb.ReadPreference;
import com.mongodb.Tag;
import com.mongodb.TagSet;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.changestream.ChangeStreamDocument;
import com.mongodb.client.model.changestream.OperationType;
import io.harness.mongo.MongoModule;
import io.harness.persistence.PersistentEntity;
import lombok.extern.slf4j.Slf4j;
import software.wings.app.MainConfiguration;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
  @Inject private ChangeEventFactory changeEventFactory;
  private ExecutorService executorService;
  private Set<ChangeTrackingTask> changeTrackingTasks;
  private Set<Future<?>> changeTrackingTasksFuture;
  private MongoDatabase mongoDatabase;
  private MongoClient mongoClient;
  private ReadPreference readPreference;
  private ClientSession clientSession;

  private MongoClientURI mongoClientUri() {
    final String mongoClientUrl = mainConfiguration.getMongoConnectionFactory().getUri();
    if (mainConfiguration.getElasticsearchConfig().getMongoTagKey().equals("none")) {
      readPreference = ReadPreference.secondaryPreferred();
    } else {
      final TagSet tags = new TagSet(new Tag(mainConfiguration.getElasticsearchConfig().getMongoTagKey(),
          mainConfiguration.getElasticsearchConfig().getMongoTagValue()));
      readPreference = ReadPreference.secondary(tags);
    }
    return new MongoClientURI(mongoClientUrl,
        MongoClientOptions.builder(MongoModule.defaultMongoClientOptions).readPreference(readPreference));
  }

  private void connectToMongoDatabase() {
    MongoClientURI uri = mongoClientUri();
    mongoClient = new MongoClient(uri);
    if (readPreference.getClass() == ReadPreference.secondaryPreferred().getClass()) {
      clientSession = null;
    } else {
      clientSession = mongoClient.startSession(ClientSessionOptions.builder().build());
    }
    final String databaseName = uri.getDatabase();
    logger.info("Database is {}", databaseName);
    mongoDatabase =
        mongoClient.getDatabase(databaseName).withReadConcern(ReadConcern.MAJORITY).withReadPreference(readPreference);
  }

  private void createChangeStreamTasks(Set<ChangeTrackingInfo<?>> changeTrackingInfos, CountDownLatch latch) {
    changeTrackingTasks = new HashSet<>();
    for (ChangeTrackingInfo<?> changeTrackingInfo : changeTrackingInfos) {
      MongoCollection<DBObject> collection =
          mongoDatabase.getCollection(getCollectionName(changeTrackingInfo.getMorphiaClass()))
              .withDocumentClass(DBObject.class)
              .withReadPreference(readPreference);
      logger.info("Connection details for mongo collection {}", collection.getReadPreference());

      ChangeStreamSubscriber changeStreamSubscriber = getChangeStreamSubscriber(changeTrackingInfo);
      ChangeTrackingTask changeTrackingTask = new ChangeTrackingTask(
          changeStreamSubscriber, collection, clientSession, latch, changeTrackingInfo.getResumeToken());
      changeTrackingTasks.add(changeTrackingTask);
    }
  }

  private void openChangeStreams(Set<ChangeTrackingInfo<?>> changeTrackingInfos) {
    executorService =
        Executors.newFixedThreadPool(8, new ThreadFactoryBuilder().setNameFormat("change-tracker-%d").build());
    CountDownLatch latch = new CountDownLatch(changeTrackingInfos.size());
    createChangeStreamTasks(changeTrackingInfos, latch);
    changeTrackingTasksFuture = new HashSet<>();

    if (!executorService.isShutdown()) {
      for (ChangeTrackingTask changeTrackingTask : changeTrackingTasks) {
        Future f = executorService.submit(changeTrackingTask);
        changeTrackingTasksFuture.add(f);
      }
    }
  }

  private boolean shouldProcessChange(ChangeStreamDocument<DBObject> changeStreamDocument) {
    return changeStreamDocument.getFullDocument() != null
        || changeStreamDocument.getOperationType() == OperationType.DELETE;
  }

  private <T extends PersistentEntity> ChangeStreamSubscriber getChangeStreamSubscriber(
      ChangeTrackingInfo<T> changeTrackingInfo) {
    return changeStreamDocument -> {
      if (shouldProcessChange(changeStreamDocument)) {
        ChangeEvent<T> changeEvent =
            changeEventFactory.fromChangeStreamDocument(changeStreamDocument, changeTrackingInfo.getMorphiaClass());
        changeTrackingInfo.getChangeSubscriber().onChange(changeEvent);
      }
    };
  }

  public void start(Set<ChangeTrackingInfo<?>> changeTrackingInfos) {
    connectToMongoDatabase();
    openChangeStreams(changeTrackingInfos);
  }

  public boolean checkIfAnyChangeTrackerIsAlive() {
    for (Future<?> f : changeTrackingTasksFuture) {
      if (!f.isDone()) {
        return true;
      }
    }
    return false;
  }

  public void stop() {
    logger.info("Trying to close changeTrackingTasks");
    for (Future<?> f : changeTrackingTasksFuture) {
      f.cancel(true);
    }
    if (executorService != null) {
      executorService.shutdownNow();
    }
    if (mongoClient != null) {
      mongoClient.close();
    }
    if (clientSession != null) {
      clientSession.close();
    }
  }
}