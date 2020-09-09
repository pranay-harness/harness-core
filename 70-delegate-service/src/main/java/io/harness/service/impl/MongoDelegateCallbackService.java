package io.harness.service.impl;

import static com.mongodb.DBCollection.ID_FIELD_NAME;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import io.harness.callback.MongoDatabase;
import io.harness.delegate.beans.DelegateAsyncTaskResponse.DelegateAsyncTaskResponseKeys;
import io.harness.delegate.beans.DelegateSyncTaskResponse.DelegateSyncTaskResponseKeys;
import io.harness.service.intfc.DelegateCallbackService;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.bson.conversions.Bson;

public class MongoDelegateCallbackService implements DelegateCallbackService {
  private static final String SYNC_TASK_COLLECTION_NAME_SUFFIX = "delegateSyncTaskResponses";
  private static final String ASYNC_TASK_COLLECTION_NAME_SUFFIX = "delegateAsyncTaskResponses";
  private static final String PARKED_TASK_COLLECTION_NAME_SUFFIX = "delegateParkedTaskResponses";
  private final MongoClient mongoClient;
  private final com.mongodb.client.MongoDatabase database;
  private final MongoCollection<Document> syncTaskResponseCollection;
  private final MongoCollection<Document> asyncTaskResponseCollection;
  private final MongoCollection<Document> parkedTaskResponseCollection;

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

    String parkedTaskResponseCollectionName = StringUtils.isBlank(mongoDatabase.getCollectionNamePrefix())
        ? PARKED_TASK_COLLECTION_NAME_SUFFIX
        : mongoDatabase.getCollectionNamePrefix() + "_" + PARKED_TASK_COLLECTION_NAME_SUFFIX;
    parkedTaskResponseCollection = database.getCollection(parkedTaskResponseCollectionName);
  }

  @Override
  public void publishSyncTaskResponse(String delegateTaskId, byte[] responseData) {
    Document document = new Document();
    document.put(ID_FIELD_NAME, delegateTaskId);
    document.put(DelegateSyncTaskResponseKeys.responseData, responseData);
    syncTaskResponseCollection.insertOne(document);
  }

  private static final UpdateOptions upsert = new UpdateOptions().upsert(true);

  @Override
  public void publishAsyncTaskResponse(String delegateTaskId, byte[] responseData) {
    Bson filter = Filters.eq(ID_FIELD_NAME, delegateTaskId);

    Document document = new Document();
    document.put(ID_FIELD_NAME, delegateTaskId);
    document.put(DelegateAsyncTaskResponseKeys.responseData, responseData);
    document.put(DelegateAsyncTaskResponseKeys.processAfter, 0);

    Bson update = new Document("$set", document);

    asyncTaskResponseCollection.updateOne(filter, update, upsert);
  }

  @Override
  public void publishParkedTaskResponse(String delegateTaskId, byte[] responseData) {
    Bson filter = Filters.eq(ID_FIELD_NAME, delegateTaskId);

    Document document = new Document();
    document.put(ID_FIELD_NAME, delegateTaskId);
    document.put(DelegateAsyncTaskResponseKeys.responseData, responseData);
    document.put(DelegateAsyncTaskResponseKeys.processAfter, 0);

    Bson update = new Document("$set", document);

    parkedTaskResponseCollection.updateOne(filter, update, upsert);
  }

  @Override
  public void destroy() {
    mongoClient.close();
  }
}
