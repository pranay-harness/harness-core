package software.wings.security.encryption.setupusage.builders;

import static io.harness.rule.OwnerRule.UTKARSH;
import static io.harness.security.encryption.EncryptionType.KMS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.common.collect.Sets;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.Account;
import software.wings.beans.AccountType;
import software.wings.beans.FeatureName;
import software.wings.beans.KmsConfig;
import software.wings.beans.KmsConfig.KmsConfigKeys;
import software.wings.beans.SecretManagerConfig;
import software.wings.security.encryption.EncryptedData;
import software.wings.security.encryption.EncryptedDataParent;
import software.wings.security.encryption.EncryptionDetail;
import software.wings.security.encryption.setupusage.SecretSetupUsage;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.security.SecretManagerConfigService;
import software.wings.settings.SettingValue.SettingVariableTypes;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class SecretManagerSetupUsageBuilderTest extends WingsBaseTest {
  @Mock SecretManagerConfigService secretManagerConfigService;
  @Inject @InjectMocks SecretManagerSetupUsageBuilder secretManagerSetupUsageBuilder;
  @Inject private FeatureFlagService featureFlagService;
  private Account account;
  private EncryptedData encryptedData;
  private KmsConfig kmsConfig;
  private EncryptionDetail encryptionDetail;

  @Before
  public void setup() {
    initMocks(this);
    account = getAccount(AccountType.PAID);
    account.setUuid(wingsPersistence.save(account));

    encryptedData = EncryptedData.builder()
                        .encryptionKey("plainTextKey")
                        .encryptedValue("encryptedValue".toCharArray())
                        .encryptionType(EncryptionType.LOCAL)
                        .type(SettingVariableTypes.GCP_KMS)
                        .kmsId(account.getUuid())
                        .enabled(true)
                        .accountId(account.getUuid())
                        .build();

    encryptedData.setUuid(null);
    encryptedData.setType(SettingVariableTypes.KMS);
    encryptedData.setUuid(UUIDGenerator.generateUuid());
    kmsConfig = getKmsConfig();
    kmsConfig.setEncryptionType(KMS);
    kmsConfig.setKmsArn(encryptedData.getUuid());
    kmsConfig.setSecretKey(encryptedData.getUuid());
    kmsConfig.setAccountId(account.getUuid());
    kmsConfig.setUuid(UUIDGenerator.generateUuid());

    encryptionDetail =
        EncryptionDetail.builder().secretManagerName("secretManagerName").encryptionType(EncryptionType.LOCAL).build();
  }

  private Set<EncryptedDataParent> getEncryptedDataParents() {
    String fieldName1 = EncryptionReflectUtils.getEncryptedFieldTag(
        ReflectionUtils.getFieldByName(kmsConfig.getClass(), KmsConfigKeys.kmsArn));
    EncryptedDataParent encryptedDataParent1 =
        new EncryptedDataParent(kmsConfig.getUuid(), SettingVariableTypes.KMS, fieldName1);

    String fieldName2 = EncryptionReflectUtils.getEncryptedFieldTag(
        ReflectionUtils.getFieldByName(kmsConfig.getClass(), KmsConfigKeys.secretKey));
    EncryptedDataParent encryptedDataParent2 =
        new EncryptedDataParent(kmsConfig.getUuid(), SettingVariableTypes.KMS, fieldName2);

    return Sets.newHashSet(encryptedDataParent1, encryptedDataParent2);
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testBuildSecretSetupUsage_featureFlagDisabled() {
    SecretManagerConfig secretManagerConfig = mock(SecretManagerConfig.class);
    when(secretManagerConfig.getUuid()).thenReturn(UUIDGenerator.generateUuid());
    when(secretManagerConfigService.listSecretManagers(account.getUuid(), true, false))
        .thenReturn(Arrays.asList(kmsConfig, secretManagerConfig));

    Map<String, Set<EncryptedDataParent>> parentByParentIds = new HashMap<>();
    parentByParentIds.put(kmsConfig.getUuid(), getEncryptedDataParents());

    Set<SecretSetupUsage> returnedSetupUsages = secretManagerSetupUsageBuilder.buildSecretSetupUsages(
        account.getUuid(), encryptedData.getUuid(), parentByParentIds, encryptionDetail);
    assertThat(returnedSetupUsages).hasSize(1);
    SecretSetupUsage secretSetupUsage = returnedSetupUsages.iterator().next();
    assertThat(secretSetupUsage.getEntityId()).isEqualTo(kmsConfig.getUuid());
    assertThat(secretSetupUsage.getType()).isEqualTo(SettingVariableTypes.KMS);
    assertThat(secretSetupUsage.getEntity()).isEqualTo(kmsConfig);
    assertThat(secretSetupUsage.getFieldName()).isNull();
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testBuildSecretSetupUsage_featureFlagEnabled() {
    featureFlagService.enableAccount(FeatureName.SECRET_PARENTS_MIGRATED, account.getUuid());
    SecretManagerConfig secretManagerConfig = mock(SecretManagerConfig.class);
    when(secretManagerConfig.getUuid()).thenReturn(UUIDGenerator.generateUuid());
    when(secretManagerConfigService.listSecretManagers(account.getUuid(), true, false))
        .thenReturn(Arrays.asList(kmsConfig, secretManagerConfig));

    Map<String, Set<EncryptedDataParent>> parentByParentIds = new HashMap<>();
    parentByParentIds.put(kmsConfig.getUuid(), getEncryptedDataParents());

    Set<SecretSetupUsage> returnedSetupUsages = secretManagerSetupUsageBuilder.buildSecretSetupUsages(
        account.getUuid(), encryptedData.getUuid(), parentByParentIds, encryptionDetail);

    assertThat(returnedSetupUsages).hasSize(2);
    Iterator<SecretSetupUsage> secretSetupUsageIterator = returnedSetupUsages.iterator();
    SecretSetupUsage secretSetupUsage1 = secretSetupUsageIterator.next();
    SecretSetupUsage secretSetupUsage2 = secretSetupUsageIterator.next();

    assertThat(secretSetupUsage1.getEntityId()).isEqualTo(kmsConfig.getUuid());
    assertThat(secretSetupUsage1.getType()).isEqualTo(SettingVariableTypes.KMS);
    assertThat(secretSetupUsage1.getEntity()).isEqualTo(kmsConfig);
    assertThat(secretSetupUsage2.getEntityId()).isEqualTo(kmsConfig.getUuid());
    assertThat(secretSetupUsage2.getType()).isEqualTo(SettingVariableTypes.KMS);
    assertThat(secretSetupUsage2.getEntity()).isEqualTo(kmsConfig);

    assertThat(Sets.newHashSet(secretSetupUsage1.getFieldName(), secretSetupUsage2.getFieldName()))
        .isEqualTo(Sets.newHashSet(KmsConfigKeys.secretKey, KmsConfigKeys.kmsArn));
  }
}
