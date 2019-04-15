package migrations.all;

import static io.harness.persistence.HPersistence.DEFAULT_STORE;

import com.google.inject.Inject;

import io.harness.persistence.ReadPref;
import lombok.extern.slf4j.Slf4j;
import migrations.Migration;
import software.wings.dl.WingsPersistence;

@Slf4j
public class DropDelegateScopeCollectionMigration implements Migration {
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    try {
      wingsPersistence.getCollection(DEFAULT_STORE, ReadPref.NORMAL, "delegateScope").drop();
    } catch (RuntimeException ex) {
      logger.error("Drop collection error", ex);
    }
  }
}
