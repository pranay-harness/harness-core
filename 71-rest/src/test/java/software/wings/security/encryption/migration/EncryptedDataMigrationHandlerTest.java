package software.wings.security.encryption.migration;

import static io.harness.rule.OwnerRule.UTKARSH;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.data.structure.UUIDGenerator;
import io.harness.rule.Owner;
import io.harness.security.encryption.EncryptionType;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.dl.WingsPersistence;
import software.wings.security.encryption.EncryptedData;
import software.wings.security.encryption.migration.secretparents.AzureToAzureVaultMigrator;
import software.wings.security.encryption.migration.secretparents.EncryptedDataMigrator;
import software.wings.settings.SettingValue;

import java.util.Optional;

public class EncryptedDataMigrationHandlerTest extends WingsBaseTest {
  @Mock private EncryptedDataMigrator encryptedDataMigrator;
  @Mock private AzureToAzureVaultMigrator azureToAzureVaultMigrator;
  @Inject private WingsPersistence wingsPersistence;
  @Inject @InjectMocks private EncryptedDataMigrationHandler encryptedDataMigrationHandler;
  private EncryptedData encryptedData;

  @Before
  public void setup() {
    initMocks(this);

    encryptedData = EncryptedData.builder()
                        .encryptionKey("plainTextKey")
                        .encryptedValue("encryptedValue".toCharArray())
                        .encryptionType(EncryptionType.KMS)
                        .type(SettingValue.SettingVariableTypes.AZURE_VAULT)
                        .kmsId(UUIDGenerator.generateUuid())
                        .enabled(true)
                        .accountId(UUIDGenerator.generateUuid())
                        .build();

    encryptedData.setUuid(wingsPersistence.save(encryptedData));
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testHandle_GoThroughAll_Successful() {
    when(azureToAzureVaultMigrator.shouldConvertToAzureVaultType(encryptedData)).thenReturn(true);
    when(azureToAzureVaultMigrator.convertToAzureVaultSettingType(encryptedData))
        .thenReturn(Optional.of(encryptedData));
    when(encryptedDataMigrator.canMigrate(encryptedData)).thenReturn(true);
    when(encryptedDataMigrator.shouldMigrate(encryptedData)).thenReturn(true);
    when(encryptedDataMigrator.migrateEncryptedDataParents(encryptedData)).thenReturn(Optional.of(encryptedData));

    encryptedDataMigrationHandler.handle(encryptedData);

    verify(azureToAzureVaultMigrator, times(1)).shouldConvertToAzureVaultType(encryptedData);
    verify(azureToAzureVaultMigrator, times(1)).convertToAzureVaultSettingType(encryptedData);
    verify(encryptedDataMigrator, times(1)).canMigrate(encryptedData);
    verify(encryptedDataMigrator, times(1)).shouldMigrate(encryptedData);
    verify(encryptedDataMigrator, times(1)).migrateEncryptedDataParents(encryptedData);
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testHandle_OnlyMigrate_Successful() {
    when(azureToAzureVaultMigrator.shouldConvertToAzureVaultType(encryptedData)).thenReturn(false);
    when(encryptedDataMigrator.canMigrate(encryptedData)).thenReturn(true);
    when(encryptedDataMigrator.shouldMigrate(encryptedData)).thenReturn(true);
    when(encryptedDataMigrator.migrateEncryptedDataParents(encryptedData)).thenReturn(Optional.of(encryptedData));

    encryptedDataMigrationHandler.handle(encryptedData);

    verify(azureToAzureVaultMigrator, times(1)).shouldConvertToAzureVaultType(encryptedData);
    verify(azureToAzureVaultMigrator, times(0)).convertToAzureVaultSettingType(encryptedData);
    verify(encryptedDataMigrator, times(1)).canMigrate(encryptedData);
    verify(encryptedDataMigrator, times(1)).shouldMigrate(encryptedData);
    verify(encryptedDataMigrator, times(1)).migrateEncryptedDataParents(encryptedData);
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testHandle_AzureMigrate_Failure() {
    when(azureToAzureVaultMigrator.shouldConvertToAzureVaultType(encryptedData)).thenReturn(true);
    when(azureToAzureVaultMigrator.convertToAzureVaultSettingType(encryptedData)).thenReturn(Optional.empty());
    when(encryptedDataMigrator.canMigrate(encryptedData)).thenReturn(true);
    when(encryptedDataMigrator.shouldMigrate(encryptedData)).thenReturn(true);
    when(encryptedDataMigrator.migrateEncryptedDataParents(encryptedData)).thenReturn(Optional.of(encryptedData));

    encryptedDataMigrationHandler.handle(encryptedData);

    verify(azureToAzureVaultMigrator, times(3)).shouldConvertToAzureVaultType(encryptedData);
    verify(azureToAzureVaultMigrator, times(3)).convertToAzureVaultSettingType(encryptedData);
    verify(encryptedDataMigrator, times(0)).canMigrate(encryptedData);
    verify(encryptedDataMigrator, times(0)).shouldMigrate(encryptedData);
    verify(encryptedDataMigrator, times(0)).migrateEncryptedDataParents(encryptedData);
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testHandle_OnlyAzure_Successful() {
    when(azureToAzureVaultMigrator.shouldConvertToAzureVaultType(encryptedData)).thenReturn(true);
    when(azureToAzureVaultMigrator.convertToAzureVaultSettingType(encryptedData))
        .thenReturn(Optional.of(encryptedData));
    when(encryptedDataMigrator.canMigrate(encryptedData)).thenReturn(false);
    when(encryptedDataMigrator.shouldMigrate(encryptedData)).thenReturn(true);
    when(encryptedDataMigrator.migrateEncryptedDataParents(encryptedData)).thenReturn(Optional.of(encryptedData));

    encryptedDataMigrationHandler.handle(encryptedData);

    verify(azureToAzureVaultMigrator, times(1)).shouldConvertToAzureVaultType(encryptedData);
    verify(azureToAzureVaultMigrator, times(1)).convertToAzureVaultSettingType(encryptedData);
    verify(encryptedDataMigrator, times(1)).canMigrate(encryptedData);
    verify(encryptedDataMigrator, times(1)).shouldMigrate(encryptedData);
    verify(encryptedDataMigrator, times(0)).migrateEncryptedDataParents(encryptedData);
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testHandle_DoNothing_Successful() {
    when(azureToAzureVaultMigrator.shouldConvertToAzureVaultType(encryptedData)).thenReturn(false);
    when(encryptedDataMigrator.shouldMigrate(encryptedData)).thenReturn(false);

    encryptedDataMigrationHandler.handle(encryptedData);

    verify(azureToAzureVaultMigrator, times(1)).shouldConvertToAzureVaultType(encryptedData);
    verify(azureToAzureVaultMigrator, times(0)).convertToAzureVaultSettingType(encryptedData);
    verify(encryptedDataMigrator, times(1)).shouldMigrate(encryptedData);
    verify(encryptedDataMigrator, times(0)).canMigrate(encryptedData);
    verify(encryptedDataMigrator, times(0)).migrateEncryptedDataParents(encryptedData);
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testHandle_OnlyMigrate_Failure() {
    when(azureToAzureVaultMigrator.shouldConvertToAzureVaultType(encryptedData)).thenReturn(false);
    when(encryptedDataMigrator.shouldMigrate(encryptedData)).thenReturn(true);
    when(encryptedDataMigrator.canMigrate(encryptedData)).thenReturn(true);
    when(encryptedDataMigrator.migrateEncryptedDataParents(encryptedData)).thenReturn(Optional.empty());

    encryptedDataMigrationHandler.handle(encryptedData);

    verify(azureToAzureVaultMigrator, times(3)).shouldConvertToAzureVaultType(encryptedData);
    verify(azureToAzureVaultMigrator, times(0)).convertToAzureVaultSettingType(encryptedData);
    verify(encryptedDataMigrator, times(3)).shouldMigrate(encryptedData);
    verify(encryptedDataMigrator, times(3)).canMigrate(encryptedData);
    verify(encryptedDataMigrator, times(3)).migrateEncryptedDataParents(encryptedData);
  }
}
