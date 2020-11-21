package migrations.all;

import software.wings.beans.GitFileActivitySummary;
import software.wings.dl.WingsPersistence;
import software.wings.yaml.gitSync.GitFileActivity;

import com.google.inject.Inject;
import com.mongodb.BasicDBObject;
import lombok.extern.slf4j.Slf4j;
import migrations.OnPrimaryManagerMigration;

@Slf4j
public class DeleteGitFileActivityAndGitFileAcitivitySummary implements OnPrimaryManagerMigration {
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    log.info("Starting the migration DeleteGitFileActivityAndGitFileAcitivitySummary");
    try {
      log.info("Deleting the records of Git File Activity collection");
      wingsPersistence.getCollection(GitFileActivity.class).remove(new BasicDBObject());
      log.info("Records deleted successfully for Git File Activity");
    } catch (Exception ex) {
      log.error("Exception while deleting the records from the db", ex);
    }

    try {
      log.info("Deleting the records of Git File Activity Summary collection");
      wingsPersistence.getCollection(GitFileActivitySummary.class).remove(new BasicDBObject());
      log.info("Records deleted successfully for Git File Activity Summary");
    } catch (Exception ex) {
      log.error("Exception while deleting the records from the db", ex);
    }
    log.info("Completed the migration DeleteGitFileActivityAndGitFileAcitivitySummary");
  }
}
