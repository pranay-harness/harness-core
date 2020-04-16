package software.wings.security.encryption.setupusage;

import static io.harness.rule.OwnerRule.UTKARSH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static software.wings.service.intfc.security.SecretManager.HARNESS_DEFAULT_SECRET_MANAGER;
import static software.wings.settings.SettingValue.SettingVariableTypes.AWS;
import static software.wings.settings.SettingValue.SettingVariableTypes.DOCKER;
import static software.wings.settings.SettingValue.SettingVariableTypes.SERVICE_VARIABLE;

import com.google.common.collect.Sets;
import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.data.structure.UUIDGenerator;
import io.harness.exception.InvalidArgumentsException;
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
import software.wings.security.encryption.EncryptedData;
import software.wings.security.encryption.EncryptedDataParent;
import software.wings.security.encryption.EncryptionDetail;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.security.SecretManagerConfigService;
import software.wings.settings.SettingValue;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class SecretSetupUsageServiceImplTest extends WingsBaseTest {
  @Mock private SecretManagerConfigService secretManagerConfigService;
  @Mock private SecretSetupUsageBuilderRegistry secretSetupUsageBuilderRegistry;
  @Inject private FeatureFlagService featureFlagService;
  @Inject @InjectMocks private SecretSetupUsageServiceImpl secretSetupUsageService;
  private SecretSetupUsageBuilder secretSetupUsageBuilder;
  private EncryptedData encryptedData;
  private Account account;
  private EncryptionDetail encryptionDetail;

  @Before
  public void setup() {
    initMocks(this);
    account = getAccount(AccountType.PAID);
    account.setUuid(wingsPersistence.save(account));

    encryptionDetail = EncryptionDetail.builder()
                           .secretManagerName(HARNESS_DEFAULT_SECRET_MANAGER)
                           .encryptionType(EncryptionType.LOCAL)
                           .build();

    encryptedData = EncryptedData.builder()
                        .encryptionKey("plainTextKey")
                        .encryptedValue("encryptedValue".toCharArray())
                        .encryptionType(EncryptionType.LOCAL)
                        .type(SettingValue.SettingVariableTypes.SECRET_TEXT)
                        .kmsId(account.getUuid())
                        .enabled(true)
                        .accountId(account.getUuid())
                        .name("xyz")
                        .build();

    encryptedData.setUuid(wingsPersistence.save(encryptedData));

    secretSetupUsageBuilder = mock(SecretSetupUsageBuilder.class);

    when(secretManagerConfigService.getSecretManagerName(account.getUuid(), EncryptionType.LOCAL, account.getUuid()))
        .thenReturn(HARNESS_DEFAULT_SECRET_MANAGER);

    when(secretSetupUsageBuilderRegistry.getSecretSetupUsageBuilder(any()))
        .thenReturn(Optional.of(secretSetupUsageBuilder));
  }

  @Test(expected = InvalidArgumentsException.class)
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testGetSecretUsage_shouldThrowError() {
    secretSetupUsageService.getSecretUsage(account.getUuid(), UUIDGenerator.generateUuid());
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testGetSecretUsage_shouldReturnEmpty() {
    Set<SecretSetupUsage> secretSetupUsageSet =
        secretSetupUsageService.getSecretUsage(account.getUuid(), encryptedData.getUuid());
    assertThat(secretSetupUsageSet).isEmpty();
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testGetSecretUsage_notSupported() {
    EncryptedDataParent encryptedDataParent1 =
        new EncryptedDataParent(UUIDGenerator.generateUuid(), SERVICE_VARIABLE, null);
    encryptedData.addParent(encryptedDataParent1);
    wingsPersistence.save(encryptedData);
    when(secretSetupUsageBuilderRegistry.getSecretSetupUsageBuilder(any())).thenReturn(Optional.empty());

    Set<SecretSetupUsage> secretSetupUsageSet =
        secretSetupUsageService.getSecretUsage(account.getUuid(), encryptedData.getUuid());
    assertThat(secretSetupUsageSet).isEmpty();
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testGetSecretUsage_featureFlagDisabled() {
    EncryptedDataParent encryptedDataParent1 =
        new EncryptedDataParent(UUIDGenerator.generateUuid(), SERVICE_VARIABLE, null);
    EncryptedDataParent encryptedDataParent2 =
        new EncryptedDataParent(UUIDGenerator.generateUuid(), SERVICE_VARIABLE, null);

    encryptedData.addParent(encryptedDataParent1);
    encryptedData.addParent(encryptedDataParent2);
    wingsPersistence.save(encryptedData);

    Map<String, Set<EncryptedDataParent>> expectedParentIdByParentsMap = new HashMap<>();
    expectedParentIdByParentsMap.put(encryptedDataParent1.getId(), Sets.newHashSet(encryptedDataParent1));
    expectedParentIdByParentsMap.put(encryptedDataParent2.getId(), Sets.newHashSet(encryptedDataParent2));

    SecretSetupUsage mockUsage1 =
        SecretSetupUsage.builder().entityId(encryptedDataParent1.getId()).type(SERVICE_VARIABLE).build();
    SecretSetupUsage mockUsage2 =
        SecretSetupUsage.builder().entityId(encryptedDataParent2.getId()).type(SERVICE_VARIABLE).build();

    when(secretSetupUsageBuilder.buildSecretSetupUsages(
             account.getUuid(), encryptedData.getUuid(), expectedParentIdByParentsMap, encryptionDetail))
        .thenReturn(Sets.newHashSet(mockUsage1, mockUsage2));

    Set<SecretSetupUsage> expectedResponse = Sets.newHashSet(mockUsage1, mockUsage2);

    Set<SecretSetupUsage> secretSetupUsages =
        secretSetupUsageService.getSecretUsage(account.getUuid(), encryptedData.getUuid());

    assertThat(secretSetupUsages).isEqualTo(expectedResponse);

    verify(secretManagerConfigService, times(1)).getSecretManagerName(any(), any(), any());
    verify(secretSetupUsageBuilderRegistry, times(1)).getSecretSetupUsageBuilder(SERVICE_VARIABLE);
    verify(secretSetupUsageBuilder, times(1))
        .buildSecretSetupUsages(
            account.getUuid(), encryptedData.getUuid(), expectedParentIdByParentsMap, encryptionDetail);
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testGetSecretUsage_featureFlagEnabled() {
    featureFlagService.enableAccount(FeatureName.SECRET_PARENTS_MIGRATED, account.getUuid());

    String commonUuid = UUIDGenerator.generateUuid();
    EncryptedDataParent encryptedDataParent1 =
        new EncryptedDataParent(UUIDGenerator.generateUuid(), SERVICE_VARIABLE, "randomFieldName1");
    EncryptedDataParent encryptedDataParent2 =
        new EncryptedDataParent(UUIDGenerator.generateUuid(), SERVICE_VARIABLE, "randomFieldName1");
    EncryptedDataParent encryptedDataParent3 =
        new EncryptedDataParent(UUIDGenerator.generateUuid(), DOCKER, "randomFieldName2");
    EncryptedDataParent encryptedDataParent4 = new EncryptedDataParent(commonUuid, AWS, "randomFieldName3");
    EncryptedDataParent encryptedDataParent5 = new EncryptedDataParent(commonUuid, AWS, "randomFieldName4");

    encryptedData.addParent(encryptedDataParent1);
    encryptedData.addParent(encryptedDataParent2);
    encryptedData.addParent(encryptedDataParent3);
    encryptedData.addParent(encryptedDataParent4);
    encryptedData.addParent(encryptedDataParent5);
    wingsPersistence.save(encryptedData);

    Map<String, Set<EncryptedDataParent>> parentIdByParentsMap1 = new HashMap<>();
    parentIdByParentsMap1.put(encryptedDataParent1.getId(), Sets.newHashSet(encryptedDataParent1));
    parentIdByParentsMap1.put(encryptedDataParent2.getId(), Sets.newHashSet(encryptedDataParent2));

    Map<String, Set<EncryptedDataParent>> parentIdByParentsMap2 = new HashMap<>();
    parentIdByParentsMap2.put(encryptedDataParent3.getId(), Sets.newHashSet(encryptedDataParent3));

    Map<String, Set<EncryptedDataParent>> parentIdByParentsMap3 = new HashMap<>();
    parentIdByParentsMap3.put(commonUuid, Sets.newHashSet(encryptedDataParent4, encryptedDataParent5));

    SecretSetupUsage mockUsage1 =
        SecretSetupUsage.builder().entityId(encryptedDataParent1.getId()).type(SERVICE_VARIABLE).build();
    SecretSetupUsage mockUsage2 =
        SecretSetupUsage.builder().entityId(encryptedDataParent2.getId()).type(SERVICE_VARIABLE).build();
    SecretSetupUsage mockUsage3 =
        SecretSetupUsage.builder().entityId(encryptedDataParent3.getId()).type(DOCKER).build();
    SecretSetupUsage mockUsage4 = SecretSetupUsage.builder().entityId(commonUuid).type(AWS).build();
    SecretSetupUsage mockUsage5 = SecretSetupUsage.builder().entityId(commonUuid).type(AWS).build();

    when(secretSetupUsageBuilder.buildSecretSetupUsages(
             eq(account.getUuid()), eq(encryptedData.getUuid()), any(), eq(encryptionDetail)))
        .thenReturn(Sets.newHashSet(mockUsage1, mockUsage2))
        .thenReturn(Sets.newHashSet(mockUsage3))
        .thenReturn(Sets.newHashSet(mockUsage4, mockUsage5));

    Set<SecretSetupUsage> expectedResponse =
        Sets.newHashSet(mockUsage1, mockUsage2, mockUsage3, mockUsage4, mockUsage5);

    Set<SecretSetupUsage> secretSetupUsages =
        secretSetupUsageService.getSecretUsage(account.getUuid(), encryptedData.getUuid());

    assertThat(secretSetupUsages).isEqualTo(expectedResponse);

    verify(secretManagerConfigService, times(1)).getSecretManagerName(any(), any(), any());
    verify(secretSetupUsageBuilderRegistry, times(1)).getSecretSetupUsageBuilder(SERVICE_VARIABLE);
    verify(secretSetupUsageBuilderRegistry, times(1)).getSecretSetupUsageBuilder(DOCKER);
    verify(secretSetupUsageBuilderRegistry, times(1)).getSecretSetupUsageBuilder(AWS);
    verify(secretSetupUsageBuilder, times(1))
        .buildSecretSetupUsages(account.getUuid(), encryptedData.getUuid(), parentIdByParentsMap1, encryptionDetail);
    verify(secretSetupUsageBuilder, times(1))
        .buildSecretSetupUsages(account.getUuid(), encryptedData.getUuid(), parentIdByParentsMap2, encryptionDetail);
    verify(secretSetupUsageBuilder, times(1))
        .buildSecretSetupUsages(account.getUuid(), encryptedData.getUuid(), parentIdByParentsMap3, encryptionDetail);
  }
}
