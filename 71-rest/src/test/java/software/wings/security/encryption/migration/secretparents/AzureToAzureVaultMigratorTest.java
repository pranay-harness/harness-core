package software.wings.security.encryption.migration.secretparents;

import static io.harness.rule.OwnerRule.UTKARSH;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.settings.SettingValue.SettingVariableTypes.AZURE;
import static software.wings.settings.SettingValue.SettingVariableTypes.AZURE_VAULT;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.data.structure.UUIDGenerator;
import io.harness.rule.Owner;
import io.harness.security.encryption.EncryptionType;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;
import software.wings.beans.AzureVaultConfig;
import software.wings.dl.WingsPersistence;
import software.wings.security.encryption.EncryptedData;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class AzureToAzureVaultMigratorTest extends WingsBaseTest {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private AzureToAzureVaultMigrator azureToAzureVaultMigrator;
  private EncryptedData encryptedData;

  @Before
  public void setup() {
    encryptedData = EncryptedData.builder()
                        .encryptionKey("plainTextKey")
                        .encryptedValue("encryptedValue".toCharArray())
                        .encryptionType(EncryptionType.KMS)
                        .type(AZURE)
                        .kmsId(UUIDGenerator.generateUuid())
                        .enabled(true)
                        .accountId(UUIDGenerator.generateUuid())
                        .build();

    encryptedData.setUuid(wingsPersistence.save(encryptedData));

    AzureVaultConfig azureVaultConfig = AzureVaultConfig.builder()
                                            .vaultName("Azure Vault")
                                            .clientId(UUIDGenerator.generateUuid())
                                            .subscription(UUIDGenerator.generateUuid())
                                            .tenantId(UUIDGenerator.generateUuid())
                                            .secretKey(encryptedData.getUuid())
                                            .name("Azure Vault")
                                            .build();
    azureVaultConfig.setAccountId(UUIDGenerator.generateUuid());
    azureVaultConfig.setEncryptionType(EncryptionType.AZURE_VAULT);

    azureVaultConfig.setUuid(wingsPersistence.save(azureVaultConfig));
    Set<String> parentIds = new HashSet<>();
    parentIds.add(azureVaultConfig.getUuid());
    parentIds.add(UUIDGenerator.generateUuid());
    encryptedData.setParentIds(parentIds);
    encryptedData = wingsPersistence.get(EncryptedData.class, wingsPersistence.save(encryptedData));
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testShouldConvertToAzureVaultType_returnTrue() {
    boolean result = azureToAzureVaultMigrator.shouldConvertToAzureVaultType(encryptedData);
    assertThat(result).isTrue();
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testShouldConvertToAzureVaultType_returnFalse_Condition1() {
    encryptedData.setType(AZURE_VAULT);
    boolean result = azureToAzureVaultMigrator.shouldConvertToAzureVaultType(encryptedData);
    assertThat(result).isFalse();
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testShouldConvertToAzureVaultType_returnFalse_Condition2() {
    Set<String> parentIds = new HashSet<>();
    parentIds.add(UUIDGenerator.generateUuid());
    encryptedData.setParentIds(parentIds);
    boolean result = azureToAzureVaultMigrator.shouldConvertToAzureVaultType(encryptedData);
    assertThat(result).isFalse();
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testConvertToAzureVaultSettingType_shouldPass() {
    Optional<EncryptedData> modifiedEncryptedData =
        azureToAzureVaultMigrator.convertToAzureVaultSettingType(encryptedData);
    assertThat(modifiedEncryptedData.isPresent()).isTrue();
    assertThat(modifiedEncryptedData.get().getType()).isEqualTo(AZURE_VAULT);
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testConvertToAzureVaultSettingType_shouldFail() {
    encryptedData.setType(AZURE_VAULT);
    encryptedData = wingsPersistence.get(EncryptedData.class, wingsPersistence.save(encryptedData));
    Optional<EncryptedData> modifiedEncryptedData =
        azureToAzureVaultMigrator.convertToAzureVaultSettingType(encryptedData);
    assertThat(modifiedEncryptedData.isPresent()).isFalse();
  }
}
