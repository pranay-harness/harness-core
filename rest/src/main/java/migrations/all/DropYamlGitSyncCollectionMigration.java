package migrations.all;

import com.google.inject.Inject;

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
      wingsPersistence.getCollection("yamlGitSync").drop();
    } catch (RuntimeException ex) {
      logger.error("Drop collection error", ex);
    }
  }
}
