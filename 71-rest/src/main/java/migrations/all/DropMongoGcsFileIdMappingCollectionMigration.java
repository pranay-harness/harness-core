package migrations.all;

import static io.harness.persistence.HPersistence.DEFAULT_STORE;

import com.google.inject.Inject;

import io.harness.persistence.ReadPref;
import lombok.extern.slf4j.Slf4j;
import migrations.Migration;
import software.wings.dl.WingsPersistence;

/**
 * Dropping the 'mongoGcsFileIdMapping' collection which is no longer needed or used.
 *
 * @author marklu on 2019-03-17
 */
@Slf4j
public class DropMongoGcsFileIdMappingCollectionMigration implements Migration {
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    try {
      wingsPersistence.getCollection(DEFAULT_STORE, ReadPref.NORMAL, "mongoGcsFileIdMapping").drop();
    } catch (RuntimeException ex) {
      logger.error("Drop collection error", ex);
    }
  }
}