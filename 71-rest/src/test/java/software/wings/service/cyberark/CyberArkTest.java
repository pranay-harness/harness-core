package software.wings.service.cyberark;

import static io.harness.persistence.HQuery.excludeAuthority;
import static io.harness.rule.OwnerRule.UTKARSH;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.exception.WingsException;
import io.harness.rule.Owner;
import io.harness.rule.RealMongo;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.security.encryption.EncryptionType;
import io.harness.serializer.KryoUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.Account;
import software.wings.beans.AccountType;
import software.wings.beans.CyberArkConfig;
import software.wings.beans.Event.Type;
import software.wings.beans.JenkinsConfig;
import software.wings.beans.KmsConfig;
import software.wings.beans.LocalEncryptionConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SyncTaskContext;
import software.wings.beans.User;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.features.api.PremiumFeature;
import software.wings.resources.CyberArkResource;
import software.wings.security.UserThreadLocal;
import software.wings.security.encryption.EncryptedData;
import software.wings.security.encryption.EncryptedData.EncryptedDataKeys;
import software.wings.service.impl.AuditServiceHelper;
import software.wings.service.impl.security.GlobalEncryptDecryptClient;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.security.CyberArkService;
import software.wings.service.intfc.security.KmsService;
import software.wings.service.intfc.security.LocalEncryptionService;
import software.wings.service.intfc.security.SecretManagementDelegateService;
import software.wings.service.intfc.security.SecretManagerConfigService;
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.settings.UsageRestrictions;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * @author marklu on 2019-08-09
 */
@RunWith(Parameterized.class)
public class CyberArkTest extends WingsBaseTest {
  @Inject private CyberArkResource cyberArkResource;
  @Mock private AccountService accountService;
  @Inject private LocalEncryptionService localEncryptionService;
  @Mock private DelegateProxyFactory delegateProxyFactory;
  @Mock private SecretManagementDelegateService secretManagementDelegateService;
  @Mock private PremiumFeature secretsManagementFeature;
  @Mock private GlobalEncryptDecryptClient globalEncryptDecryptClient;
  @Mock protected AuditServiceHelper auditServiceHelper;
  @Inject @InjectMocks private KmsService kmsService;
  @Inject @InjectMocks private CyberArkService cyberArkService;
  @Inject @InjectMocks private SecretManagerConfigService secretManagerConfigService;
  private final int numOfEncryptedValsForCyberArk = 1;
  private final int numOfEncryptedValsForCyberKms = 3;
  private final String userEmail = "mark.lu@harness.io";
  private final String userName = "mark.lu";
  private final User user = User.Builder.anUser().email(userEmail).name(userName).build();
  private String userId;
  private String accountId;
  private String kmsId;
  private KmsConfig kmsConfig;
  private LocalEncryptionConfig localEncryptionConfig;

  @Parameter public boolean isGlobalKmsEnabled;

  @Parameters
  public static Collection<Object[]> data() {
    return asList(new Object[][] {{true}, {false}});
  }

