package io.harness.service.impl;

import static com.mongodb.DBCollection.ID_FIELD_NAME;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import io.harness.callback.MongoDatabase;
import io.harness.delegate.beans.DelegateAsyncTaskResponse.DelegateAsyncTaskResponseKeys;
import io.harness.service.intfc.DelegateTaskResultsProvider;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.bson.types.Binary;

public class MongoDelegateTaskResultsProviderImpl implements DelegateTaskResultsProvider {
  private static final String PARKED_TASK_COLLECTION_NAME_SUFFIX = "delegateParkedTaskResponses";
  private final MongoClient mongoClient;
  private final MongoCollection<Document> parkedTaskResponseCollection;

  MongoDelegateTaskResultsProviderImpl(MongoDatabase mongoDatabase) {
    String connectionString = mongoDatabase.getConnection();
    mongoClient = new MongoClient(new MongoClientURI(connectionString));
    com.mongodb.client.MongoDatabase database =
        mongoClient.getDatabase(connectionString.substring(connectionString.lastIndexOf('/') + 1));

    String parkedTaskResponseCollectionName = StringUtils.isBlank(mongoDatabase.getCollectionNamePrefix())
        ? PARKED_TASK_COLLECTION_NAME_SUFFIX
        : mongoDatabase.getCollectionNamePrefix() + "_" + PARKED_TASK_COLLECTION_NAME_SUFFIX;
    parkedTaskResponseCollection = database.getCollection(parkedTaskResponseCollectionName);
  }

  @Override
  public byte[] getDelegateTaskResults(String delegateTaskId) {
    Document filter = new Document();
    filter.put(ID_FIELD_NAME, delegateTaskId);
    Document document = parkedTaskResponseCollection.find(filter).first();

    byte[] responseData = new byte[0];
    if (document != null && document.containsKey(DelegateAsyncTaskResponseKeys.responseData)) {
      responseData = ((Binary) document.get(DelegateAsyncTaskResponseKeys.responseData)).getData();
    }
    parkedTaskResponseCollection.deleteOne(filter);
    return responseData;
  }

  @Override
  public void destroy() {
    mongoClient.close();
  }
}
