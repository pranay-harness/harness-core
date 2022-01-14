package io.harness.migrations.all;

import static io.harness.mongo.MongoUtils.setUnset;

import io.harness.migrations.Migration;

import software.wings.beans.GitCommit;
import software.wings.beans.GitCommit.GitCommitKeys;
import software.wings.beans.GitCommit.Status;
import software.wings.dl.WingsPersistence;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.mongodb.morphia.query.UpdateResults;

@Slf4j
public class GitCommitStatusMigration implements Migration {
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    log.info("Running migration GitCommitStatusMigration");

    try {
      UpdateOperations<GitCommit> ops = wingsPersistence.createUpdateOperations(GitCommit.class);
      setUnset(ops, GitCommitKeys.status, Status.COMPLETED_WITH_ERRORS);

      Query<GitCommit> gitCommitQuery =
          wingsPersistence.createQuery(GitCommit.class).filter(GitCommitKeys.status, Status.FAILED);

      final UpdateResults updateResults = wingsPersistence.update(gitCommitQuery, ops);
      log.info("update results = {}", updateResults);
    } catch (Exception e) {
      log.error("Error running migration GitCommitStatusMigration", e);
    }
    log.info("Completed migration:  GitSyncErrorGitDetailsMigration");
  }
}
