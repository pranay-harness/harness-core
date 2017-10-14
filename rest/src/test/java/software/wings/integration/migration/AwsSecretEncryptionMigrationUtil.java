package software.wings.integration.migration;

import com.google.inject.Inject;

import org.junit.Ignore;
import org.junit.Test;
import software.wings.WingsBaseTest;
import software.wings.beans.AwsConfig;
import software.wings.beans.SettingAttribute;
import software.wings.dl.WingsPersistence;
import software.wings.rules.Integration;
import software.wings.security.encryption.SimpleEncryption;

import java.util.List;

/**
 * Migration script to set usePublicDns on AWS infrastructure mappings to true since that has been the default.
 *
 * @author brett on 10/1/17
 */
@Integration
@Ignore
public class AwsSecretEncryptionMigrationUtil extends WingsBaseTest {
  @Inject private WingsPersistence wingsPersistence;

  @Test
  public void setAwsInfraMappingsUsePublicDns() throws InterruptedException {
    List<SettingAttribute> settingAttributes = wingsPersistence.createQuery(SettingAttribute.class)
                                                   .field("category")
                                                   .equal("CLOUD_PROVIDER")
                                                   .field("value.type")
                                                   .equal("AWS")
                                                   .asList();

    System.out.println("will update " + settingAttributes.size() + " records");
    int updated = 0;
    for (SettingAttribute settingAttribute : settingAttributes) {
      AwsConfig awsConfig = (AwsConfig) settingAttribute.getValue();
      SimpleEncryption encryption = new SimpleEncryption(settingAttribute.getAccountId());
      char[] encrypted = encryption.encryptChars(awsConfig.getSecretKey());
      awsConfig.setSecretKey(encrypted);

      wingsPersistence.save(settingAttribute);
      updated++;
      Thread.sleep(100);
    }

    System.out.println("Complete. Updated " + updated + " aws configs.");
  }
}
