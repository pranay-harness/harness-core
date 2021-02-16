package io.harness.changestreamsframework;

import io.harness.ChangeDataCaptureServiceConfig;
import io.harness.mongo.MongoModule;
import io.harness.persistence.PersistentEntity;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoClientURI;
import com.mongodb.ReadConcern;
import com.mongodb.ReadPreference;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.changestream.ChangeStreamDocument;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.annotations.Entity;

@Slf4j
public class ChangeTracker {
  @Inject private ChangeDataCaptureServiceConfig mainConfiguration;
  @Inject private ChangeEventFactory changeEventFactory;
  private ExecutorService executorService;
  private Set<ChangeTrackingTask> changeTrackingTasks;
  private Set<Future<?>> changeTrackingTasksFuture;
  private MongoClient mongoClient;
  private ReadPreference readPreference;

  private String getCollectionName(Class<? extends PersistentEntity> clazz) {
    return clazz.getAnnotation(Entity.class).value();
  }

  private String getChangeDataCaptureDataStore(Class<? extends PersistentEntity> clazz) {
    return clazz.getAnnotation(ChangeDataCapture.class).dataStore();
  }

  private MongoClientURI mongoClientUri(String dataStore) {
    String mongoClientUrl;
    switch (dataStore) {
      case "events":
        mongoClientUrl = mainConfiguration.getEventsMongo().getUri();
        break;
      default:
        mongoClientUrl = mainConfiguration.getHarnessMongo().getUri();
        break;
    }
    readPreference = ReadPreference.secondaryPreferred();
    return new MongoClientURI(mongoClientUrl,
        MongoClientOptions.builder(MongoModule.defaultMongoClientOptions).readPreference(readPreference));
  }

  private MongoDatabase connectToMongoDatabase(String dataStore) {
    MongoClientURI uri = mongoClientUri(dataStore);
    mongoClient = new MongoClient(uri);
    final String databaseName = uri.getDatabase();
    log.info("Database is {}", databaseName);
    return mongoClient.getDatabase(databaseName)
        .withReadConcern(ReadConcern.MAJORITY)
        .withReadPreference(readPreference);
  }

  private void createChangeStreamTasks(Set<ChangeTrackingInfo<?>> changeTrackingInfos) {
    changeTrackingTasks = new HashSet<>();
    for (ChangeTrackingInfo<?> changeTrackingInfo : changeTrackingInfos) {
      MongoDatabase mongoDatabase =
          connectToMongoDatabase(getChangeDataCaptureDataStore(changeTrackingInfo.getMorphiaClass()));

      MongoCollection<DBObject> collection =
          mongoDatabase.getCollection(getCollectionName(changeTrackingInfo.getMorphiaClass()))
              .withDocumentClass(DBObject.class)
              .withReadPreference(readPreference);

      log.info("Connection details for mongo collection {}", collection.getReadPreference());

      ChangeStreamSubscriber changeStreamSubscriber = getChangeStreamSubscriber(changeTrackingInfo);
      ChangeTrackingTask changeTrackingTask = new ChangeTrackingTask(changeStreamSubscriber, collection);
      changeTrackingTasks.add(changeTrackingTask);
    }
  }

  private void openChangeStreams(Set<ChangeTrackingInfo<?>> changeTrackingInfos) {
    executorService =
        Executors.newFixedThreadPool(8, new ThreadFactoryBuilder().setNameFormat("change-tracker-%d").build());
    createChangeStreamTasks(changeTrackingInfos);
    changeTrackingTasksFuture = new HashSet<>();

    if (!executorService.isShutdown()) {
      for (ChangeTrackingTask changeTrackingTask : changeTrackingTasks) {
        Future f = executorService.submit(changeTrackingTask);
        changeTrackingTasksFuture.add(f);
      }
    }
  }

  private boolean shouldProcessChange(ChangeStreamDocument<DBObject> changeStreamDocument) {
    return changeStreamDocument.getFullDocument() != null;
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
    log.info("Trying to close changeTrackingTasks");
    for (Future<?> f : changeTrackingTasksFuture) {
      f.cancel(true);
    }
    if (executorService != null) {
      executorService.shutdownNow();
    }
    if (mongoClient != null) {
      mongoClient.close();
    }
  }
}
