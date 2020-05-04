package software.wings.security.encryption.migration;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.persistence.HQuery.excludeAuthority;
import static io.harness.rule.OwnerRule.RAGHU;
import static io.harness.rule.OwnerRule.UTKARSH;
import static io.harness.security.encryption.EncryptionType.KMS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.FeatureName.CONNECTORS_REF_SECRETS_MIGRATION;
import static software.wings.beans.FeatureName.SECRET_PARENTS_MIGRATED;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.beans.SettingAttribute.VALUE_TYPE_KEY;
import static software.wings.service.impl.SettingServiceHelper.ATTRIBUTES_USING_REFERENCES;
import static software.wings.settings.SettingValue.SettingVariableTypes.APP_DYNAMICS;
import static software.wings.settings.SettingValue.SettingVariableTypes.SECRET_TEXT;

import com.google.common.collect.Sets;
import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
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
import software.wings.beans.APMValidateCollectorConfig;
import software.wings.beans.APMVerificationConfig;
import software.wings.beans.APMVerificationConfig.KeyValues;
import software.wings.beans.Account;
import software.wings.beans.AccountType;
import software.wings.beans.AppDynamicsConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.SettingCategory;
import software.wings.dl.WingsPersistence;
import software.wings.security.encryption.EncryptedData;
import software.wings.security.encryption.EncryptedDataParent;
import software.wings.service.impl.SettingValidationService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.UsageRestrictionsService;
import software.wings.service.intfc.newrelic.NewRelicService;
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.settings.UsageRestrictions;

import java.util.ArrayList;
import java.util.List;

