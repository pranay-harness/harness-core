package software.wings.service.cyberark;

import static io.harness.persistence.HQuery.excludeAuthority;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.common.collect.Sets;
import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.exception.WingsException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.Account;
import software.wings.beans.AccountType;
import software.wings.beans.CyberArkConfig;
import software.wings.beans.SyncTaskContext;
import software.wings.beans.User;
import software.wings.beans.security.HarnessUserGroup;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.dl.WingsPersistence;
import software.wings.features.api.PremiumFeature;
import software.wings.resources.CyberArkResource;
import software.wings.resources.SecretManagementResource;
import software.wings.security.PermissionAttribute.Action;
import software.wings.security.UserThreadLocal;
import software.wings.security.encryption.EncryptedData;
import software.wings.security.encryption.EncryptedData.EncryptedDataKeys;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.HarnessUserGroupService;
import software.wings.service.intfc.security.CyberArkService;
import software.wings.service.intfc.security.SecretManagementDelegateService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.service.intfc.security.SecretManagerConfigService;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

/**
 * @author marklu on 2019-08-09
 */
public class CyberArkTest extends WingsBaseTest {
  @Inject private CyberArkResource cyberArkResource;
  @Inject private SecretManagementResource secretManagementResource;
  @Mock private AccountService accountService;
  @Inject @InjectMocks private CyberArkService cyberArkService;
  @Inject @InjectMocks private SecretManagerConfigService secretManagerConfigService;
  @Inject private SecretManager secretManager;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private HarnessUserGroupService harnessUserGroupService;
  @Mock private SecretManagementDelegateService secretManagementDelegateService;
  @Mock private DelegateProxyFactory delegateProxyFactory;
  @Mock private PremiumFeature secretsManagementFeature;
  private final int numOfEncryptedValsForCyberArk = 1;
  private final String userEmail = "mark.lu@harness.io";
  private final String userName = "raghu";
  private final User user = User.Builder.anUser().withEmail(userEmail).withName(userName).build();
  private String userId;
  private String accountId;

  @Before
  public void setup() throws IOException, NoSuchFieldException, IllegalAccessException {
    initMocks(this);

    Account account = getAccount(AccountType.PAID);
    accountId = account.getUuid();
    when(accountService.get(accountId)).thenReturn(account);

    when(secretsManagementFeature.isAvailableForAccount(accountId)).thenReturn(true);

    when(secretManagementDelegateService.decrypt(anyObject(), any(CyberArkConfig.class))).then(invocation -> {
      Object[] args = invocation.getArguments();
      return decrypt((EncryptedData) args[0], (CyberArkConfig) args[1]);
    });
    when(delegateProxyFactory.get(eq(SecretManagementDelegateService.class), any(SyncTaskContext.class)))
        .thenReturn(secretManagementDelegateService);
    FieldUtils.writeField(cyberArkService, "delegateProxyFactory", delegateProxyFactory, true);
    FieldUtils.writeField(secretManager, "cyberArkService", cyberArkService, true);
    FieldUtils.writeField(wingsPersistence, "secretManager", secretManager, true);
    FieldUtils.writeField(cyberArkResource, "cyberArkService", cyberArkService, true);
    FieldUtils.writeField(secretManagementResource, "secretManager", secretManager, true);
    userId = wingsPersistence.save(user);
    UserThreadLocal.set(user);

    // Add current user to harness user group so that save-global-kms operation can succeed
    HarnessUserGroup harnessUserGroup = HarnessUserGroup.builder()
                                            .applyToAllAccounts(true)
                                            .memberIds(Sets.newHashSet(userId))
                                            .actions(Sets.newHashSet(Action.READ))
                                            .build();
    harnessUserGroupService.save(harnessUserGroup);
  }

  @Test
  @Category(UnitTests.class)
  public void validateConfig() {
    CyberArkConfig cyberArkConfig = getCyberArkConfig("invalidCertificate");
    cyberArkConfig.setAccountId(accountId);

    try {
      cyberArkResource.saveCyberArkConfig(cyberArkConfig.getAccountId(), cyberArkConfig);
      fail("Saved invalid CyberArk config");
    } catch (WingsException e) {
      assertTrue(true);
    }

    cyberArkConfig = getCyberArkConfig();
    cyberArkConfig.setAccountId(accountId);
    cyberArkConfig.setCyberArkUrl("invalidUrl");

    try {
      cyberArkResource.saveCyberArkConfig(cyberArkConfig.getAccountId(), cyberArkConfig);
      fail("Saved invalid CyberArk config");
    } catch (WingsException e) {
      assertTrue(true);
    }
  }

  @Test
  @Category(UnitTests.class)
  public void getCyberArkConfigForAccount() {
    CyberArkConfig cyberArkConfig = getCyberArkConfig();
    cyberArkConfig.setAccountId(accountId);

    cyberArkResource.saveCyberArkConfig(cyberArkConfig.getAccountId(), cyberArkConfig);

    CyberArkConfig savedConfig =
        (CyberArkConfig) secretManagerConfigService.getDefaultSecretManager(cyberArkConfig.getAccountId());
    assertEquals(cyberArkConfig.getName(), savedConfig.getName());
    assertEquals(cyberArkConfig.getAccountId(), savedConfig.getAccountId());
  }

  @Test
  @Category(UnitTests.class)
  public void saveAndEditConfig() {
    InputStream inputStream = CyberArkTest.class.getResourceAsStream("/certs/clientCert.pem");
    String clientCertificate = null;
    try {
      clientCertificate = IOUtils.toString(inputStream, "UTF-8");
    } catch (IOException e) {
      // Should not happen.
    }

    String name = UUID.randomUUID().toString();
    CyberArkConfig cyberArkConfig = getCyberArkConfig(clientCertificate);
    cyberArkConfig.setName(name);
    cyberArkConfig.setAccountId(accountId);

    cyberArkResource.saveCyberArkConfig(cyberArkConfig.getAccountId(), cyberArkConfig);

    CyberArkConfig savedConfig =
        (CyberArkConfig) secretManagerConfigService.getDefaultSecretManager(cyberArkConfig.getAccountId());
    assertEquals(name, savedConfig.getName());
    List<EncryptedData> encryptedDataList =
        wingsPersistence.createQuery(EncryptedData.class, excludeAuthority).asList();
    assertEquals(numOfEncryptedValsForCyberArk, encryptedDataList.size());
    for (EncryptedData encryptedData : encryptedDataList) {
      assertEquals(encryptedData.getName(), name + "_clientCertificate");
      assertEquals(1, encryptedData.getParentIds().size());
      assertEquals(savedConfig.getUuid(), encryptedData.getParentIds().iterator().next());
    }

    name = UUID.randomUUID().toString();
    CyberArkConfig newConfig = getCyberArkConfig();
    savedConfig.setClientCertificate(newConfig.getClientCertificate());
    savedConfig.setName(name);
    cyberArkResource.saveCyberArkConfig(accountId, savedConfig);
    encryptedDataList =
        wingsPersistence.createQuery(EncryptedData.class).filter(EncryptedDataKeys.accountId, accountId).asList();
    assertEquals(numOfEncryptedValsForCyberArk, encryptedDataList.size());
    for (EncryptedData encryptedData : encryptedDataList) {
      assertEquals(1, encryptedData.getParentIds().size());
      assertEquals(savedConfig.getUuid(), encryptedData.getParentIds().iterator().next());
    }
  }
}
