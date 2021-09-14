/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.migrations.all;

import static io.harness.persistence.HPersistence.DEFAULT_STORE;

import io.harness.migrations.Migration;

import software.wings.dl.WingsPersistence;
import software.wings.yaml.gitSync.GitFileActivity;
import software.wings.yaml.gitSync.GitFileActivity.GitFileActivityKeys;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;

@Slf4j
public class DeleteGitActivityWithoutProcCommitIdMigration implements Migration {
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    try {
      wingsPersistence.getCollection(DEFAULT_STORE, "gitFileActivity").dropIndex("uniqueIdx_1");
    } catch (RuntimeException ex) {
      log.error("Drop index error", ex);
    }

    try {
      Query<GitFileActivity> query = wingsPersistence.createQuery(GitFileActivity.class)
                                         .field(GitFileActivityKeys.processingCommitId)
                                         .doesNotExist();

      wingsPersistence.delete(query);
    } catch (Exception ex) {
      log.error("Error while deleting activities not having processingCommitId field", ex);
    }
  }
}
