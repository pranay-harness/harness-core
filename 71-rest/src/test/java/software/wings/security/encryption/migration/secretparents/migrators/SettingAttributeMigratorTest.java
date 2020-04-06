package software.wings.security.encryption.migration.secretparents.migrators;

import static io.harness.rule.OwnerRule.UTKARSH;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.data.structure.UUIDGenerator;
import io.harness.encryption.EncryptionReflectUtils;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;
import software.wings.beans.Account;
import software.wings.beans.AccountType;
import software.wings.beans.AppDynamicsConfig;
import software.wings.beans.SettingAttribute;
import software.wings.dl.WingsPersistence;
import software.wings.security.encryption.EncryptedData;
import software.wings.security.encryption.EncryptedDataParent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class SettingAttributeMigratorTest extends WingsBaseTest {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private SettingAttributeMigrator settingAttributeMigrator;
  private Account account;
  private SettingAttribute settingAttribute;
  private EncryptedData encryptedData;

  @Before
  public void setup() {
    account = getAccount(AccountType.PAID);
    account.setUuid(wingsPersistence.save(account));
    String password = "password";
    AppDynamicsConfig appDynamicsConfig = getAppDynamicsConfig(account.getUuid(), password);
    settingAttribute = getSettingAttribute(appDynamicsConfig);
    String savedAttributeId = wingsPersistence.save(settingAttribute);
    settingAttribute = wingsPersistence.get(SettingAttribute.class, savedAttributeId);
    appDynamicsConfig = (AppDynamicsConfig) settingAttribute.getValue();
    encryptedData = wingsPersistence.get(EncryptedData.class, appDynamicsConfig.getEncryptedPassword());
    assertThat(encryptedData).isNotNull();
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testGetParents_shouldPass() {
    Set<String> parentIds = new HashSet<>();
    parentIds.add(settingAttribute.getUuid());
    parentIds.add(UUIDGenerator.generateUuid());
    List<SettingAttribute> settingAttributeList = settingAttributeMigrator.getParents(parentIds);
    assertThat(settingAttributeList).hasSize(1);
    assertThat(settingAttributeList.get(0)).isEqualTo(settingAttribute);
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testBuildEncryptedDataParents_shouldPass() {
    AppDynamicsConfig appDynamicsConfig = getAppDynamicsConfig(account.getUuid(), "password");
    SettingAttribute altSettingAttribute = getSettingAttribute(appDynamicsConfig);
    String savedAttributeId = wingsPersistence.save(altSettingAttribute);
    altSettingAttribute = wingsPersistence.get(SettingAttribute.class, savedAttributeId);
    EncryptedData altEncryptedData =
        wingsPersistence.get(EncryptedData.class, appDynamicsConfig.getEncryptedPassword());
    assertThat(altEncryptedData).isNotNull();

    List<SettingAttribute> settingAttributes = new ArrayList<>();
    settingAttributes.add(settingAttribute);
    settingAttributes.add(altSettingAttribute);

    String encryptedFieldName =
        EncryptionReflectUtils.getEncryptedFieldTag(appDynamicsConfig.getEncryptedFields().get(0));
    List<EncryptedDataParent> expectedDataParents = new ArrayList<>();
    expectedDataParents.add(
        new EncryptedDataParent(settingAttribute.getUuid(), appDynamicsConfig.getSettingType(), encryptedFieldName));
    expectedDataParents.add(
        new EncryptedDataParent(altSettingAttribute.getUuid(), appDynamicsConfig.getSettingType(), encryptedFieldName));

    Optional<EncryptedDataParent> parent =
        settingAttributeMigrator.buildEncryptedDataParent(encryptedData.getUuid(), settingAttributes.get(0));
    assertThat(parent.isPresent()).isTrue();
    assertThat(parent.get()).isEqualTo(expectedDataParents.get(0));

    parent = settingAttributeMigrator.buildEncryptedDataParent(encryptedData.getUuid(), settingAttributes.get(1));
    assertThat(parent.isPresent()).isFalse();

    parent = settingAttributeMigrator.buildEncryptedDataParent(altEncryptedData.getUuid(), settingAttributes.get(0));
    assertThat(parent.isPresent()).isFalse();

    parent = settingAttributeMigrator.buildEncryptedDataParent(altEncryptedData.getUuid(), settingAttributes.get(1));
    assertThat(parent.isPresent()).isTrue();
    assertThat(parent.get()).isEqualTo(expectedDataParents.get(1));
  }
}
