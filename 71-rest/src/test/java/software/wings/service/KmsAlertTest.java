package software.wings.service;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.rule.OwnerRule.RAGHU;
import static io.harness.rule.OwnerRule.UTKARSH;
import static org.assertj.core.api.Assertions.assertThat;
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
import io.harness.rule.OwnerRule.Owner;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.alerts.AlertStatus;
import software.wings.beans.Account;
import software.wings.beans.AccountStatus;
import software.wings.beans.KmsConfig;
import software.wings.beans.LicenseInfo;
import software.wings.beans.SyncTaskContext;
import software.wings.beans.VaultConfig;
import software.wings.beans.alert.Alert;
import software.wings.beans.alert.AlertType;
import software.wings.beans.alert.KmsSetupAlert;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.security.SecretManagementException;
import software.wings.service.intfc.AlertService;
import software.wings.service.intfc.security.KmsService;
import software.wings.service.intfc.security.SecretManagementDelegateService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.service.intfc.security.VaultService;

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
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testAlertFiredForVaultRenewal() throws InterruptedException {
    VaultConfig vaultConfig = getVaultConfig();
    vaultConfig.setRenewIntervalHours(1);
    vaultConfig.setRenewedAt(0);
    vaultConfig.setAccountId(accountId);
    when(delegateProxyFactory.get(anyObject(), any(SyncTaskContext.class))).thenReturn(mockDelegateServiceOK);
    vaultService.saveVaultConfig(accountId, vaultConfig);
    when(delegateProxyFactory.get(anyObject(), any(SyncTaskContext.class))).thenReturn(mockDelegateServiceEx);
    vaultService.renewTokens(accountId);

    PageResponse<Alert> alerts = listOpenAlerts(accountId);
    assertThat(alerts).hasSize(1);
    Alert alert = alerts.get(0);
    assertThat(alert.getAccountId()).isEqualTo(accountId);
    assertThat(alert.getType()).isEqualTo(AlertType.InvalidKMS);
    assertThat(alert.getStatus()).isEqualTo(AlertStatus.Open);
    KmsSetupAlert alertData = (KmsSetupAlert) alert.getAlertData();
    assertThat(alertData.getKmsId()).isEqualTo(vaultConfig.getUuid());

    VaultConfig savedVaultConfig = wingsPersistence.get(VaultConfig.class, vaultConfig.getUuid());
    assertThat(savedVaultConfig.getRenewedAt()).isEqualTo(0);

    when(delegateProxyFactory.get(anyObject(), any(SyncTaskContext.class))).thenReturn(mockDelegateServiceOK);
    vaultService.renewTokens(accountId);
    Thread.sleep(2000);
    assertThat(listOpenAlerts(accountId)).isEmpty();
    savedVaultConfig = wingsPersistence.get(VaultConfig.class, vaultConfig.getUuid());
    assertThat(savedVaultConfig.getRenewedAt() > 0).isTrue();

    assertThat(listClosedAlerts(accountId)).hasSize(1);
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

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testNoAlertForInactiveAccount() {
    accountId =
        wingsPersistence.save(Account.Builder.anAccount()
                                  .withAccountName(UUID.randomUUID().toString())
                                  .withLicenseInfo(LicenseInfo.builder().accountStatus(AccountStatus.INACTIVE).build())
                                  .build());

    final KmsConfig kmsConfig = getKmsConfig();
    kmsConfig.setAccountId(accountId);
    when(delegateProxyFactory.get(anyObject(), any(SyncTaskContext.class))).thenReturn(mockDelegateServiceOK);
    kmsService.saveKmsConfig(accountId, kmsConfig);
    when(delegateProxyFactory.get(anyObject(), any(SyncTaskContext.class))).thenReturn(mockDelegateServiceEx);
    vaultService.renewTokens(accountId);
    vaultService.appRoleLogin(accountId);
    PageResponse<Alert> alerts = listOpenAlerts(accountId);
    assertThat(alerts).hasSize(0);
  }

  private PageResponse<Alert> listOpenAlerts(String accountId) {
    return listAlerts(accountId, AlertStatus.Open);
  }

  private PageResponse<Alert> listClosedAlerts(String accountId) {
    return listAlerts(accountId, AlertStatus.Closed);
  }

  private PageResponse<Alert> listAlerts(String accountId, AlertStatus alertStatus) {
    PageRequest<Alert> pageRequest = aPageRequest()
                                         .addFilter("status", Operator.EQ, alertStatus)
                                         .addFilter("accountId", Operator.EQ, accountId)
                                         .build();
    return alertService.list(pageRequest);
  }
}
