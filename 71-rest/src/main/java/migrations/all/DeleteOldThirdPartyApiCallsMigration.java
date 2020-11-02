package migrations.all;

import com.google.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import migrations.Migration;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.ThirdPartyApiCallLog;

@Slf4j
public class DeleteOldThirdPartyApiCallsMigration implements Migration {
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    try {
      wingsPersistence.delete(wingsPersistence.createQuery(ThirdPartyApiCallLog.class));
    } catch (RuntimeException ex) {
      log.error("clear collection error", ex);
    }
  }
}
