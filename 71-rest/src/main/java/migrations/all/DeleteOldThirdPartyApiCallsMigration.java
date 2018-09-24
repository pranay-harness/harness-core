package migrations.all;

import static org.slf4j.LoggerFactory.getLogger;

import com.google.inject.Inject;

import migrations.Migration;
import org.slf4j.Logger;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.ThirdPartyApiCallLog;

public class DeleteOldThirdPartyApiCallsMigration implements Migration {
  private static final Logger logger = getLogger(DeleteOldThirdPartyApiCallsMigration.class);
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    try {
      wingsPersistence.delete(wingsPersistence.createQuery(ThirdPartyApiCallLog.class));
    } catch (RuntimeException ex) {
      logger.error("clear collection error", ex);
    }
  }
}
