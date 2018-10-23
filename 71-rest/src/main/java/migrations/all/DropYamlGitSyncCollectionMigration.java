package migrations.all;

import static io.harness.persistence.HPersistence.DEFAULT_STORE;

import com.google.inject.Inject;

import io.harness.persistence.ReadPref;
import migrations.Migration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.dl.WingsPersistence;

public class DropYamlGitSyncCollectionMigration implements Migration {
  private static final Logger logger = LoggerFactory.getLogger(DropYamlGitSyncCollectionMigration.class);
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    try {
      wingsPersistence.getCollection(DEFAULT_STORE, ReadPref.NORMAL, "yamlGitSync").drop();
    } catch (RuntimeException ex) {
      logger.error("Drop collection error", ex);
    }
  }
}
