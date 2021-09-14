/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.migrations.all;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.Delegate;
import io.harness.delegate.beans.Delegate.DelegateKeys;
import io.harness.delegate.beans.DelegateGroup;
import io.harness.migrations.Migration;
import io.harness.persistence.HIterator;
import io.harness.persistence.HPersistence;

import software.wings.service.intfc.DelegateService;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.FindAndModifyOptions;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

@Slf4j
@OwnedBy(HarnessTeam.DEL)
public class DelegatesWithoutGroupMigration implements Migration {
  @Inject private HPersistence persistence;
  @Inject private DelegateService delegateService;

  @Override
  public void migrate() {
    log.info("Starting the migration of the delegates without an assigned delegate group.");
    Query<Delegate> delegatesQuery = persistence.createQuery(Delegate.class)
                                         .field(DelegateKeys.delegateGroupName)
                                         .exists()
                                         .field(DelegateKeys.delegateGroupId)
                                         .doesNotExist();

    try (HIterator<Delegate> delegatesWithoutGroup = new HIterator<>(delegatesQuery.fetch())) {
      for (Delegate delegate : delegatesWithoutGroup) {
        log.debug("Delegate with ID {} has no delegate group assigned.", delegate.getUuid());
        DelegateGroup delegateGroup =
            delegateService.upsertDelegateGroup(delegate.getDelegateGroupName(), delegate.getAccountId(), null);

        log.debug("Assigning group with name: {} and id: {} for account: {} to delegate: {}", delegateGroup.getName(),
            delegateGroup.getUuid(), delegateGroup.getAccountId(), delegate.getUuid());
        updateGroupIdOnDelegate(delegate.getUuid(), delegate.getAccountId(), delegateGroup.getUuid());
      }
    }
    log.info("The migration for the delegates with no delegate group assigned has finished.");
  }

  private void updateGroupIdOnDelegate(String delegateId, String accountId, String delegateGroupId) {
    Query<Delegate> delegateQuery = persistence.createQuery(Delegate.class)
                                        .filter(DelegateKeys.uuid, delegateId)
                                        .filter(DelegateKeys.accountId, accountId);

    UpdateOperations<Delegate> updateOperations =
        persistence.createUpdateOperations(Delegate.class).set(DelegateKeys.delegateGroupId, delegateGroupId);
    persistence.findAndModify(delegateQuery, updateOperations, new FindAndModifyOptions());
  }
}