@RunWith(PowerMockRunner.class)
@PrepareForTest({PersistenceIteratorFactory.class})
@PowerMockIgnore({"javax.security.*", "javax.crypto.*", "javax.net.*"})
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SettingAttributesSecretsMigrationHandlerTest extends WingsBaseTest {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private UsageRestrictionsService usageRestrictionsService;
  @Mock private PersistenceIteratorFactory persistenceIteratorFactory;
  @Mock private NewRelicService newRelicService;
  @Inject @InjectMocks private SettingAttributesSecretsMigrationHandler settingAttributesSecretsMigrationHandler;
  @Inject @InjectMocks private SettingValidationService settingValidationService;
  @Inject @InjectMocks private SettingsService settingsService;
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

    when(query.field(VALUE_TYPE_KEY)).thenReturn(fieldEnd);
    when(fieldEnd.in(ATTRIBUTES_USING_REFERENCES)).thenReturn(query);

    settingAttributesSecretsMigrationHandler.createQuery(query);

    verify(query, times(1)).field(any());
    verify(fieldEnd, times(1)).in(any());
  }

  private void createSettingAttribute() {
    Account account = getAccount(AccountType.PAID);
    String accountId = wingsPersistence.save(account);
    account.setUuid(accountId);
    UsageRestrictions usageRestrictions =
        usageRestrictionsService.getDefaultUsageRestrictions(account.getUuid(), "appId", "envId");
    encryptedData = EncryptedData.builder()
                        .name(generateUuid())
                        .encryptionKey("plainTextKey")
                        .encryptedValue("encryptedValue".toCharArray())
                        .encryptionType(KMS)
                        .type(APP_DYNAMICS)
                        .kmsId(generateUuid())
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
    ((AppDynamicsConfig) settingAttribute.getValue()).setEncryptedPassword(generateUuid());
    wingsPersistence.save(settingAttribute);

    settingAttributesSecretsMigrationHandler.handle(settingAttribute);
    SettingAttribute updatedSettingAttribute = wingsPersistence.get(SettingAttribute.class, settingAttribute.getUuid());
    assertThat(updatedSettingAttribute).isNotNull();
    assertThat(updatedSettingAttribute.isSecretsMigrated()).isTrue();

    EncryptedData updatedEncryptedData = wingsPersistence.get(EncryptedData.class, encryptedData.getUuid());
    assertThat(updatedEncryptedData).isNull();
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testHandle_ApmConnector() {
    doNothing()
        .when(newRelicService)
        .validateAPMConfig(any(SettingAttribute.class), any(APMValidateCollectorConfig.class));
    String accountId = generateUuid();
    String appId = generateUuid();
    APMVerificationConfig apmVerificationConfig = new APMVerificationConfig();
    apmVerificationConfig.setSettingType(SettingVariableTypes.APM_VERIFICATION);
    apmVerificationConfig.setUrl("https://apm-example.com/");
    apmVerificationConfig.setValidationUrl(generateUuid());
    List<KeyValues> headers = new ArrayList<>();
    headers.add(KeyValues.builder().key("key1").value(generateUuid()).encrypted(true).build());
    headers.add(KeyValues.builder().key("key2").value(generateUuid()).encrypted(true).build());
    headers.add(KeyValues.builder().key("api_key_plain").value("123").encrypted(false).build());

    List<KeyValues> options = new ArrayList<>();
    options.add(KeyValues.builder().key("key3").value(generateUuid()).encrypted(true).build());
    options.add(KeyValues.builder().key("key4").value(generateUuid()).encrypted(true).build());
    options.add(KeyValues.builder().key("option_key_plain").value("321").encrypted(false).build());

    apmVerificationConfig.setHeadersList(headers);
    apmVerificationConfig.setOptionsList(options);
    apmVerificationConfig.setAccountId(accountId);

    final SettingAttribute settingAttribute = aSettingAttribute()
                                                  .withAccountId(accountId)
                                                  .withName(generateUuid())
                                                  .withCategory(SettingCategory.CONNECTOR)
                                                  .withAppId(appId)
                                                  .withValue(apmVerificationConfig)
                                                  .build();

    settingsService.saveWithPruning(settingAttribute, appId, accountId);
    final String settingAttributeUuid = settingAttribute.getUuid();

    List<EncryptedData> encryptedDataList =
        wingsPersistence.createQuery(EncryptedData.class, excludeAuthority).asList();
    assertThat(encryptedDataList.size()).isEqualTo(4);
    encryptedDataList.forEach(encryptedData -> {
      assertThat(encryptedData.getType()).isEqualTo(SettingVariableTypes.APM_VERIFICATION);
      assertThat(encryptedData.getParents()).isEmpty();
    });

    SettingAttribute savedSettingAttribute = wingsPersistence.get(SettingAttribute.class, settingAttributeUuid);
    APMVerificationConfig savedApmVerificationConfig = (APMVerificationConfig) savedSettingAttribute.getValue();

    List<KeyValues> headersList = savedApmVerificationConfig.getHeadersList();
    KeyValues keyValues = headersList.get(0);
    assertThat(keyValues.getKey()).isEqualTo("key1");
    assertThat(keyValues.getValue()).isEqualTo(APMVerificationConfig.MASKED_STRING);
    assertThat(keyValues.getEncryptedValue()).isNotEmpty();
    assertThat(keyValues.isEncrypted()).isTrue();

    keyValues = headersList.get(1);
    assertThat(keyValues.getKey()).isEqualTo("key2");
    assertThat(keyValues.getValue()).isEqualTo(APMVerificationConfig.MASKED_STRING);
    assertThat(keyValues.getEncryptedValue()).isNotEmpty();
    assertThat(keyValues.isEncrypted()).isTrue();

    keyValues = headersList.get(2);
    assertThat(keyValues.getKey()).isEqualTo("api_key_plain");
    assertThat(keyValues.getValue()).isEqualTo("123");
    assertThat(keyValues.getEncryptedValue()).isNull();
    assertThat(keyValues.isEncrypted()).isFalse();

    List<KeyValues> optionsList = savedApmVerificationConfig.getOptionsList();
    keyValues = optionsList.get(0);
    assertThat(keyValues.getKey()).isEqualTo("key3");
    assertThat(keyValues.getValue()).isEqualTo(APMVerificationConfig.MASKED_STRING);
    assertThat(keyValues.getEncryptedValue()).isNotEmpty();
    assertThat(keyValues.isEncrypted()).isTrue();

    keyValues = optionsList.get(1);
    assertThat(keyValues.getKey()).isEqualTo("key4");
    assertThat(keyValues.getValue()).isEqualTo(APMVerificationConfig.MASKED_STRING);
    assertThat(keyValues.getEncryptedValue()).isNotEmpty();
    assertThat(keyValues.isEncrypted()).isTrue();

    keyValues = optionsList.get(2);
    assertThat(keyValues.getKey()).isEqualTo("option_key_plain");
    assertThat(keyValues.getValue()).isEqualTo("321");
    assertThat(keyValues.getEncryptedValue()).isNull();
    assertThat(keyValues.isEncrypted()).isFalse();

    featureFlagService.enableAccount(CONNECTORS_REF_SECRETS_MIGRATION, settingAttribute.getAccountId());
    featureFlagService.enableAccount(SECRET_PARENTS_MIGRATED, settingAttribute.getAccountId());
    settingAttributesSecretsMigrationHandler.handle(savedSettingAttribute);

    encryptedDataList = wingsPersistence.createQuery(EncryptedData.class, excludeAuthority).asList();
    assertThat(encryptedDataList.size()).isEqualTo(4);
    for (int i = 0; i < encryptedDataList.size(); i++) {
      assertThat(encryptedDataList.get(i).getType()).isEqualTo(SettingVariableTypes.SECRET_TEXT);
      assertThat(encryptedDataList.get(i).getParents())
          .isEqualTo(Sets.newHashSet(new EncryptedDataParent(settingAttributeUuid,
              SettingVariableTypes.APM_VERIFICATION, (i < 2 ? "header." : "option.") + "key" + (i + 1))));
    }

    savedSettingAttribute = wingsPersistence.get(SettingAttribute.class, settingAttributeUuid);
    savedApmVerificationConfig = (APMVerificationConfig) savedSettingAttribute.getValue();
    headersList = savedApmVerificationConfig.getHeadersList();

    keyValues = headersList.get(0);
    assertThat(keyValues.getKey()).isEqualTo("key1");
    assertThat(keyValues.getValue()).isEqualTo(encryptedDataList.get(0).getUuid());
    assertThat(keyValues.isEncrypted()).isTrue();

    keyValues = headersList.get(1);
    assertThat(keyValues.getKey()).isEqualTo("key2");
    assertThat(keyValues.getValue()).isEqualTo(encryptedDataList.get(1).getUuid());
    assertThat(keyValues.isEncrypted()).isTrue();

    keyValues = headersList.get(2);
    assertThat(keyValues.getKey()).isEqualTo("api_key_plain");
    assertThat(keyValues.getValue()).isEqualTo("123");
    assertThat(keyValues.getEncryptedValue()).isNull();
    assertThat(keyValues.isEncrypted()).isFalse();

    optionsList = savedApmVerificationConfig.getOptionsList();
    keyValues = optionsList.get(0);
    assertThat(keyValues.getKey()).isEqualTo("key3");
    assertThat(keyValues.getValue()).isEqualTo(encryptedDataList.get(2).getUuid());
    assertThat(keyValues.isEncrypted()).isTrue();

    keyValues = optionsList.get(1);
    assertThat(keyValues.getKey()).isEqualTo("key4");
    assertThat(keyValues.getValue()).isEqualTo(encryptedDataList.get(3).getUuid());
    assertThat(keyValues.isEncrypted()).isTrue();

    keyValues = optionsList.get(2);
    assertThat(keyValues.getKey()).isEqualTo("option_key_plain");
    assertThat(keyValues.getValue()).isEqualTo("321");
    assertThat(keyValues.getEncryptedValue()).isNull();
    assertThat(keyValues.isEncrypted()).isFalse();
  }
}
