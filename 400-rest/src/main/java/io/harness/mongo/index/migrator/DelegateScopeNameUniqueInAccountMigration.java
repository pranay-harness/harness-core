/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.mongo.index.migrator;

import static org.mongodb.morphia.aggregation.Accumulator.accumulator;
import static org.mongodb.morphia.aggregation.Group.grouping;
import static org.mongodb.morphia.aggregation.Group.id;

import io.harness.delegate.beans.DelegateScope;
import io.harness.delegate.beans.DelegateScope.DelegateScopeKeys;
import io.harness.persistence.HIterator;

import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.AdvancedDatastore;
import org.mongodb.morphia.FindAndModifyOptions;
import org.mongodb.morphia.aggregation.AggregationPipeline;
import org.mongodb.morphia.query.MorphiaIterator;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

@Slf4j
public class DelegateScopeNameUniqueInAccountMigration implements Migrator {
  @Override
  public void execute(AdvancedDatastore datastore) {
    log.info("Starting migration of delegate scopes with duplicate names for accountId.");
    Query<AggregateResult> queryForMultipleItems =
        datastore.createQuery(AggregateResult.class).field("count").greaterThan(1);
    AggregationPipeline invalidEntryPipeline =
        datastore.createAggregation(DelegateScope.class)
            .group(id(grouping("accountId", "accountId"), grouping("name", "name")),
                grouping("count", accumulator("$sum", 1)))
            .match(queryForMultipleItems);

    try (HIterator<AggregateResult> invalidEntries =
             new HIterator((MorphiaIterator) invalidEntryPipeline.out(AggregateResult.class))) {
      for (AggregateResult invalidEntry : invalidEntries) {
        Query<DelegateScope> delegateScopeToRenameQuery = datastore.createQuery(DelegateScope.class)
                                                              .field(DelegateScopeKeys.accountId)
                                                              .equal(invalidEntry.get_id().getAccountId())
                                                              .field(DelegateScopeKeys.name)
                                                              .equal(invalidEntry.get_id().getName());
        try (HIterator<DelegateScope> delegateScopesToRename = new HIterator<>(delegateScopeToRenameQuery.fetch())) {
          int index = 1;
          for (DelegateScope delegateScope : delegateScopeToRenameQuery) {
            updateDelegateScope(datastore, index++, delegateScope);
          }
        }
      }
    }
    log.info("Finished migration of delegate scopes with duplicate names for accountId.");
  }

  private void updateDelegateScope(AdvancedDatastore datastore, int index, DelegateScope delegateScope) {
    try {
      log.info("Updating delegate scope.");
      Query<DelegateScope> updateQuery =
          datastore.createQuery(DelegateScope.class).field(DelegateScopeKeys.uuid).equal(delegateScope.getUuid());
      UpdateOperations<DelegateScope> updateOperations =
          datastore.createUpdateOperations(DelegateScope.class)
              .set(DelegateScopeKeys.name, delegateScope.getName() + "_" + index);
      datastore.findAndModify(updateQuery, updateOperations, new FindAndModifyOptions());
      log.info("Delegate scope updated successfully.");
    } catch (Exception e) {
      log.error("Unexpected error occurred while processing delegate scope.", e);
    }
  }
}
