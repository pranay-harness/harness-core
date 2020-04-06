package software.wings.security.encryption.migration.secretparents.migrators;

import static io.harness.rule.OwnerRule.UTKARSH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.data.structure.UUIDGenerator;
import io.harness.encryption.EncryptionReflectUtils;
import io.harness.reflection.ReflectionUtils;
import io.harness.rule.Owner;
import io.harness.security.encryption.EncryptionType;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;
import software.wings.beans.Account;
import software.wings.beans.AccountType;
import software.wings.beans.GcpKmsConfig;
import software.wings.beans.KmsConfig;
import software.wings.beans.KmsConfig.KmsConfigKeys;
import software.wings.beans.SecretManagerConfig;
import software.wings.dl.WingsPersistence;
import software.wings.security.encryption.EncryptedData;
import software.wings.security.encryption.EncryptedDataParent;
import software.wings.settings.SettingValue.SettingVariableTypes;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class SecretManagerConfigMigratorTest extends WingsBaseTest {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private SecretManagerConfigMigrator secretManagerConfigMigrator;
  private KmsConfig kmsConfig;
  private GcpKmsConfig gcpKmsConfig;

  @Before
  public void setup() {
    initMocks(this);
    Account account = getAccount(AccountType.PAID);
    account.setUuid(wingsPersistence.save(account));

    EncryptedData encryptedData = EncryptedData.builder()
                                      .encryptionKey("plainTextKey")
                                      .encryptedValue("encryptedValue".toCharArray())
                                      .encryptionType(EncryptionType.LOCAL)
                                      .type(SettingVariableTypes.GCP_KMS)
                                      .kmsId(UUIDGenerator.generateUuid())
                                      .enabled(true)
                                      .accountId(account.getUuid())
                                      .build();

    gcpKmsConfig = getGcpKmsConfig();
    gcpKmsConfig.setCredentials(wingsPersistence.save(encryptedData).toCharArray());
    gcpKmsConfig.setUuid(wingsPersistence.save(gcpKmsConfig));

    encryptedData.setUuid(null);
    encryptedData.setType(SettingVariableTypes.KMS);
    kmsConfig = getKmsConfig();
    kmsConfig.setKmsArn(wingsPersistence.save(encryptedData));
    kmsConfig.setUuid(wingsPersistence.save(kmsConfig));
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testGetParents_shouldPass() {
    Set<String> parentIds = new HashSet<>();
    parentIds.add(gcpKmsConfig.getUuid());
    parentIds.add(UUIDGenerator.generateUuid());
    List<SecretManagerConfig> secretManagerConfigs = secretManagerConfigMigrator.getParents(parentIds);
    assertThat(secretManagerConfigs).hasSize(1);
    assertThat(secretManagerConfigs.get(0)).isEqualTo(gcpKmsConfig);
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testBuildEncryptedDataParents_shouldPass() {
    List<SecretManagerConfig> secretManagerConfigs = new ArrayList<>();
    secretManagerConfigs.add(gcpKmsConfig);
    secretManagerConfigs.add(kmsConfig);

    List<EncryptedDataParent> expectedDataParents = new ArrayList<>();

    String encryptedFieldName = EncryptionReflectUtils.getEncryptedFieldTag(
        EncryptionReflectUtils.getEncryptedFields(GcpKmsConfig.class).get(0));
    expectedDataParents.add(new EncryptedDataParent(gcpKmsConfig.getUuid(),
        SettingVariableTypes.valueOf(gcpKmsConfig.getEncryptionType().name()), encryptedFieldName));

    encryptedFieldName = EncryptionReflectUtils.getEncryptedFieldTag(
        ReflectionUtils.getFieldByName(KmsConfig.class, KmsConfigKeys.kmsArn));
    expectedDataParents.add(new EncryptedDataParent(
        kmsConfig.getUuid(), SettingVariableTypes.valueOf(kmsConfig.getEncryptionType().name()), encryptedFieldName));

    Optional<EncryptedDataParent> parent = secretManagerConfigMigrator.buildEncryptedDataParent(
        String.valueOf(gcpKmsConfig.getCredentials()), secretManagerConfigs.get(0));
    assertThat(parent.isPresent()).isTrue();
    assertThat(parent.get()).isEqualTo(expectedDataParents.get(0));

    parent = secretManagerConfigMigrator.buildEncryptedDataParent(
        String.valueOf(gcpKmsConfig.getCredentials()), secretManagerConfigs.get(1));
    assertThat(parent.isPresent()).isFalse();

    parent = secretManagerConfigMigrator.buildEncryptedDataParent(kmsConfig.getKmsArn(), secretManagerConfigs.get(0));
    assertThat(parent.isPresent()).isFalse();

    parent = secretManagerConfigMigrator.buildEncryptedDataParent(kmsConfig.getKmsArn(), secretManagerConfigs.get(1));
    assertThat(parent.isPresent()).isTrue();
    assertThat(parent.get()).isEqualTo(expectedDataParents.get(1));
  }
}
