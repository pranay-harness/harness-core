/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.service.impl;

import static com.mongodb.DBCollection.ID_FIELD_NAME;

import io.harness.callback.MongoDatabase;
import io.harness.delegate.beans.DelegateAsyncTaskResponse.DelegateAsyncTaskResponseKeys;
import io.harness.service.intfc.DelegateTaskResultsProvider;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.bson.types.Binary;

public class MongoDelegateTaskResultsProviderImpl implements DelegateTaskResultsProvider {
  private static final String ASYNC_TASK_COLLECTION_NAME_SUFFIX = "delegateAsyncTaskResponses";
  private final MongoClient mongoClient;
  private final MongoCollection<Document> asyncTaskResponseCollection;

  MongoDelegateTaskResultsProviderImpl(MongoDatabase mongoDatabase) {
    String connectionString = mongoDatabase.getConnection();
    MongoClientURI mongoClientURI = new MongoClientURI(connectionString);
    mongoClient = new MongoClient(mongoClientURI);
    com.mongodb.client.MongoDatabase database =
        mongoClient.getDatabase(Objects.requireNonNull(mongoClientURI.getDatabase()));

    String asyncTaskResponseCollectionName = StringUtils.isBlank(mongoDatabase.getCollectionNamePrefix())
        ? ASYNC_TASK_COLLECTION_NAME_SUFFIX
        : mongoDatabase.getCollectionNamePrefix() + "_" + ASYNC_TASK_COLLECTION_NAME_SUFFIX;
    asyncTaskResponseCollection = database.getCollection(asyncTaskResponseCollectionName);
  }

  @Override
  public byte[] getDelegateTaskResults(String delegateTaskId) {
    Document filter = new Document();
    filter.put(ID_FIELD_NAME, delegateTaskId);
    Document document = asyncTaskResponseCollection.find(filter).first();

    byte[] responseData = new byte[0];
    if (document != null && document.containsKey(DelegateAsyncTaskResponseKeys.responseData)) {
      responseData = ((Binary) document.get(DelegateAsyncTaskResponseKeys.responseData)).getData();
    }
    return responseData;
  }

  @Override
  public void destroy() {
    mongoClient.close();
  }
}
