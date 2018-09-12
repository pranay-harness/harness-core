package software.wings.service;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.mockito.internal.util.reflection.Whitebox.setInternalState;
import static software.wings.dl.PageRequest.PageRequestBuilder.aPageRequest;

import com.google.inject.Inject;

import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
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
    when(mockDelegateServiceOK.encrypt(anyString(), anyString(), anyString(), anyObject(), anyObject(), anyObject()))
        .thenReturn(null);
    when(mockDelegateServiceOK.encrypt(anyString(), anyObject(), anyObject())).thenReturn(null);
    when(mockDelegateServiceOK.renewVaultToken(any(VaultConfig.class))).thenReturn(true);
    when(mockDelegateServiceEx.encrypt(anyString(), anyString(), anyString(), anyObject(), anyObject(), anyObject()))
        .thenThrow(new WingsException(ErrorCode.KMS_OPERATION_ERROR));
    when(mockDelegateServiceEx.encrypt(anyString(), anyObject(), anyObject()))
        .thenThrow(new WingsException(ErrorCode.KMS_OPERATION_ERROR));
    when(delegateProxyFactory.get(anyObject(), any(SyncTaskContext.class))).thenReturn(mockDelegateServiceOK);
    when(mockDelegateServiceEx.renewVaultToken(any(VaultConfig.class)))
        .thenThrow(new WingsException(ErrorCode.KMS_OPERATION_ERROR));
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
    PageRequest<Alert> pageRequest = aPageRequest()
                                         .addFilter("status", Operator.EQ, AlertStatus.Open)
                                         .addFilter("accountId", Operator.EQ, accountId)
                                         .build();
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

    pageRequest = aPageRequest()
                      .addFilter("status", Operator.EQ, AlertStatus.Closed)
                      .addFilter("accountId", Operator.EQ, accountId)
                      .build();
    assertEquals(1, alertService.list(pageRequest).size());
  }

  @Test
  public void testAlertFiredForVaultRenewal() throws IOException, InterruptedException {
    VaultConfig vaultConfig = getVaultConfig();
    vaultConfig.setRenewIntervalHours(1);
    vaultConfig.setRenewedAt(0);
    vaultConfig.setAuthToken(UUID.randomUUID().toString());
    vaultConfig.setAccountId(accountId);
    when(delegateProxyFactory.get(anyObject(), any(SyncTaskContext.class))).thenReturn(mockDelegateServiceOK);
    vaultService.saveVaultConfig(accountId, vaultConfig);
    when(delegateProxyFactory.get(anyObject(), any(SyncTaskContext.class))).thenReturn(mockDelegateServiceEx);
    vaultService.renewTokens(accountId);
    PageRequest<Alert> pageRequest = aPageRequest()
                                         .addFilter("status", Operator.EQ, AlertStatus.Open)
                                         .addFilter("accountId", Operator.EQ, accountId)
                                         .build();
    PageResponse<Alert> alerts = alertService.list(pageRequest);
    assertEquals(1, alerts.size());
    Alert alert = alerts.get(0);
    assertEquals(accountId, alert.getAccountId());
    assertEquals(AlertType.InvalidKMS, alert.getType());
    assertEquals(AlertStatus.Open, alert.getStatus());
    KmsSetupAlert alertData = (KmsSetupAlert) alert.getAlertData();
    assertEquals(vaultConfig.getUuid(), alertData.getKmsId());

    VaultConfig savedVaultConfig = wingsPersistence.get(VaultConfig.class, vaultConfig.getUuid());
    assertEquals(0, savedVaultConfig.getRenewedAt());

    when(delegateProxyFactory.get(anyObject(), any(SyncTaskContext.class))).thenReturn(mockDelegateServiceOK);
    vaultService.renewTokens(accountId);
    Thread.sleep(2000);
    assertEquals(0, alertService.list(pageRequest).size());
    savedVaultConfig = wingsPersistence.get(VaultConfig.class, vaultConfig.getUuid());
    assertTrue(savedVaultConfig.getRenewedAt() > 0);

    pageRequest = aPageRequest()
                      .addFilter("status", Operator.EQ, AlertStatus.Closed)
                      .addFilter("accountId", Operator.EQ, accountId)
                      .build();
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
    PageRequest<Alert> pageRequest = aPageRequest()
                                         .addFilter("status", Operator.EQ, AlertStatus.Open)
                                         .addFilter("accountId", Operator.EQ, accountId)
                                         .build();
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

    pageRequest = aPageRequest()
                      .addFilter("status", Operator.EQ, AlertStatus.Closed)
                      .addFilter("accountId", Operator.EQ, accountId)
                      .build();
    assertEquals(1, alertService.list(pageRequest).size());
  }

  private VaultConfig getVaultConfig() {
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
