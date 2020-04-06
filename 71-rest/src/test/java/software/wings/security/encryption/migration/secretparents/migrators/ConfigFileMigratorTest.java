package software.wings.security.encryption.migration.secretparents.migrators;

import static io.harness.rule.OwnerRule.UTKARSH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.data.structure.UUIDGenerator;
import io.harness.rule.Owner;
import io.harness.security.encryption.EncryptionType;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;
import software.wings.beans.Account;
import software.wings.beans.AccountType;
import software.wings.beans.ConfigFile;
import software.wings.beans.EntityType;
import software.wings.dl.WingsPersistence;
import software.wings.security.encryption.EncryptedData;
import software.wings.security.encryption.EncryptedDataParent;
import software.wings.settings.SettingValue.SettingVariableTypes;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class ConfigFileMigratorTest extends WingsBaseTest {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private ConfigFileMigrator configFileMigrator;
  private List<ConfigFile> configFiles;

  @Before
  public void setup() {
    initMocks(this);
    Account account = getAccount(AccountType.PAID);
    account.setUuid(wingsPersistence.save(account));
    configFiles = new ArrayList<>();

    EncryptedData encryptedData = EncryptedData.builder()
                                      .encryptionKey("plainTextKey")
                                      .encryptedValue(UUIDGenerator.generateUuid().toCharArray())
                                      .encryptionType(EncryptionType.LOCAL)
                                      .type(SettingVariableTypes.CONFIG_FILE)
                                      .kmsId(UUIDGenerator.generateUuid())
                                      .enabled(true)
                                      .accountId(account.getUuid())
                                      .name("xyz")
                                      .fileSize(200)
                                      .build();

    ConfigFile configFile = ConfigFile.builder()
                                .templateId(UUIDGenerator.generateUuid())
                                .envId(UUIDGenerator.generateUuid())
                                .entityType(EntityType.SERVICE)
                                .entityId(UUIDGenerator.generateUuid())
                                .description(UUIDGenerator.generateUuid())
                                .envIdVersionMapString(UUIDGenerator.generateUuid())
                                .setAsDefault(true)
                                .encryptedFileId(wingsPersistence.save(encryptedData))
                                .encrypted(true)
                                .build();

    configFile.setAccountId(account.getUuid());
    configFile.setName(UUIDGenerator.generateUuid());
    configFile.setFileName(UUIDGenerator.generateUuid());
    configFile.setAppId(UUIDGenerator.generateUuid());

    configFiles.add(wingsPersistence.get(ConfigFile.class, wingsPersistence.save(configFile)));

    encryptedData.setUuid(null);
    configFile.setUuid(null);
    configFile.setEncryptedFileId(wingsPersistence.save(encryptedData));
    configFiles.add(wingsPersistence.get(ConfigFile.class, wingsPersistence.save(configFile)));

    encryptedData.setUuid(null);
    configFile.setUuid(null);
    configFile.setEncrypted(false);
    configFile.setEncryptedFileId(null);
    configFile.setFileUuid(UUIDGenerator.generateUuid());

    configFiles.add(wingsPersistence.get(ConfigFile.class, wingsPersistence.save(configFile)));
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testGetParents_shouldPass() {
    Set<String> parentIds = new HashSet<>();
    parentIds.add(configFiles.get(0).getUuid());
    parentIds.add(configFiles.get(2).getUuid());
    parentIds.add(UUIDGenerator.generateUuid());
    List<ConfigFile> configFileList = configFileMigrator.getParents(parentIds);
    assertThat(configFileList).hasSize(1);
    assertThat(configFileList.get(0)).isEqualTo(configFiles.get(0));
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testBuildEncryptedDataParents_shouldPass() {
    String encryptedFieldName = SettingVariableTypes.CONFIG_FILE.toString();
    List<EncryptedDataParent> expectedDataParents = new ArrayList<>();
    expectedDataParents.add(
        new EncryptedDataParent(configFiles.get(0).getUuid(), SettingVariableTypes.CONFIG_FILE, encryptedFieldName));
    expectedDataParents.add(
        new EncryptedDataParent(configFiles.get(1).getUuid(), SettingVariableTypes.CONFIG_FILE, encryptedFieldName));

    Optional<EncryptedDataParent> parent =
        configFileMigrator.buildEncryptedDataParent(configFiles.get(0).getEncryptedFileId(), configFiles.get(0));
    assertThat(parent.isPresent()).isTrue();
    assertThat(parent.get()).isEqualTo(expectedDataParents.get(0));

    parent = configFileMigrator.buildEncryptedDataParent(configFiles.get(0).getEncryptedFileId(), configFiles.get(1));
    assertThat(parent.isPresent()).isFalse();

    parent = configFileMigrator.buildEncryptedDataParent(configFiles.get(1).getEncryptedFileId(), configFiles.get(0));
    assertThat(parent.isPresent()).isFalse();

    parent = configFileMigrator.buildEncryptedDataParent(configFiles.get(1).getEncryptedFileId(), configFiles.get(1));
    assertThat(parent.isPresent()).isTrue();
    assertThat(parent.get()).isEqualTo(expectedDataParents.get(1));
  }
}
