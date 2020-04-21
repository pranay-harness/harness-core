package software.wings.security.encryption.migration;

import static io.harness.rule.OwnerRule.UTKARSH;
import static io.harness.security.encryption.EncryptionType.KMS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.FeatureName.CONNECTORS_REF_SECRETS_MIGRATION;
import static software.wings.beans.FeatureName.SECRET_PARENTS_MIGRATED;
import static software.wings.beans.SettingAttribute.VALUE_TYPE_KEY;
import static software.wings.service.impl.SettingServiceHelper.ATTRIBUTES_USING_REFERENCES;
import static software.wings.settings.SettingValue.SettingVariableTypes.APP_DYNAMICS;
import static software.wings.settings.SettingValue.SettingVariableTypes.SECRET_TEXT;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.data.structure.UUIDGenerator;
import io.harness.iterator.PersistenceIteratorFactory;
import io.harness.mongo.iterator.MongoPersistenceIterator.MongoPersistenceIteratorBuilder;
import io.harness.rule.Owner;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mongodb.morphia.query.FieldEnd;
import org.mongodb.morphia.query.Query;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import software.wings.WingsBaseTest;
import software.wings.beans.Account;
import software.wings.beans.AccountType;
import software.wings.beans.AppDynamicsConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.SettingAttributeKeys;
import software.wings.dl.WingsPersistence;
import software.wings.security.encryption.EncryptedData;
import software.wings.service.intfc.UsageRestrictionsService;
import software.wings.settings.UsageRestrictions;

import java.util.Collections;

