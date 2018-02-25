package software.wings.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.mockito.internal.util.reflection.Whitebox.setInternalState;
import static software.wings.dl.PageRequest.PageRequestBuilder.aPageRequest;

import com.google.inject.Inject;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;
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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.UUID;

/**
 * Created by rsingh on 11/3/17.
 */
@Ignore
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
  public void setup() throws IOException {
    initMocks(this);
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
    URL resource = getClass().getClassLoader().getResource("vault_token.txt");

    if (resource == null) {
      System.out.println("reading vault token from environment variable");
    } else {
      System.out.println("reading vault token from file");
      VAULT_TOKEN = FileUtils.readFileToString(new File(resource.getFile()), Charset.defaultCharset());
    }
    if (VAULT_TOKEN.endsWith("\n")) {
      VAULT_TOKEN = VAULT_TOKEN.replaceAll("\n", "");
    }
    System.out.println("VAULT_TOKEN: " + VAULT_TOKEN);
    return VaultConfig.builder()
        .vaultUrl("http://127.0.0.1:8200")
        .authToken(VAULT_TOKEN)
        .name("myVault")
        .isDefault(true)
        .build();
  }

  private KmsConfig getKmsConfig() {
    final KmsConfig kmsConfig = new KmsConfig();
    kmsConfig.setName("myKms");
    kmsConfig.setDefault(true);
    kmsConfig.setKmsArn("arn:aws:kms:us-east-1:830767422336:key/6b64906a-b7ab-4f69-8159-e20fef1f204d");
    kmsConfig.setAccessKey("AKIAJLEKM45P4PO5QUFQ");
    kmsConfig.setSecretKey("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");
    return kmsConfig;
  }
}
