package software.wings.integration.migration;

import com.google.inject.Inject;

import org.junit.Ignore;
import org.junit.Test;
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
  @Inject private WingsPersistence wingsPersistence;

  @Test
  public void migrateGcpConfig() throws InterruptedException {
    List<SettingAttribute> gcpConfigs =
        wingsPersistence.createQuery(SettingAttribute.class).field("value.type").equal("GCP").asList();

    System.out.println("will update " + gcpConfigs.size() + " records");

    int updated = 0;
    for (SettingAttribute settingAttribute : gcpConfigs) {
      String accountId = settingAttribute.getAccountId();
      System.out.println("accountId: " + accountId);
      GcpConfig gcpConfig = (GcpConfig) settingAttribute.getValue();
      gcpConfig.setAccountId(accountId);

      System.out.println("value:" + gcpConfig.getServiceAccountKeyFileContent());
      updated++;

      //      wingsPersistence.save(settingAttribute);
    }

    System.out.println("Complete. Updated " + updated + " gcp configs.");
  }
}