@RunWith(PowerMockRunner.class)
@PrepareForTest({PersistenceIteratorFactory.class})
@PowerMockIgnore({"javax.crypto.*", "javax.net.*"})
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SettingAttributesSecretsMigrationHandlerTest extends WingsBaseTest {
  @Inject WingsPersistence wingsPersistence;
  @Inject UsageRestrictionsService usageRestrictionsService;
  @Mock PersistenceIteratorFactory persistenceIteratorFactory;
  @Inject @InjectMocks SettingAttributesSecretsMigrationHandler settingAttributesSecretsMigrationHandler;
  private SettingAttribute settingAttribute;
  private EncryptedData encryptedData;

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testRegisterIterators_featureFlagEnabled() {
    featureFlagService.enableGlobally(SECRET_PARENTS_MIGRATED);
    ArgumentCaptor<MongoPersistenceIteratorBuilder> captor =
        ArgumentCaptor.forClass(MongoPersistenceIteratorBuilder.class);
    settingAttributesSecretsMigrationHandler.registerIterators();
    verify(persistenceIteratorFactory, times(1))
        .createPumpIteratorWithDedicatedThreadPool(any(), eq(SettingAttribute.class), captor.capture());
    MongoPersistenceIteratorBuilder mongoPersistenceIteratorBuilder = captor.getValue();
    assertThat(mongoPersistenceIteratorBuilder).isNotNull();
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testRegisterIterators_featureFlagDisabled() {
    ArgumentCaptor<MongoPersistenceIteratorBuilder> captor =
        ArgumentCaptor.forClass(MongoPersistenceIteratorBuilder.class);
    settingAttributesSecretsMigrationHandler.registerIterators();
    verify(persistenceIteratorFactory, times(0)).createPumpIteratorWithDedicatedThreadPool(any(), any(), any());
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testCreateQuery() {
    Query<SettingAttribute> query = mock(Query.class);
    FieldEnd fieldEnd = mock(FieldEnd.class);

    when(query.field(SettingAttributeKeys.secretsMigrated)).thenReturn(fieldEnd);
    when(fieldEnd.notIn(Collections.singleton(Boolean.TRUE))).thenReturn(query);
    when(query.field(VALUE_TYPE_KEY)).thenReturn(fieldEnd);
    when(fieldEnd.in(ATTRIBUTES_USING_REFERENCES)).thenReturn(query);

    settingAttributesSecretsMigrationHandler.createQuery(query);

    verify(query, times(2)).field(any());
    verify(fieldEnd, times(1)).notIn(any());
    verify(fieldEnd, times(1)).in(any());
  }

  private void createSettingAttribute() {
    Account account = getAccount(AccountType.PAID);
    String accountId = wingsPersistence.save(account);
    account.setUuid(accountId);
    UsageRestrictions usageRestrictions =
        usageRestrictionsService.getDefaultUsageRestrictions(account.getUuid(), "appId", "envId");
    encryptedData = EncryptedData.builder()
                        .name(UUIDGenerator.generateUuid())
                        .encryptionKey("plainTextKey")
                        .encryptedValue("encryptedValue".toCharArray())
                        .encryptionType(KMS)
                        .type(APP_DYNAMICS)
                        .kmsId(UUIDGenerator.generateUuid())
                        .enabled(true)
                        .accountId(accountId)
                        .build();

    String secretId = wingsPersistence.save(encryptedData);
    encryptedData.setUuid(secretId);
    AppDynamicsConfig appDynamicsConfig = getAppDynamicsConfig(accountId, null, secretId);
    settingAttribute = getSettingAttribute(appDynamicsConfig);
    settingAttribute.setName("testAttribute");
    settingAttribute.setUsageRestrictions(usageRestrictions);
    String settingAttributeId = wingsPersistence.save(settingAttribute);
    settingAttribute.setUuid(settingAttributeId);

    encryptedData = wingsPersistence.get(EncryptedData.class, secretId);
    settingAttribute = wingsPersistence.get(SettingAttribute.class, settingAttributeId);
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testHandle_shouldSucceed() {
    createSettingAttribute();
    featureFlagService.enableAccount(SECRET_PARENTS_MIGRATED, settingAttribute.getAccountId());
    featureFlagService.enableAccount(CONNECTORS_REF_SECRETS_MIGRATION, settingAttribute.getAccountId());
    settingAttributesSecretsMigrationHandler.handle(settingAttribute);
    SettingAttribute updatedSettingAttribute = wingsPersistence.get(SettingAttribute.class, settingAttribute.getUuid());
    assertThat(updatedSettingAttribute).isNotNull();
    assertThat(updatedSettingAttribute.isSecretsMigrated()).isTrue();

    EncryptedData updatedEncryptedData = wingsPersistence.get(EncryptedData.class, encryptedData.getUuid());
    assertThat(updatedEncryptedData).isNotNull();
    assertThat(updatedEncryptedData.getType()).isEqualTo(SECRET_TEXT);
    assertThat(updatedEncryptedData.getUsageRestrictions()).isEqualTo(settingAttribute.getUsageRestrictions());
    assertThat(updatedEncryptedData.getName()).isEqualTo("testAttribute_APP_DYNAMICS_password");
    assertThat(updatedEncryptedData.getParents()).hasSize(1);
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testHandle_shouldFail_FeatureFlagDisabled() {
    createSettingAttribute();
    featureFlagService.enableAccount(SECRET_PARENTS_MIGRATED, settingAttribute.getAccountId());
    settingAttributesSecretsMigrationHandler.handle(settingAttribute);
    SettingAttribute updatedSettingAttribute = wingsPersistence.get(SettingAttribute.class, settingAttribute.getUuid());
    assertThat(updatedSettingAttribute).isNotNull();
    assertThat(updatedSettingAttribute.isSecretsMigrated()).isFalse();

    EncryptedData updatedEncryptedData = wingsPersistence.get(EncryptedData.class, encryptedData.getUuid());
    assertThat(updatedEncryptedData).isNotNull();
    assertThat(updatedEncryptedData.getType()).isEqualTo(APP_DYNAMICS);
    assertThat(updatedEncryptedData.getUsageRestrictions()).isNull();
    assertThat(updatedEncryptedData.getName()).isEqualTo(encryptedData.getName());
    assertThat(updatedEncryptedData.getParents()).hasSize(1);
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testHandle_shouldPass_secretAlreadyMigrated() {
    createSettingAttribute();
    featureFlagService.enableAccount(SECRET_PARENTS_MIGRATED, settingAttribute.getAccountId());
    featureFlagService.enableAccount(CONNECTORS_REF_SECRETS_MIGRATION, settingAttribute.getAccountId());
    encryptedData.setType(SECRET_TEXT);
    wingsPersistence.save(encryptedData);

    settingAttributesSecretsMigrationHandler.handle(settingAttribute);
    SettingAttribute updatedSettingAttribute = wingsPersistence.get(SettingAttribute.class, settingAttribute.getUuid());
    assertThat(updatedSettingAttribute).isNotNull();
    assertThat(updatedSettingAttribute.isSecretsMigrated()).isTrue();

    EncryptedData updatedEncryptedData = wingsPersistence.get(EncryptedData.class, encryptedData.getUuid());
    assertThat(updatedEncryptedData).isNotNull();
    assertThat(updatedEncryptedData.getType()).isEqualTo(SECRET_TEXT);
    assertThat(updatedEncryptedData.getUsageRestrictions()).isNull();
    assertThat(updatedEncryptedData.getName()).isEqualTo(encryptedData.getName());
    assertThat(updatedEncryptedData.getParents()).hasSize(1);
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testHandle_shouldPass_secretNotFound() {
    createSettingAttribute();
    featureFlagService.enableAccount(SECRET_PARENTS_MIGRATED, settingAttribute.getAccountId());
    featureFlagService.enableAccount(CONNECTORS_REF_SECRETS_MIGRATION, settingAttribute.getAccountId());
    ((AppDynamicsConfig) settingAttribute.getValue()).setEncryptedPassword(UUIDGenerator.generateUuid());
    wingsPersistence.save(settingAttribute);

    settingAttributesSecretsMigrationHandler.handle(settingAttribute);
    SettingAttribute updatedSettingAttribute = wingsPersistence.get(SettingAttribute.class, settingAttribute.getUuid());
    assertThat(updatedSettingAttribute).isNotNull();
    assertThat(updatedSettingAttribute.isSecretsMigrated()).isTrue();

    EncryptedData updatedEncryptedData = wingsPersistence.get(EncryptedData.class, encryptedData.getUuid());
    assertThat(updatedEncryptedData).isNull();
  }
}
