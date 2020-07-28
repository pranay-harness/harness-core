package io.harness.service.impl;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import io.harness.callback.MongoDatabase;
import io.harness.service.intfc.DelegateCallbackService;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;

public class MongoDelegateCallbackService implements DelegateCallbackService {
  private static final String SYNC_TASK_COLLECTION_NAME_SUFFIX = "delegateSyncTaskResponses";
  private static final String ASYNC_TASK_COLLECTION_NAME_SUFFIX = "delegateAsyncTaskResponses";
  private static final String ID_PROPERTY = "_id";
  private static final String RESPONSE_DATA_PROPERTY = "responseData";
  private static final String LAST_PROCESSING_ATTEMPT_PROPERTY = "lastProcessingAttempt";
  private final MongoClient mongoClient;
  private final com.mongodb.client.MongoDatabase database;
  private final MongoCollection<Document> syncTaskResponseCollection;
  private final MongoCollection<Document> asyncTaskResponseCollection;

  MongoDelegateCallbackService(MongoDatabase mongoDatabase) {
    String connectionString = mongoDatabase.getConnection();
    mongoClient = new MongoClient(new MongoClientURI(connectionString));
    database = mongoClient.getDatabase(connectionString.substring(connectionString.lastIndexOf('/') + 1));

    String syncTaskResponseCollectionName = StringUtils.isBlank(mongoDatabase.getCollectionNamePrefix())
        ? SYNC_TASK_COLLECTION_NAME_SUFFIX
        : mongoDatabase.getCollectionNamePrefix() + "_" + SYNC_TASK_COLLECTION_NAME_SUFFIX;
    syncTaskResponseCollection = database.getCollection(syncTaskResponseCollectionName);

    String asyncTaskResponseCollectionName = StringUtils.isBlank(mongoDatabase.getCollectionNamePrefix())
        ? ASYNC_TASK_COLLECTION_NAME_SUFFIX
        : mongoDatabase.getCollectionNamePrefix() + "_" + ASYNC_TASK_COLLECTION_NAME_SUFFIX;
    asyncTaskResponseCollection = database.getCollection(asyncTaskResponseCollectionName);
  }

  @Override
  public void publishSyncTaskResponse(String delegateTaskId, byte[] responseData) {
    Document document = new Document();
    document.put(ID_PROPERTY, delegateTaskId);
    document.put(RESPONSE_DATA_PROPERTY, responseData);
    syncTaskResponseCollection.insertOne(document);
  }

  @Override
  public void publishAsyncTaskResponse(String delegateTaskId, byte[] responseData) {
    Document document = new Document();
    document.put(ID_PROPERTY, delegateTaskId);
    document.put(RESPONSE_DATA_PROPERTY, responseData);
    document.put(LAST_PROCESSING_ATTEMPT_PROPERTY, 0L);
    asyncTaskResponseCollection.insertOne(document);
  }

  @Override
  public void destroy() {
    mongoClient.close();
  }
}
