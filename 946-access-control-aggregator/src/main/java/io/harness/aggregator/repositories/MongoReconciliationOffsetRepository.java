/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.aggregator.repositories;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.mongodb.MongoNamespace;
import com.mongodb.client.model.RenameCollectionOptions;
import org.springframework.data.mongodb.core.MongoTemplate;

@OwnedBy(HarnessTeam.PL)
@Singleton
public class MongoReconciliationOffsetRepository {
  private final MongoTemplate mongoTemplate;

  @Inject
  public MongoReconciliationOffsetRepository(@Named("mongoTemplate") MongoTemplate mongoTemplate) {
    this.mongoTemplate = mongoTemplate;
  }

  public void cleanCollection(String collectionName) {
    mongoTemplate.dropCollection(collectionName);
    mongoTemplate.createCollection(collectionName);
  }

  public void renameCollectionAToCollectionB(String collectionA, String collectionB) {
    MongoNamespace mongoNamespace = new MongoNamespace(mongoTemplate.getDb().getName(), collectionB);
    mongoTemplate.getCollection(collectionA)
        .renameCollection(mongoNamespace, new RenameCollectionOptions().dropTarget(true));
  }
}
