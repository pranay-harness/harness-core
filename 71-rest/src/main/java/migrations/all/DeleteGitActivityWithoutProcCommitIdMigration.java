package migrations.all;

import static io.harness.persistence.HPersistence.DEFAULT_STORE;

import com.google.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import migrations.Migration;
import org.mongodb.morphia.query.Query;
import software.wings.dl.WingsPersistence;
import software.wings.yaml.gitSync.GitFileActivity;
import software.wings.yaml.gitSync.GitFileActivity.GitFileActivityKeys;

@Slf4j
public class DeleteGitActivityWithoutProcCommitIdMigration implements Migration {
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    try {
      wingsPersistence.getCollection(DEFAULT_STORE, "gitFileActivity").dropIndex("uniqueIdx_1");
    } catch (RuntimeException ex) {
      logger.error("Drop index error", ex);
    }

    try {
      Query<GitFileActivity> query = wingsPersistence.createQuery(GitFileActivity.class)
                                         .field(GitFileActivityKeys.processingCommitId)
                                         .doesNotExist();

      wingsPersistence.delete(query);
    } catch (Exception ex) {
      logger.error("Error while deleting activities not having processingCommitId field", ex);
    }
  }
}