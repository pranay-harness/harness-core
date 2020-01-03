package software.wings.integration.security;

import static io.harness.rule.OwnerRule.UTKARSH;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.IntegrationTests;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.Account;
import software.wings.beans.KmsConfig;
import software.wings.beans.SecretManagerConfig;
import software.wings.security.encryption.EncryptedData;

import java.io.IOException;

/**
 * @author marklu on 10/1/19
 */
public class KmsIntegrationTest extends BaseSecretManagementIntegrationTest {
  private KmsConfig globalKmsDefault;
  private KmsConfig globalKmsBackup;

  @Before
  public void setUp() {
    super.setUp();

    globalKmsDefault = KmsConfig.builder()
                           .name("DefaultGlobalKMS")
                           .accessKey(kmsAccessKey)
                           .kmsArn(kmsArn)
                           .secretKey(kmsSecretKey)
                           .region(kmsRegion)
                           .build();
    kmsConfig.setAccountId(Account.GLOBAL_ACCOUNT_ID);
    kmsConfig.setDefault(true);

    globalKmsBackup = KmsConfig.builder()
                          .name("BackupGlobalKMS")
                          .accessKey(kmsAccessKey)
                          .kmsArn(kmsArn)
                          .secretKey(kmsSecretKey)
                          .region(kmsRegion)
                          .build();
    kmsConfig.setAccountId(Account.GLOBAL_ACCOUNT_ID);
    kmsConfig.setDefault(false);
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(IntegrationTests.class)
  public void testUpdateKmsSecretText_withBackupGlobalKMS_shouldSucceed() {
    String backupGlobalKmsConfigId = createGlobalKmsConfig(globalKmsBackup);

    String globalKmsConfigId = createGlobalKmsConfig(globalKmsDefault);
    KmsConfig savedKmsConfig = wingsPersistence.get(KmsConfig.class, globalKmsConfigId);
    assertThat(savedKmsConfig).isNotNull();
    savedKmsConfig.setDefault(true);
    wingsPersistence.save(savedKmsConfig);

    try {
      testUpdateSecretText(savedKmsConfig);
    } finally {
      wingsPersistence.delete(
          wingsPersistence.createQuery(EncryptedData.class).field("parentIds").hasThisOne(globalKmsConfigId));
      wingsPersistence.delete(SecretManagerConfig.class, globalKmsConfigId);
      wingsPersistence.delete(
          wingsPersistence.createQuery(EncryptedData.class).field("parentIds").hasThisOne(backupGlobalKmsConfigId));
      wingsPersistence.delete(SecretManagerConfig.class, backupGlobalKmsConfigId);
    }
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(IntegrationTests.class)
  public void testUpdateKmsSecretText_shouldSucceed() {
    String kmsConfigId = createKmsConfig(kmsConfig);
    KmsConfig savedKmsConfig = wingsPersistence.get(KmsConfig.class, kmsConfigId);
    assertThat(savedKmsConfig).isNotNull();
    savedKmsConfig.setDefault(true);
    wingsPersistence.save(savedKmsConfig);

    try {
      testUpdateSecretText(savedKmsConfig);
    } finally {
      deleteKmsConfig(kmsConfigId);
    }
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(IntegrationTests.class)
  public void testUpdateKmsEncryptedSecretFile_withNoContent_shouldNot_UpdateFileContent() throws IOException {
    String kmsConfigId = createKmsConfig(kmsConfig);
    KmsConfig savedKmsConfig = wingsPersistence.get(KmsConfig.class, kmsConfigId);
    assertThat(savedKmsConfig).isNotNull();
    savedKmsConfig.setDefault(true);
    wingsPersistence.save(savedKmsConfig);

    try {
      testUpdateEncryptedFile(savedKmsConfig);
    } finally {
      deleteKmsConfig(kmsConfigId);
    }
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(IntegrationTests.class)
  public void testUpdateKmsSecretTextName_shouldNotAlterSecretValue() {
    String kmsConfigId = createKmsConfig(kmsConfig);
    KmsConfig savedKmsConfig = wingsPersistence.get(KmsConfig.class, kmsConfigId);
    assertThat(savedKmsConfig).isNotNull();
    savedKmsConfig.setDefault(true);
    wingsPersistence.save(savedKmsConfig);

    try {
      testUpdateSecretTextNameOnly(savedKmsConfig);
    } finally {
      deleteKmsConfig(kmsConfigId);
    }
  }
}
