/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.mongo.index.migrator;

import static org.mongodb.morphia.aggregation.Accumulator.accumulator;
import static org.mongodb.morphia.aggregation.Group.grouping;
import static org.mongodb.morphia.aggregation.Group.id;

import io.harness.persistence.HIterator;

import software.wings.beans.ApiKeyEntry;
import software.wings.beans.ApiKeyEntry.ApiKeyEntryKeys;

import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.AdvancedDatastore;
import org.mongodb.morphia.FindAndModifyOptions;
import org.mongodb.morphia.aggregation.AggregationPipeline;
import org.mongodb.morphia.query.MorphiaIterator;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

@Slf4j
public class ApiKeysNameUniqueInAccountMigration implements Migrator {
  @Override
  public void execute(AdvancedDatastore datastore) {
    log.info("Starting migration of API keys with duplicate names for accountId.");
    Query<AggregateResult> queryForMultipleItems =
        datastore.createQuery(AggregateResult.class).field("count").greaterThan(1);
    AggregationPipeline invalidEntryPipeline =
        datastore.createAggregation(ApiKeyEntry.class)
            .group(id(grouping("accountId", "accountId"), grouping("name", "name")),
                grouping("count", accumulator("$sum", 1)))
            .match(queryForMultipleItems);

    try (HIterator<AggregateResult> invalidEntries =
             new HIterator((MorphiaIterator) invalidEntryPipeline.out(AggregateResult.class))) {
      for (AggregateResult invalidEntry : invalidEntries) {
        Query<ApiKeyEntry> apiKeyToRenameQuery = datastore.createQuery(ApiKeyEntry.class)
                                                     .field(ApiKeyEntryKeys.accountId)
                                                     .equal(invalidEntry.get_id().getAccountId())
                                                     .field(ApiKeyEntryKeys.name)
                                                     .equal(invalidEntry.get_id().getName());
        try (HIterator<ApiKeyEntry> apiKeysToRename = new HIterator<>(apiKeyToRenameQuery.fetch())) {
          int index = 1;
          for (ApiKeyEntry apiKeyEntry : apiKeysToRename) {
            updateApiKey(datastore, index++, apiKeyEntry);
          }
        }
      }
    }
    log.info("Finished migration of delegate profiles with duplicate names for accountId.");
  }

  private void updateApiKey(AdvancedDatastore dataStore, int index, ApiKeyEntry keyEntry) {
    try {
      log.info("Updating API key.");
      Query<ApiKeyEntry> updateQuery =
          dataStore.createQuery(ApiKeyEntry.class).field(ApiKeyEntryKeys.uuid).equal(keyEntry.getUuid());
      UpdateOperations<ApiKeyEntry> updateOperations = dataStore.createUpdateOperations(ApiKeyEntry.class)
                                                           .set(ApiKeyEntryKeys.name, keyEntry.getName() + "_" + index);
      dataStore.findAndModify(updateQuery, updateOperations, new FindAndModifyOptions());
      log.info("API key updated successfully.");
    } catch (Exception e) {
      log.error("Unexpected error occurred while processing API key.", e);
    }
  }
}
