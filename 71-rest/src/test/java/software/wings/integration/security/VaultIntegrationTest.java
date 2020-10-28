package software.wings.integration.security;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.rule.OwnerRule.UTKARSH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import io.harness.beans.EncryptedData;
import io.harness.beans.SecretManagerConfig;
import io.harness.category.element.DeprecatedIntegrationTests;
import io.harness.expression.SecretString;
import io.harness.helpers.ext.vault.SecretEngineSummary;
import io.harness.rule.Owner;
import io.harness.security.encryption.EncryptionType;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.Account;
import software.wings.beans.GcpConfig;
import software.wings.beans.LocalEncryptionConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.VaultConfig;
import software.wings.service.intfc.security.SecretManager;
import software.wings.service.intfc.security.VaultService;
import software.wings.settings.SettingValue;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Field;
import java.util.List;

/**
 * Created by rsingh on 9/21/18.
 */
public class VaultIntegrationTest extends SecretManagementIntegrationTestBase {
  private static final String VAULT_URL_1 = "http://127.0.0.1:8200";
  private static final String VAULT_URL_2 = "http://127.0.0.1:8300";
  private static final String VAULT_BASE_PATH = "/foo/bar";
  private static final String VAULT_BASE_PATH_2 = "foo2/bar2/ ";
  private static final String VAULT_BASE_PATH_3 = " /";

  private VaultConfig vaultConfig;
  private VaultConfig vaultConfig2;

  private VaultConfig vaultConfigWithBasePath;
  private VaultConfig vaultConfigWithBasePath2;
  private VaultConfig vaultConfigWithBasePath3;

  private LocalEncryptionConfig localEncryptionConfig;