  @Before
  public void setup() throws IOException, NoSuchFieldException, IllegalAccessException {
    initMocks(this);

    Account account = getAccount(AccountType.PAID);
    accountId = account.getUuid();
    when(accountService.get(accountId)).thenReturn(account);

    when(secretsManagementFeature.isAvailableForAccount(accountId)).thenReturn(true);

    when(secretManagementDelegateService.decrypt(anyObject(), any(KmsConfig.class))).then(invocation -> {
      Object[] args = invocation.getArguments();
      return decrypt((EncryptedData) args[0], (KmsConfig) args[1]);
    });

    when(secretManagementDelegateService.decrypt(anyObject(), any(CyberArkConfig.class))).then(invocation -> {
      Object[] args = invocation.getArguments();
      return decrypt((EncryptedData) args[0], (CyberArkConfig) args[1]);
    });

    when(globalEncryptDecryptClient.encrypt(anyString(), any(), any(KmsConfig.class))).then(invocation -> {
      Object[] args = invocation.getArguments();
      return encrypt((String) args[0], (char[]) args[1], (KmsConfig) args[2]);
    });

    when(globalEncryptDecryptClient.decrypt(anyObject(), anyString(), any(KmsConfig.class))).then(invocation -> {
      Object[] args = invocation.getArguments();
      return decrypt((EncryptedData) args[0], (KmsConfig) args[2]);
    });

    when(delegateProxyFactory.get(eq(SecretManagementDelegateService.class), any(SyncTaskContext.class)))
        .thenReturn(secretManagementDelegateService);
    when(secretManagementDelegateService.encrypt(anyString(), anyObject(), any(KmsConfig.class))).then(invocation -> {
      Object[] args = invocation.getArguments();
      return encrypt((String) args[0], (char[]) args[1], (KmsConfig) args[2]);
    });
    when(secretManagementDelegateService.validateCyberArkConfig(any(CyberArkConfig.class))).then(invocation -> {
      Object[] args = invocation.getArguments();
      return validateCyberArkConfig((CyberArkConfig) args[0]);
    });

    FieldUtils.writeField(cyberArkService, "delegateProxyFactory", delegateProxyFactory, true);
    FieldUtils.writeField(kmsService, "delegateProxyFactory", delegateProxyFactory, true);
    FieldUtils.writeField(secretManager, "kmsService", kmsService, true);
    FieldUtils.writeField(secretManager, "cyberArkService", cyberArkService, true);
    FieldUtils.writeField(wingsPersistence, "secretManager", secretManager, true);
    FieldUtils.writeField(cyberArkResource, "cyberArkService", cyberArkService, true);
    FieldUtils.writeField(secretManagementResource, "secretManager", secretManager, true);
    userId = wingsPersistence.save(user);
    UserThreadLocal.set(user);

    if (isGlobalKmsEnabled) {
      kmsConfig = getKmsConfig();
      kmsConfig.setName("Global KMS");
      kmsConfig.setAccountId(Account.GLOBAL_ACCOUNT_ID);
      kmsId = kmsService.saveGlobalKmsConfig(accountId, kmsConfig);
      kmsConfig = kmsService.getKmsConfig(accountId, kmsId);
    } else {
      localEncryptionConfig = localEncryptionService.getEncryptionConfig(accountId);
    }
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void validateConfig() {
    CyberArkConfig cyberArkConfig = getCyberArkConfig("invalidCertificate");
    cyberArkConfig.setAccountId(accountId);

    try {
      cyberArkResource.saveCyberArkConfig(cyberArkConfig.getAccountId(), cyberArkConfig);
      fail("Saved invalid CyberArk config");
    } catch (WingsException e) {
      assertThat(true).isTrue();
    }

    cyberArkConfig = getCyberArkConfig();
    cyberArkConfig.setAccountId(accountId);
    cyberArkConfig.setCyberArkUrl("invalidUrl");

    try {
      cyberArkResource.saveCyberArkConfig(cyberArkConfig.getAccountId(), cyberArkConfig);
      fail("Saved invalid CyberArk config");
    } catch (WingsException e) {
      assertThat(true).isTrue();
    }
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void getCyberArkConfigForAccount() {
    CyberArkConfig cyberArkConfig = getCyberArkConfig();
    cyberArkConfig.setAccountId(accountId);

    cyberArkResource.saveCyberArkConfig(cyberArkConfig.getAccountId(), cyberArkConfig);

    CyberArkConfig savedConfig =
        (CyberArkConfig) secretManagerConfigService.getDefaultSecretManager(cyberArkConfig.getAccountId());
    assertThat(savedConfig.getName()).isEqualTo(cyberArkConfig.getName());
    assertThat(savedConfig.getAccountId()).isEqualTo(cyberArkConfig.getAccountId());
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void saveAndEditConfig() {
    InputStream inputStream = CyberArkTest.class.getResourceAsStream("/certs/clientCert.pem");
    String clientCertificate = null;
    try {
      clientCertificate = IOUtils.toString(inputStream, "UTF-8");
    } catch (IOException e) {
      // Should not happen.
    }

    CyberArkConfig cyberArkConfig = saveCyberArkConfig(clientCertificate);
    String name = cyberArkConfig.getName();

    CyberArkConfig savedConfig =
        (CyberArkConfig) secretManagerConfigService.getDefaultSecretManager(cyberArkConfig.getAccountId());
    assertThat(savedConfig.getName()).isEqualTo(name);
    List<EncryptedData> encryptedDataList =
        wingsPersistence.createQuery(EncryptedData.class, excludeAuthority).asList();
    if (isGlobalKmsEnabled) {
      assertThat(encryptedDataList).hasSize(numOfEncryptedValsForCyberArk + numOfEncryptedValsForCyberKms);
    } else {
      assertThat(encryptedDataList).hasSize(numOfEncryptedValsForCyberArk);
      for (EncryptedData encryptedData : encryptedDataList) {
        assertThat(name + "_clientCertificate").isEqualTo(encryptedData.getName());
        assertThat(encryptedData.getParents()).hasSize(1);
        assertThat(encryptedData.containsParent(savedConfig.getUuid(), SettingVariableTypes.CYBERARK)).isTrue();
      }
    }

    name = UUID.randomUUID().toString();
    CyberArkConfig newConfig = getCyberArkConfig();
    savedConfig.setClientCertificate(newConfig.getClientCertificate());
    savedConfig.setName(name);
    cyberArkResource.saveCyberArkConfig(accountId, savedConfig);
    encryptedDataList =
        wingsPersistence.createQuery(EncryptedData.class).filter(EncryptedDataKeys.accountId, accountId).asList();
    assertThat(encryptedDataList).hasSize(numOfEncryptedValsForCyberArk);
    for (EncryptedData encryptedData : encryptedDataList) {
      assertThat(encryptedData.getParents()).hasSize(1);
      assertThat(encryptedData.containsParent(savedConfig.getUuid(), SettingVariableTypes.CYBERARK)).isTrue();
    }
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  @RealMongo
  public void testEncryptDecryptArtifactoryConfig() {
    String queryAsPath = "Address=components;Username=svc_account";
    String secretName = "TestSecret";
    String secretValue = "MySecretValue";

    saveCyberArkConfig();

    // Encrypt of path reference will use a CyberArk decryption to validate the reference
    EncryptedData encryptedData = secretManager.encrypt(
        accountId, SettingVariableTypes.ARTIFACTORY, null, queryAsPath, null, secretName, new UsageRestrictions());
    assertThat(encryptedData).isNotNull();
    assertThat(encryptedData.getEncryptionType()).isEqualTo(EncryptionType.CYBERARK);
    assertThat(encryptedData.getType()).isEqualTo(SettingVariableTypes.ARTIFACTORY);
    assertThat(encryptedData.getEncryptedValue()).isNull();

    // Encrypt of real secret text will use a LOCAL Harness SecretStore to encrypt, since CyberArk doesn't support
    // creating new reference now.
    encryptedData = secretManager.encrypt(accountId, SettingVariableTypes.ARTIFACTORY, secretValue.toCharArray(), null,
        null, secretName, new UsageRestrictions());
    assertThat(encryptedData).isNotNull();
    if (isGlobalKmsEnabled) {
      assertThat(encryptedData.getEncryptionType()).isEqualTo(EncryptionType.KMS);
      assertThat(encryptedData.getKmsId()).isEqualTo(kmsId);
    } else {
      assertThat(encryptedData.getEncryptionType()).isEqualTo(EncryptionType.LOCAL);
    }
    assertThat(encryptedData.getType()).isEqualTo(SettingVariableTypes.ARTIFACTORY);
    assertThat(encryptedData.getEncryptedValue()).isNotNull();
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  @RealMongo
  public void testEncryptDecryptJenkinsConfig() {
    saveCyberArkConfig();

    String password = UUID.randomUUID().toString();
    JenkinsConfig jenkinsConfig = getJenkinsConfig(accountId, password);
    SettingAttribute settingAttribute = getSettingAttribute(jenkinsConfig);
    String savedAttributeId = wingsPersistence.save(KryoUtils.clone(settingAttribute));

    SettingAttribute savedAttribute = wingsPersistence.get(SettingAttribute.class, savedAttributeId);
    JenkinsConfig savedJenkinsConfig = (JenkinsConfig) savedAttribute.getValue();

    List<EncryptedDataDetail> encryptedDataDetails =
        secretManager.getEncryptionDetails(savedJenkinsConfig, accountId, null);
    assertThat(encryptedDataDetails).hasSize(1);

    char[] decryptedValue;

    EncryptedData encryptedPasswordData =
        wingsPersistence.get(EncryptedData.class, savedJenkinsConfig.getEncryptedPassword());
    assertThat(encryptedPasswordData).isNotNull();
    assertThat(encryptedPasswordData.getType()).isEqualTo(SettingVariableTypes.JENKINS);
    if (isGlobalKmsEnabled) {
      assertThat(encryptedPasswordData.getEncryptionType()).isEqualTo(EncryptionType.KMS);
      assertThat(encryptedPasswordData.getKmsId()).isEqualTo(kmsId);
      decryptedValue = kmsService.decrypt(encryptedPasswordData, accountId, kmsConfig);
    } else {
      assertThat(encryptedPasswordData.getEncryptionType()).isEqualTo(EncryptionType.LOCAL);
      decryptedValue = localEncryptionService.decrypt(encryptedPasswordData, accountId, localEncryptionConfig);
    }

    assertThat(decryptedValue).isNotNull();
    assertThat(new String(decryptedValue)).isEqualTo(new String(jenkinsConfig.getPassword()));

    assertThat(savedJenkinsConfig.getEncryptedToken()).isNull();
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void saveAndEditConfig_withMaskedSecrets_changeNameDefaultOnly() {
    String name = UUID.randomUUID().toString();
    CyberArkConfig cyberArkConfig = getCyberArkConfig();
    cyberArkConfig.setName(name);
    cyberArkConfig.setAccountId(accountId);

    cyberArkService.saveConfig(accountId, KryoUtils.clone(cyberArkConfig));

    CyberArkConfig savedConfig = (CyberArkConfig) secretManagerConfigService.getDefaultSecretManager(accountId);
    assertThat(savedConfig.getClientCertificate()).isEqualTo(cyberArkConfig.getClientCertificate());
    assertThat(savedConfig.getAppId()).isEqualTo(cyberArkConfig.getAppId());
    assertThat(savedConfig.getName()).isEqualTo(cyberArkConfig.getName());
    assertThat(savedConfig.isDefault()).isEqualTo(true);

    String newName = UUID.randomUUID().toString();
    cyberArkConfig.setUuid(savedConfig.getUuid());
    cyberArkConfig.setName(newName);
    cyberArkConfig.setDefault(false);
    cyberArkConfig.maskSecrets();

    // Masked Secrets, only name and default flag should be updated.
    cyberArkService.saveConfig(accountId, KryoUtils.clone(cyberArkConfig));

    CyberArkConfig modifiedSavedConfig = cyberArkService.getConfig(accountId, savedConfig.getUuid());
    assertThat(modifiedSavedConfig.getClientCertificate()).isEqualTo(savedConfig.getClientCertificate());
    assertThat(modifiedSavedConfig.getAppId()).isEqualTo(savedConfig.getAppId());
    assertThat(modifiedSavedConfig.getName()).isEqualTo(cyberArkConfig.getName());
    assertThat(modifiedSavedConfig.isDefault()).isEqualTo(false);
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void cyberArkSecretManager_Crud_shouldGenerate_Audit() {
    if (isGlobalKmsEnabled) {
      verify(auditServiceHelper)
          .reportForAuditingUsingAccountId(
              eq(Account.GLOBAL_ACCOUNT_ID), eq(null), any(KmsConfig.class), eq(Type.CREATE));
    }

    String name = UUID.randomUUID().toString();
    CyberArkConfig cyberArkConfig = getCyberArkConfig();
    cyberArkConfig.setName(name);
    cyberArkConfig.setAccountId(accountId);

    String secretManagerId = cyberArkService.saveConfig(accountId, KryoUtils.clone(cyberArkConfig));
    verify(auditServiceHelper)
        .reportForAuditingUsingAccountId(eq(accountId), eq(null), any(CyberArkConfig.class), eq(Type.CREATE));

    cyberArkConfig.setUuid(secretManagerId);
    cyberArkConfig.setDefault(false);
    cyberArkConfig.setName(cyberArkConfig.getName() + "_Updated");
    cyberArkService.saveConfig(accountId, KryoUtils.clone(cyberArkConfig));
    verify(auditServiceHelper)
        .reportForAuditingUsingAccountId(
            eq(accountId), any(CyberArkConfig.class), any(CyberArkConfig.class), eq(Type.UPDATE));

    cyberArkService.deleteConfig(accountId, secretManagerId);
    verify(auditServiceHelper).reportDeleteForAuditingUsingAccountId(eq(accountId), any(CyberArkConfig.class));
  }

  private CyberArkConfig saveCyberArkConfig() {
    return saveCyberArkConfig(null);
  }

  private CyberArkConfig saveCyberArkConfig(String clientCertificate) {
    String name = UUID.randomUUID().toString();
    CyberArkConfig cyberArkConfig = getCyberArkConfig();
    cyberArkConfig.setName(name);
    cyberArkConfig.setAccountId(accountId);
    cyberArkConfig.setClientCertificate(clientCertificate);

    cyberArkResource.saveCyberArkConfig(cyberArkConfig.getAccountId(), cyberArkConfig);
    return cyberArkConfig;
  }
}
