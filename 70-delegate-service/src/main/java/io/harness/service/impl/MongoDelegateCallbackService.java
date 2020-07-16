package io.harness.service.impl;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import io.harness.callback.MongoDatabase;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.serializer.JsonUtils;
import io.harness.service.intfc.DelegateCallbackService;
import org.bson.Document;

public class MongoDelegateCallbackService implements DelegateCallbackService {
  private static final String COLLECTION_NAME = "_delegate_task_response_event";
  private static final String TASK_ID_PROPERTY = "taskId";
  private final MongoClient mongoClient;
  private final com.mongodb.client.MongoDatabase database;
  private final MongoCollection<Document> collection;

  MongoDelegateCallbackService(MongoDatabase mongoDatabase) {
    String connectionString = mongoDatabase.getConnection();
    mongoClient = new MongoClient(new MongoClientURI(connectionString));
    database = mongoClient.getDatabase(connectionString.substring(connectionString.lastIndexOf('/') + 1));
    collection = database.getCollection(mongoDatabase.getCollectionNamePrefix() + COLLECTION_NAME);
  }

  @Override
  public void publishTaskResponse(String delegateTaskId, DelegateTaskResponse response) {
    Document document = Document.parse(JsonUtils.asJson(response));
    document.put(TASK_ID_PROPERTY, delegateTaskId);
    collection.insertOne(document);
  }

  @Override
  public void destroy() {
    mongoClient.close();
  }
}
