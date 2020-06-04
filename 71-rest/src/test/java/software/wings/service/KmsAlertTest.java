package software.wings.service;

import static io.harness.rule.OwnerRule.RAGHU;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.Account;
import software.wings.beans.KmsConfig;
import software.wings.beans.SyncTaskContext;
import software.wings.beans.VaultConfig;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.security.SecretManagementException;
import software.wings.service.intfc.security.KmsService;
import software.wings.service.intfc.security.SecretManagementDelegateService;

import java.util.UUID;

/**
 * Created by rsingh on 11/3/17.
 */
@Slf4j
public class KmsAlertTest extends WingsBaseTest {
  private static String VAULT_TOKEN = System.getProperty("vault.token");

  @Inject private KmsService kmsService;
  @Inject private WingsPersistence wingsPersistence;
  @Mock private DelegateProxyFactory delegateProxyFactory;
  @Mock private SecretManagementDelegateService mockDelegateServiceOK;
  @Mock private SecretManagementDelegateService mockDelegateServiceEx;
  private String accountId;

  @Before
  public void setup() throws NoSuchFieldException, IllegalAccessException {
    initMocks(this);
    when(mockDelegateServiceOK.encrypt(
             anyString(), anyString(), anyString(), anyObject(), any(VaultConfig.class), anyObject()))
        .thenReturn(null);
    when(mockDelegateServiceOK.encrypt(anyString(), anyObject(), anyObject())).thenReturn(null);
    when(mockDelegateServiceOK.renewVaultToken(any(VaultConfig.class))).thenReturn(true);
    when(mockDelegateServiceEx.encrypt(
             anyString(), anyString(), anyString(), anyObject(), any(VaultConfig.class), anyObject()))
        .thenThrow(new SecretManagementException("reason"));
    when(mockDelegateServiceEx.encrypt(anyString(), anyObject(), anyObject()))
        .thenThrow(new SecretManagementException("reason"));
    when(delegateProxyFactory.get(anyObject(), any(SyncTaskContext.class))).thenReturn(mockDelegateServiceOK);
    when(mockDelegateServiceEx.renewVaultToken(any(VaultConfig.class)))
        .thenThrow(new SecretManagementException("reason"));
    FieldUtils.writeField(kmsService, "delegateProxyFactory", delegateProxyFactory, true);
    FieldUtils.writeField(secretManager, "kmsService", kmsService, true);
    FieldUtils.writeField(wingsPersistence, "secretManager", secretManager, true);

    accountId =
        wingsPersistence.save(Account.Builder.anAccount().withAccountName(UUID.randomUUID().toString()).build());
  }

  @Test(expected = SecretManagementException.class)
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testValidateKms() {
    final KmsConfig kmsConfig = getKmsConfig();
    kmsConfig.setAccountId(accountId);
    when(delegateProxyFactory.get(anyObject(), any(SyncTaskContext.class))).thenReturn(mockDelegateServiceOK);
    kmsService.saveKmsConfig(accountId, kmsConfig);
    when(delegateProxyFactory.get(anyObject(), any(SyncTaskContext.class))).thenReturn(mockDelegateServiceEx);
    kmsService.saveKmsConfig(accountId, kmsConfig);
  }
}
