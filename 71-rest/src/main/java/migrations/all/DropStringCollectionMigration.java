package migrations.all;

import static io.harness.persistence.HPersistence.DEFAULT_STORE;

import com.google.inject.Inject;

import io.harness.persistence.ReadPref;
import migrations.Migration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.dl.WingsPersistence;

public class DropStringCollectionMigration implements Migration {
  private static final Logger logger = LoggerFactory.getLogger(DropStringCollectionMigration.class);
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    try {
      wingsPersistence.getCollection(DEFAULT_STORE, ReadPref.NORMAL, "String").drop();
    } catch (RuntimeException ex) {
      logger.error("Drop collection error", ex);
    }
  }
}
