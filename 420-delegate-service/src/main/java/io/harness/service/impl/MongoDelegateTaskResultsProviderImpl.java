/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
