package migrations.all;

import static io.harness.persistence.HPersistence.DEFAULT_STORE;

import com.google.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import migrations.Migration;
import software.wings.dl.WingsPersistence;

@Slf4j
public class DropAppIdIndexOnCommandLogs implements Migration {
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    try {
      wingsPersistence.getCollection(DEFAULT_STORE, "commandLogs").dropIndex("appId_1");
    } catch (RuntimeException ex) {
      logger.error("Drop index error", ex);
    }
  }
}
