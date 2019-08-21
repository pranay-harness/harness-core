package software.wings.service;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.inject.Inject;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.beans.SearchFilter.Operator;
import io.harness.category.element.UnitTests;
import io.harness.exception.KmsOperationException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.alerts.AlertStatus;
import software.wings.beans.Account;
import software.wings.beans.KmsConfig;
import software.wings.beans.SyncTaskContext;
import software.wings.beans.VaultConfig;
import software.wings.beans.alert.Alert;
import software.wings.beans.alert.AlertType;
import software.wings.beans.alert.KmsSetupAlert;
import software.wings.delegatetasks.DelegateProxyFactory;
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
@Slf4j
public class KmsAlertTest extends WingsBaseTest {
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
    when(mockDelegateServiceOK.encrypt(
             anyString(), anyString(), anyString(), anyObject(), any(VaultConfig.class), anyObject()))
        .thenReturn(null);
    when(mockDelegateServiceOK.encrypt(anyString(), anyObject(), anyObject())).thenReturn(null);
    when(mockDelegateServiceOK.renewVaultToken(any(VaultConfig.class))).thenReturn(true);
    when(mockDelegateServiceEx.encrypt(
             anyString(), anyString(), anyString(), anyObject(), any(VaultConfig.class), anyObject()))
        .thenThrow(new KmsOperationException("reason"));
    when(mockDelegateServiceEx.encrypt(anyString(), anyObject(), anyObject()))
        .thenThrow(new KmsOperationException("reason"));
    when(delegateProxyFactory.get(anyObject(), any(SyncTaskContext.class))).thenReturn(mockDelegateServiceOK);
    when(mockDelegateServiceEx.renewVaultToken(any(VaultConfig.class))).thenThrow(new KmsOperationException("reason"));
    FieldUtils.writeField(vaultService, "delegateProxyFactory", delegateProxyFactory, true);
    FieldUtils.writeField(kmsService, "delegateProxyFactory", delegateProxyFactory, true);
    FieldUtils.writeField(secretManager, "kmsService", kmsService, true);
    FieldUtils.writeField(wingsPersistence, "secretManager", secretManager, true);
    FieldUtils.writeField(vaultService, "kmsService", kmsService, true);
    FieldUtils.writeField(secretManager, "vaultService", vaultService, true);

    accountId =
        wingsPersistence.save(Account.Builder.anAccount().withAccountName(UUID.randomUUID().toString()).build());
  }

  @Test
  @Category(UnitTests.class)
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
    assertThat(alertService.list(pageRequest)).isEmpty();

    pageRequest = aPageRequest()
                      .addFilter("status", Operator.EQ, AlertStatus.Closed)
                      .addFilter("accountId", Operator.EQ, accountId)
                      .build();
    assertEquals(1, alertService.list(pageRequest).size());
  }

  @Test
  @Category(UnitTests.class)
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
    assertThat(alertService.list(pageRequest)).isEmpty();
    savedVaultConfig = wingsPersistence.get(VaultConfig.class, vaultConfig.getUuid());
    assertThat(savedVaultConfig.getRenewedAt() > 0).isTrue();

    pageRequest = aPageRequest()
                      .addFilter("status", Operator.EQ, AlertStatus.Closed)
                      .addFilter("accountId", Operator.EQ, accountId)
                      .build();
    assertEquals(1, alertService.list(pageRequest).size());
  }

  @Test
  @Category(UnitTests.class)
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
    assertThat(alertService.list(pageRequest)).isEmpty();

    pageRequest = aPageRequest()
                      .addFilter("status", Operator.EQ, AlertStatus.Closed)
                      .addFilter("accountId", Operator.EQ, accountId)
                      .build();
    assertEquals(1, alertService.list(pageRequest).size());
  }
}
