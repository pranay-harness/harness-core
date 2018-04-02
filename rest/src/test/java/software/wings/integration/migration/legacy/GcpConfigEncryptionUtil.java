package software.wings.integration.migration.legacy;

import com.google.inject.Inject;

import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.WingsBaseTest;
import software.wings.beans.GcpConfig;
import software.wings.beans.SettingAttribute;
import software.wings.dl.WingsPersistence;
import software.wings.rules.Integration;

import java.util.List;

/**
 * Created by rsingh on 10/17/17.
 */
@Integration
@Ignore
public class GcpConfigEncryptionUtil extends WingsBaseTest {
  private static final Logger logger = LoggerFactory.getLogger(GcpConfigEncryptionUtil.class);

  @Inject private WingsPersistence wingsPersistence;

  @Test
  public void migrateGcpConfig() throws InterruptedException {
    List<SettingAttribute> gcpConfigs =
        wingsPersistence.createQuery(SettingAttribute.class).filter("value.type", "GCP").asList();

    logger.info("will update " + gcpConfigs.size() + " records");

    int updated = 0;
    for (SettingAttribute settingAttribute : gcpConfigs) {
      String accountId = settingAttribute.getAccountId();
      logger.info("accountId: " + accountId);
      GcpConfig gcpConfig = (GcpConfig) settingAttribute.getValue();
      gcpConfig.setAccountId(accountId);

      logger.info("value:" + gcpConfig.getServiceAccountKeyFileContent());
      updated++;

      //      wingsPersistence.save(settingAttribute);
    }

    logger.info("Complete. Updated " + updated + " gcp configs.");
  }
}