  @Override
  @Before
  public void setUp() {
    super.setUp();

    vaultConfig = VaultConfig.builder().name("TestVault").vaultUrl(VAULT_URL_1).authToken(vaultToken).build();
    vaultConfig.setAccountId(accountId);
    vaultConfig.setDefault(true);

    vaultConfig2 = VaultConfig.builder().name("TestVault2").vaultUrl(VAULT_URL_2).authToken(vaultToken).build();
    vaultConfig2.setAccountId(accountId);
    vaultConfig2.setDefault(true);

    vaultConfigWithBasePath = VaultConfig.builder()
                                  .name("TestVaultWithBasePath")
                                  .vaultUrl(VAULT_URL_1)
                                  .authToken(vaultToken)
                                  .basePath(VAULT_BASE_PATH)
                                  .build();
    vaultConfigWithBasePath.setAccountId(accountId);
    vaultConfigWithBasePath.setDefault(true);

    vaultConfigWithBasePath2 = VaultConfig.builder()
                                   .name("TestVaultWithBasePath")
                                   .vaultUrl(VAULT_URL_1)
                                   .authToken(vaultToken)
                                   .basePath(VAULT_BASE_PATH_2)
                                   .build();
    vaultConfigWithBasePath2.setAccountId(accountId);
    vaultConfigWithBasePath2.setDefault(true);

    vaultConfigWithBasePath3 = VaultConfig.builder()
                                   .name("TestVaultWithRootBasePath")
                                   .vaultUrl(VAULT_URL_1)
                                   .authToken(vaultToken)
                                   .basePath(VAULT_BASE_PATH_3)
                                   .build();
    vaultConfigWithBasePath3.setAccountId(accountId);
    vaultConfigWithBasePath3.setDefault(true);

    localEncryptionConfig = localEncryptionService.getEncryptionConfig(accountId);
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(DeprecatedIntegrationTests.class)
  @Ignore("skipping the integration test")
  public void test_LocalEncryption_shouldSucceed() {
    String secretValue = "TestSecret";
    EncryptedData encryptedData =
        localEncryptionService.encrypt(secretValue.toCharArray(), accountId, localEncryptionConfig);
    char[] decrypted = localEncryptionService.decrypt(encryptedData, accountId, localEncryptionConfig);
    assertThat(new String(decrypted)).isEqualTo(secretValue);

    String fileContent = "This file is to be encrypted";
    EncryptedData encryptedFileData = localEncryptionService.encryptFile(
        accountId, localEncryptionConfig, "TestEncryptedFile", fileContent.getBytes());
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    localEncryptionService.decryptToStream(accountId, encryptedFileData, outputStream);
    assertThat(new String(outputStream.toByteArray())).isEqualTo(fileContent);
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(DeprecatedIntegrationTests.class)
  @Ignore("skipping the integration test")
  public void test_VaultOperations_accountLocalEncryptionEnabled_shouldFail() {
    // 1. Create a new Vault config.
    String vaultConfigId = createVaultConfig(vaultConfig);

    // 2. update account to be 'localEncryptionEnabled'
    wingsPersistence.updateField(Account.class, accountId, "localEncryptionEnabled", true);

    // 3. account encryption type is LOCAL
    EncryptionType encryptionType = secretManager.getEncryptionType(accountId);
    assertThat(encryptionType).isEqualTo(EncryptionType.LOCAL);

    // 4. No secret manager will be returned
    List<SecretManagerConfig> secretManagers = secretManager.listSecretManagers(accountId);
    assertThat(secretManagers).isEmpty();

    // 5. Create new VAULT secret manager should fail.
    try {
      createVaultConfig(vaultConfig2);
      fail("Can't create new Vault secret manager if the account has LOCAL encryption enabled!");
    } catch (Exception e) {
      // Exception is expected.
    } finally {
      // 6. Disable LOCAL encryption for this account
      wingsPersistence.updateField(Account.class, accountId, "localEncryptionEnabled", false);

      // 7. Delete the vault config
      deleteVaultConfig(vaultConfigId);
    }
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(DeprecatedIntegrationTests.class)
  @Ignore("skipping the integration test")
  public void testListVaultSecretEngines() {
    String vaultConfigId = createVaultConfig(vaultConfig);
    VaultConfig savedVaultConfig = wingsPersistence.get(VaultConfig.class, vaultConfigId);
    assertThat(savedVaultConfig).isNotNull();

    try {
      List<SecretEngineSummary> secretEngineSummaries = listSecretEngines(vaultConfig);
      boolean foundDefaultEngine = secretEngineSummaries.stream().anyMatch(
          secretEngineSummary -> secretEngineSummary.getName().equals(VaultService.DEFAULT_SECRET_ENGINE_NAME));
      assertThat(foundDefaultEngine).isTrue();
    } finally {
      deleteVaultConfig(vaultConfigId);
    }
  }

  //  @Test
  //  @Owner(emails = UNKNOWN)
  //  @Category(IntegrationTests.class)
  //  public void testUpdateSecret_changeDefaultSecretManager_fromVaultToKms_shouldSucceed() {
  //    // Start with VAULT as default secret manager
  //    String vaultConfigId = createVaultConfig(vaultConfig);
  //    VaultConfig savedVaultConfig = wingsPersistence.get(VaultConfig.class, vaultConfigId);
  //    assertThat(savedVaultConfig).isNotNull();
  //
  //    String secretUuid = null;
  //    try {
  //      // Created a secret in Vault.
  //      secretUuid = createSecretText("FooBarSecret", "MySecretValue", null);
  //
  //      // No change to use KMS as DEFAULT secret manager!
  //      String kmsConfigId = createKmsConfig(kmsConfig);
  //      KmsConfig savedKmsConfig = wingsPersistence.get(KmsConfig.class, kmsConfigId);
  //      assertThat(savedKmsConfig).isNotNull();
  //
  //      try {
  //        // Update will save the secret in KMS as KMS is the default now.
  //        updateSecretText(secretUuid, "FooBarSecret_Modified", "MySecretValue_Modified", null);
  //        verifySecret(secretUuid, "FooBarSecret_Modified", "MySecretValue_Modified", savedKmsConfig);
  //        deleteSecretText(secretUuid);
  //      } finally {
  //        deleteKmsConfig(kmsConfigId);
  //      }
  //    } finally {
  //      deleteSecretText(secretUuid);
  //      deleteVaultConfig(vaultConfigId);
  //    }
  //  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(DeprecatedIntegrationTests.class)
  @Ignore("skipping the integration test")
  public void testUpdateVaultEncryptedSeretFile_withNoContent_shouldNot_UpdateFileContent() {
    String vaultConfigId = createVaultConfig(vaultConfig);
    VaultConfig savedVaultConfig = wingsPersistence.get(VaultConfig.class, vaultConfigId);
    assertThat(savedVaultConfig).isNotNull();

    try {
      testUpdateEncryptedFile(savedVaultConfig);
    } finally {
      deleteVaultConfig(vaultConfigId);
    }
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(DeprecatedIntegrationTests.class)
  @Ignore("skipping the integration test")
  public void testCreateUpdateDeleteVaultConfig_shouldSucceed() {
    // 1. Create a new Vault config.
    String vaultConfigId = createVaultConfig(vaultConfig);

    try {
      VaultConfig savedVaultConfig = wingsPersistence.get(VaultConfig.class, vaultConfigId);
      assertThat(savedVaultConfig).isNotNull();

      // 2. Update the existing vault config to make it default
      savedVaultConfig.setAuthToken(vaultToken);
      updateVaultConfig(savedVaultConfig);

      savedVaultConfig = wingsPersistence.get(VaultConfig.class, vaultConfigId);
      assertThat(savedVaultConfig).isNotNull();
      assertThat(savedVaultConfig.isDefault()).isTrue();
    } finally {
      // 3. Delete the vault config
      deleteVaultConfig(vaultConfigId);
    }
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(DeprecatedIntegrationTests.class)
  @Ignore("skipping the integration test")
  public void test_updateVaultBasePath_shouldSucceed() {
    // Create the first default vault config
    String vaultConfigId = createVaultConfig(vaultConfig);

    VaultConfig savedVaultConfig = wingsPersistence.get(VaultConfig.class, vaultConfigId);
    assertThat(savedVaultConfig).isNotNull();

    // Update the vault base path
    savedVaultConfig.setBasePath(VAULT_BASE_PATH);
    savedVaultConfig.setAuthToken(SecretString.SECRET_MASK);
    updateVaultConfig(savedVaultConfig);

    savedVaultConfig = wingsPersistence.get(VaultConfig.class, vaultConfigId);
    assertThat(savedVaultConfig.getBasePath()).isEqualTo(VAULT_BASE_PATH);

    deleteVaultConfig(vaultConfigId);
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(DeprecatedIntegrationTests.class)
  @Ignore("skipping the integration test")
  public void testUpdateVaultSecretTextName_shouldNotAlterSecretValue() {
    String vaultConfigId = createVaultConfig(vaultConfig);
    VaultConfig savedVaultConfig = wingsPersistence.get(VaultConfig.class, vaultConfigId);
    assertThat(savedVaultConfig).isNotNull();

    try {
      testUpdateSecretTextNameOnly(savedVaultConfig);
    } finally {
      deleteVaultConfig(vaultConfigId);
    }
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(DeprecatedIntegrationTests.class)
  @Ignore("skipping the integration test")
  public void test_createNewDefaultVault_shouldUnsetPreviousDefaultVaultConfig() {
    // Create the first default vault config
    String vaultConfigId = createVaultConfig(vaultConfig);
    // Create 2nd default vault config. The 1 vault will be set to be non-default.
    String vaultConfig2Id = createVaultConfig(vaultConfig2);

    VaultConfig savedVaultConfig = wingsPersistence.get(VaultConfig.class, vaultConfigId);
    assertThat(savedVaultConfig).isNotNull();

    VaultConfig savedVaultConfig2 = wingsPersistence.get(VaultConfig.class, vaultConfig2Id);
    assertThat(savedVaultConfig2).isNotNull();

    try {
      assertThat(savedVaultConfig.isDefault()).isFalse();
      assertThat(savedVaultConfig2.isDefault()).isTrue();

      // Update 1st vault config to be default again. 2nd vault config will be set to be non-default.
      savedVaultConfig.setDefault(true);
      savedVaultConfig.setAuthToken(vaultToken);
      updateVaultConfig(savedVaultConfig);

      savedVaultConfig = wingsPersistence.get(VaultConfig.class, vaultConfigId);
      assertThat(savedVaultConfig.isDefault()).isTrue();

      savedVaultConfig2 = wingsPersistence.get(VaultConfig.class, vaultConfig2Id);
      assertThat(savedVaultConfig2.isDefault()).isFalse();
    } finally {
      // Delete both vault configs.
      deleteVaultConfig(vaultConfigId);
      deleteVaultConfig(vaultConfig2Id);
    }
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(DeprecatedIntegrationTests.class)
  @Ignore("skipping the integration test")
  public void test_UpdateSecretTextWithValue_VaultWithBasePath_shouldSucceed() {
    // Create the first default vault config
    String vaultConfigId = createVaultConfig(vaultConfigWithBasePath);
    VaultConfig savedVaultConfig = wingsPersistence.get(VaultConfig.class, vaultConfigId);
    assertThat(savedVaultConfig).isNotNull();

    testUpdateSecretText(savedVaultConfig);
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(DeprecatedIntegrationTests.class)
  @Ignore("skipping the integration test")
  public void test_UpdateSecretTextWithValue_VaultWithBasePath2_shouldSucceed() {
    // Create the first default vault config
    String vaultConfigId = createVaultConfig(vaultConfigWithBasePath2);
    VaultConfig savedVaultConfig = wingsPersistence.get(VaultConfig.class, vaultConfigId);
    assertThat(savedVaultConfig).isNotNull();

    testUpdateSecretText(savedVaultConfig);
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(DeprecatedIntegrationTests.class)
  @Ignore("skipping the integration test")
  public void test_UpdateSecretTextWithValue_shouldSucceed() {
    // Create the first default vault config
    String vaultConfigId = createVaultConfig(vaultConfig);
    VaultConfig savedVaultConfig = wingsPersistence.get(VaultConfig.class, vaultConfigId);
    assertThat(savedVaultConfig).isNotNull();

    testUpdateSecretText(savedVaultConfig);
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(DeprecatedIntegrationTests.class)
  @Ignore("skipping the integration test")
  public void test_CreateSecretText_WithInvalidPath_shouldFail() {
    // Create the first default vault config
    String vaultConfigId = createVaultConfig(vaultConfig);
    VaultConfig savedVaultConfig = wingsPersistence.get(VaultConfig.class, vaultConfigId);
    assertThat(savedVaultConfig).isNotNull();

    try {
      createSecretText("FooBarSecret", null, "foo/bar/InvalidSecretPath");
      fail("Expected to fail when creating a secret text pointing to an invalid Vault path.");
    } catch (Exception e) {
      // Exception expected.
    } finally {
      // Clean up.
      deleteVaultConfig(vaultConfigId);
    }
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(DeprecatedIntegrationTests.class)
  @Ignore("skipping the integration test")
  public void test_UpdateSecretText_WithInvalidPath_shouldFail() {
    String vaultConfigId = createVaultConfig(vaultConfig);
    VaultConfig savedVaultConfig = wingsPersistence.get(VaultConfig.class, vaultConfigId);
    assertThat(savedVaultConfig).isNotNull();

    String secretValue = "MySecretValue";
    String secretUuid1 = null;
    String secretUuid2 = null;
    try {
      // This will create one secret at path 'harness/SECRET_TEXT/FooSecret".
      secretUuid1 = createSecretText("FooSecret", secretValue, null);
      // Second secret will refer the first secret by relative path, as the default root is "/harness".
      secretUuid2 = createSecretText("BarSecret", null, "SECRET_TEXT/FooSecret");
      updateSecretText(secretUuid2, "BarSecret", null, "foo/bar/InvalidSecretPath");
      fail("Expected to fail when updating a secret text pointing to an invalid Vault path.");
    } catch (Exception e) {
      // Exception is expected here.
    } finally {
      if (secretUuid1 != null) {
        deleteSecretText(secretUuid1);
      }
      if (secretUuid2 != null) {
        deleteSecretText(secretUuid2);
      }
      // Clean up.
      deleteVaultConfig(vaultConfigId);
    }
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(DeprecatedIntegrationTests.class)
  @Ignore("skipping the integration test")
  public void test_CreateSecretText_withInvalidPathReference_shouldFail() {
    String vaultConfigId = createVaultConfig(vaultConfig);
    VaultConfig savedVaultConfig = wingsPersistence.get(VaultConfig.class, vaultConfigId);
    assertThat(savedVaultConfig).isNotNull();

    String secretName = "MySecret";
    String secretName2 = "AbsolutePathSecret";
    String pathPrefix = isEmpty(savedVaultConfig.getBasePath()) ? "/harness" : savedVaultConfig.getBasePath();
    String absoluteSecretPathWithNoPound = pathPrefix + "/SECRET_TEXT/" + secretName + "/FooSecret";

    try {
      testCreateSecretText(savedVaultConfig, secretName, secretName2, absoluteSecretPathWithNoPound);
      fail("Saved with secret path doesn't contain # should fail");
    } catch (Exception e) {
      // Exception is expected.
    } finally {
      deleteVaultConfig(vaultConfigId);
    }
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(DeprecatedIntegrationTests.class)
  @Ignore("skipping the integration test")
  public void test_CreateSecretText_WithValidPath_shouldSucceed() {
    testCreateSecretText(vaultConfig);
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(DeprecatedIntegrationTests.class)
  @Ignore("skipping the integration test")
  public void test_importSecrets_fromCSV_shouldSucceed() {
    importSecretTextsFromCsv("./encryption/secrets.csv");
    verifySecretTextExists("secret1");
    verifySecretTextExists("secret3");
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(DeprecatedIntegrationTests.class)
  @Ignore("skipping the integration test")
  public void test_listSettingAttributes_shoudlSucceed() throws IllegalAccessException {
    List<SettingAttribute> settingAttributes = listSettingAttributes();
    for (SettingAttribute settingAttribute : settingAttributes) {
      SettingValue settingValue = settingAttribute.getValue();
      if (settingValue instanceof GcpConfig) {
        // For some reason GcpConfig is not seting the full encrypted value back. Skip it for now.
        continue;
      }
      List<Field> encryptedFields = settingValue.getEncryptedFields();
      for (Field field : encryptedFields) {
        field.setAccessible(true);
        char[] secrets = (char[]) field.get(settingValue);
        assertThat(SecretManager.ENCRYPTED_FIELD_MASK.toCharArray()).isEqualTo(secrets);
      }
    }
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(DeprecatedIntegrationTests.class)
  @Ignore("skipping the integration test")
  public void test_CreateSecretText_vaultWithBasePath_validPath_shouldSucceed() {
    testCreateSecretText(vaultConfigWithBasePath);
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(DeprecatedIntegrationTests.class)
  @Ignore("skipping the integration test")
  public void test_CreateSecretText_vaultWithBasePath2_validPath_shouldSucceed() {
    testCreateSecretText(vaultConfigWithBasePath2);
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(DeprecatedIntegrationTests.class)
  @Ignore("skipping the integration test")
  public void test_CreateSecretText_vaultWithBasePath3_validPath_shouldSucceed() {
    testCreateSecretText(vaultConfigWithBasePath3);
  }

  private void testCreateSecretText(VaultConfig vaultconfig) {
    String vaultConfigId = createVaultConfig(vaultconfig);
    VaultConfig savedVaultConfig = wingsPersistence.get(VaultConfig.class, vaultConfigId);
    assertThat(savedVaultConfig).isNotNull();

    String secretName = "FooSecret";
    String secretName2 = "AbsolutePathSecret";
    String pathPrefix = isEmpty(savedVaultConfig.getBasePath()) ? "/harness" : savedVaultConfig.getBasePath();
    String absoluteSecretPath = pathPrefix + "/SECRET_TEXT/" + secretName + "#value";

    try {
      testCreateSecretText(savedVaultConfig, secretName, secretName2, absoluteSecretPath);
    } finally {
      deleteVaultConfig(vaultConfigId);
    }
  }

  private void testCreateSecretText(
      VaultConfig savedVaultConfig, String secretName, String secretName2, String absoluteSecretPath) {
    String secretValue = "MySecretValue";
    String secretUuid1 = null;
    String secretUuid2 = null;
    try {
      // This will create one secret at path 'harness/SECRET_TEXT/FooSecret".
      secretUuid1 = createSecretText(secretName, secretValue, null);
      verifySecret(secretUuid1, secretName, secretValue, savedVaultConfig);

      // Second secret will refer the first secret by absolute path of format "/foo/bar/FooSecret#value'.
      secretUuid2 = createSecretText(secretName2, null, absoluteSecretPath);
      verifySecret(secretUuid2, secretName2, secretValue, savedVaultConfig);
      verifyVaultChangeLog(secretUuid2);

    } finally {
      if (secretUuid1 != null) {
        deleteSecretText(secretUuid1);
      }
      if (secretUuid2 != null) {
        deleteSecretText(secretUuid2);
      }
    }
  }
}
