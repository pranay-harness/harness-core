
package migrations.all;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.inject.Inject;

import io.harness.persistence.HIterator;
import lombok.extern.slf4j.Slf4j;
import migrations.Migration;
import org.mongodb.morphia.query.Query;
import software.wings.beans.GitCommit;
import software.wings.beans.GitCommit.GitCommitKeys;
import software.wings.dl.WingsPersistence;
import software.wings.yaml.errorhandling.GitSyncError;
import software.wings.yaml.errorhandling.GitSyncError.GitSyncErrorKeys;

import java.util.Arrays;
import java.util.List;

@Slf4j
public class AddCommitTimeToGitSyncError implements Migration {
  @Inject private WingsPersistence wingsPersistence;
  private static final List<String> NULL_AND_EMPTY = Arrays.asList(null, "");
  private Table<String, String, GitCommit> gitCommitTable = HashBasedTable.create();

  @Override
  public void migrate() {
    logger.info("Running migration AddCommitTimeToGitSyncError");

    Query<GitSyncError> query = wingsPersistence.createAuthorizedQuery(GitSyncError.class)
                                    .field(GitSyncErrorKeys.gitCommitId)
                                    .notIn(NULL_AND_EMPTY);

    try (HIterator<GitSyncError> records = new HIterator<>(query.fetch())) {
      while (records.hasNext()) {
        GitSyncError syncError = records.next();
        try {
          if (isNotEmpty(syncError.getGitCommitId()) && syncError.getCommitTime() == null) {
            Long commitCreatedAtTime = getCreationTimeOfCommit(syncError.getAccountId(), syncError.getGitCommitId());
            if (commitCreatedAtTime != null) {
              syncError.setCommitTime(commitCreatedAtTime);
            } else {
              logger.info("The createdAt value is null for the commit {} in account", syncError.getUuid(),
                  syncError.getAccountId());
              syncError.setCommitTime(syncError.getLastUpdatedAt());
            }
          }
          wingsPersistence.save(syncError);
        } catch (Exception e) {
          logger.error("Error while processing gitsyncerror id =" + syncError.getUuid(), e);
        }
      }
    }
    logger.info("Completed migration:  AddCommitTimeToGitSyncError");
  }

  private Long getCreationTimeOfCommit(String accountId, String gitCommitId) {
    GitCommit gitCommit = gitCommitTable.get(accountId, gitCommitId);
    if (gitCommit == null) {
      gitCommit = wingsPersistence.createQuery(GitCommit.class)
                      .filter(GitCommitKeys.accountId, accountId)
                      .filter(GitCommitKeys.commitId, gitCommitId)
                      .project(GitCommitKeys.createdAt, true)
                      .get();
      if (gitCommit == null) {
        logger.info("The gitCommitId {} was not found in the db for the account {}", gitCommitId, accountId);
        return null;
      }
      gitCommitTable.put(accountId, gitCommitId, gitCommit);
    }
    return gitCommit.getCreatedAt();
  }
}