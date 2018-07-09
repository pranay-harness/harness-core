package software.wings.service;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.mockito.internal.util.reflection.Whitebox.setInternalState;
import static software.wings.dl.PageRequest.PageRequestBuilder.aPageRequest;

import com.google.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.WingsBaseTest;
import software.wings.alerts.AlertStatus;
import software.wings.beans.Account;
import software.wings.beans.DelegateTask.SyncTaskContext;
import software.wings.beans.KmsConfig;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.VaultConfig;
import software.wings.beans.alert.Alert;
import software.wings.beans.alert.AlertType;
import software.wings.beans.alert.KmsSetupAlert;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.AlertService;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.service.intfc.security.KmsService;
import software.wings.service.intfc.security.SecretManagementDelegateService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.service.intfc.security.VaultService;

import java.io.IOException;
import java.util.UUID;

/**
 * Created by rsingh on 11/3/17.
 */
public class KmsAlertTest extends WingsBaseTest {
  private static final Logger logger = LoggerFactory.getLogger(KmsAlertTest.class);

  private static String VAULT_TOKEN = System.getProperty("vault.token");

  @Inject private VaultService vaultService;
  @Inject private KmsService kmsService;
  @Inject private AlertService alertService;
  @Inject private SecretManager secretManager;
  @Inject private WingsPersistence wingsPersistence;
  @Mock private DelegateProxyFactory delegateProxyFactory;
  @Mock private SecretManagementDelegateService mockDelegateServiceOK;
  @Mock private SecretManagementDelegateService mockDelegateServiceEx;
  private String accountId;

  @Before
  public void setup() throws IOException, NoSuchFieldException, IllegalAccessException {
    initMocks(this);
    setStaticTimeOut(EncryptionService.class, "DECRYPTION_DELEGATE_TASK_TIMEOUT", 100L);
    setStaticTimeOut(EncryptionService.class, "DECRYPTION_DELEGATE_TIMEOUT", 200L);
    when(mockDelegateServiceOK.encrypt(anyString(), anyString(), anyString(), anyObject(), anyObject(), anyObject()))
        .thenReturn(null);
    when(mockDelegateServiceOK.encrypt(anyString(), anyObject(), anyObject())).thenReturn(null);
    when(mockDelegateServiceEx.encrypt(anyString(), anyString(), anyString(), anyObject(), anyObject(), anyObject()))
        .thenThrow(new IOException());
    when(mockDelegateServiceEx.encrypt(anyString(), anyObject(), anyObject())).thenThrow(new IOException());
    when(delegateProxyFactory.get(anyObject(), any(SyncTaskContext.class))).thenReturn(mockDelegateServiceOK);
    setInternalState(vaultService, "delegateProxyFactory", delegateProxyFactory);
    setInternalState(kmsService, "delegateProxyFactory", delegateProxyFactory);
    setInternalState(secretManager, "kmsService", kmsService);
    setInternalState(wingsPersistence, "secretManager", secretManager);
    setInternalState(vaultService, "kmsService", kmsService);
    setInternalState(secretManager, "vaultService", vaultService);

    accountId =
        wingsPersistence.save(Account.Builder.anAccount().withAccountName(UUID.randomUUID().toString()).build());
  }

  @Test
  public void testAlertFiredForVault() throws IOException, InterruptedException {
    VaultConfig vaultConfig = getVaultConfig();
    vaultConfig.setAuthToken(UUID.randomUUID().toString());
    vaultConfig.setAccountId(accountId);
    when(delegateProxyFactory.get(anyObject(), any(SyncTaskContext.class))).thenReturn(mockDelegateServiceOK);
    vaultService.saveVaultConfig(accountId, vaultConfig);
    when(delegateProxyFactory.get(anyObject(), any(SyncTaskContext.class))).thenReturn(mockDelegateServiceEx);
    secretManager.checkAndAlertForInvalidManagers();
    PageRequest<Alert> pageRequest = aPageRequest().addFilter("status", Operator.EQ, AlertStatus.Open).build();
    PageResponse<Alert> alerts = alertService.list(pageRequest);
    assertEquals(1, alerts.size());
    Alert alert = alerts.get(0);
    assertEquals(accountId, alert.getAccountId());
    assertEquals(AlertType.InvalidKMS, alert.getType());
    assertEquals(AlertStatus.Open, alert.getStatus());
    KmsSetupAlert alertData = (KmsSetupAlert) alert.getAlertData();
    assertEquals(vaultConfig.getUuid(), alertData.getKmsId());

    when(delegateProxyFactory.get(anyObject(), any(SyncTaskContext.class))).thenReturn(mockDelegateServiceOK);
    secretManager.checkAndAlertForInvalidManagers();
    Thread.sleep(2000);
    assertEquals(0, alertService.list(pageRequest).size());

    pageRequest = aPageRequest().addFilter("status", Operator.EQ, AlertStatus.Closed).build();
    assertEquals(1, alertService.list(pageRequest).size());
  }

  @Test
  public void testAlertFiredForKms() throws IOException, InterruptedException {
    final KmsConfig kmsConfig = getKmsConfig();
    kmsConfig.setAccountId(accountId);
    when(delegateProxyFactory.get(anyObject(), any(SyncTaskContext.class))).thenReturn(mockDelegateServiceOK);
    kmsService.saveKmsConfig(accountId, kmsConfig);
    when(delegateProxyFactory.get(anyObject(), any(SyncTaskContext.class))).thenReturn(mockDelegateServiceEx);
    secretManager.checkAndAlertForInvalidManagers();
    PageRequest<Alert> pageRequest = aPageRequest().addFilter("status", Operator.EQ, AlertStatus.Open).build();
    PageResponse<Alert> alerts = alertService.list(pageRequest);
    assertEquals(1, alerts.size());
    Alert alert = alerts.get(0);
    assertEquals(accountId, alert.getAccountId());
    assertEquals(AlertType.InvalidKMS, alert.getType());
    assertEquals(AlertStatus.Open, alert.getStatus());
    KmsSetupAlert alertData = (KmsSetupAlert) alert.getAlertData();
    assertEquals(kmsConfig.getUuid(), alertData.getKmsId());

    when(delegateProxyFactory.get(anyObject(), any(SyncTaskContext.class))).thenReturn(mockDelegateServiceOK);
    secretManager.checkAndAlertForInvalidManagers();
    Thread.sleep(2000);
    assertEquals(0, alertService.list(pageRequest).size());

    pageRequest = aPageRequest().addFilter("status", Operator.EQ, AlertStatus.Closed).build();
    assertEquals(1, alertService.list(pageRequest).size());
  }

  private VaultConfig getVaultConfig() throws IOException {
    return VaultConfig.builder()
        .vaultUrl("http://127.0.0.1:8200")
        .authToken(generateUuid())
        .name("myVault")
        .isDefault(true)
        .build();
  }

  private KmsConfig getKmsConfig() {
    final KmsConfig kmsConfig = new KmsConfig();
    kmsConfig.setName("myKms");
    kmsConfig.setDefault(true);
    kmsConfig.setKmsArn(generateUuid());
    kmsConfig.setAccessKey(generateUuid());
    kmsConfig.setSecretKey(generateUuid());
    return kmsConfig;
  }
}
