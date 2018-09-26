package software.wings.service;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.common.collect.Lists;

import io.harness.network.Http;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import retrofit2.Call;
import retrofit2.Response;
import software.wings.WingsBaseTest;
import software.wings.beans.DelegateTask;
import software.wings.beans.NewRelicConfig;
import software.wings.beans.VaultConfig;
import software.wings.delegatetasks.validation.DelegateConnectionResult;
import software.wings.delegatetasks.validation.NewRelicValidation;
import software.wings.helpers.ext.vault.VaultRestClient;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.newrelic.NewRelicDataCollectionInfo;
import software.wings.service.impl.security.SecretManagementDelegateServiceImpl;
import software.wings.service.impl.security.VaultSecretValue;
import software.wings.settings.SettingValue.SettingVariableTypes;

import java.io.IOException;
import java.net.SocketException;
import java.util.List;

/**
 * Created by rsingh on 7/6/18.
 */
@RunWith(PowerMockRunner.class)
public class NewRelicTaskScopeValidationTest extends WingsBaseTest {
  @Mock private VaultRestClient vaultRestClient;
  private String newRelicUrl = "https://api.newrelic.com";

  @Before
  public void setup() throws IOException {
    initMocks(this);
  }

  @PrepareForTest({SecretManagementDelegateServiceImpl.class, Http.class})
  @Test
  public void validationVaultReachable() throws Exception {
    PowerMockito.mockStatic(Http.class);
    PowerMockito.when(Http.connectableHttpUrl(newRelicUrl)).thenReturn(true);
    Call<Void> restCall = Mockito.mock(Call.class);
    VaultConfig vaultConfig =
        VaultConfig.builder().vaultUrl(generateUuid()).accountId(generateUuid()).authToken(generateUuid()).build();
    when(restCall.execute()).thenReturn(Response.success(null));
    when(vaultRestClient.writeSecret(
             anyString(), anyString(), any(SettingVariableTypes.class), any(VaultSecretValue.class)))
        .thenReturn(restCall);
    PowerMockito.mockStatic(SecretManagementDelegateServiceImpl.class);
    PowerMockito.when(SecretManagementDelegateServiceImpl.getVaultRestClient(vaultConfig)).thenReturn(vaultRestClient);

    NewRelicValidation newRelicValidation = new NewRelicValidation(generateUuid(),
        DelegateTask.Builder.aDelegateTask()
            .withParameters(new Object[] {NewRelicConfig.builder()
                                              .newRelicUrl(newRelicUrl)
                                              .accountId(generateUuid())
                                              .apiKey(generateUuid().toCharArray())
                                              .build(),
                NewRelicDataCollectionInfo.builder()
                    .encryptedDataDetails(
                        Lists.newArrayList(EncryptedDataDetail.builder().encryptionConfig(vaultConfig).build()))
                    .build()})
            .build(),
        null);
    List<DelegateConnectionResult> validate = newRelicValidation.validate();
    assertEquals(1, validate.size());
    DelegateConnectionResult delegateConnectionResult = validate.get(0);
    assertEquals(newRelicUrl, delegateConnectionResult.getCriteria());
    assertTrue(delegateConnectionResult.isValidated());
  }

  @PrepareForTest({SecretManagementDelegateServiceImpl.class, Http.class})
  @Test
  public void validationVaultUnReachable() throws Exception {
    PowerMockito.mockStatic(Http.class);
    PowerMockito.when(Http.connectableHttpUrl(newRelicUrl)).thenReturn(true);
    Call<Void> restCall = Mockito.mock(Call.class);
    VaultConfig vaultConfig =
        VaultConfig.builder().vaultUrl(generateUuid()).accountId(generateUuid()).authToken(generateUuid()).build();
    doThrow(new SocketException("can't reach to vault")).when(restCall).execute();
    when(vaultRestClient.writeSecret(
             anyString(), anyString(), any(SettingVariableTypes.class), any(VaultSecretValue.class)))
        .thenReturn(restCall);
    PowerMockito.mockStatic(SecretManagementDelegateServiceImpl.class);
    PowerMockito.when(SecretManagementDelegateServiceImpl.getVaultRestClient(vaultConfig)).thenReturn(vaultRestClient);

    NewRelicValidation newRelicValidation = new NewRelicValidation(generateUuid(),
        DelegateTask.Builder.aDelegateTask()
            .withParameters(new Object[] {NewRelicConfig.builder()
                                              .newRelicUrl(newRelicUrl)
                                              .accountId(generateUuid())
                                              .apiKey(generateUuid().toCharArray())
                                              .build(),
                NewRelicDataCollectionInfo.builder()
                    .encryptedDataDetails(
                        Lists.newArrayList(EncryptedDataDetail.builder().encryptionConfig(vaultConfig).build()))
                    .build()})
            .build(),
        null);
    List<DelegateConnectionResult> validate = newRelicValidation.validate();
    assertEquals(1, validate.size());
    DelegateConnectionResult delegateConnectionResult = validate.get(0);
    assertEquals(newRelicUrl, delegateConnectionResult.getCriteria());
    assertFalse(delegateConnectionResult.isValidated());
  }

  @PrepareForTest({SecretManagementDelegateServiceImpl.class, Http.class})
  @Test
  public void validationNewRelicUnReachable() throws Exception {
    PowerMockito.mockStatic(Http.class);
    PowerMockito.when(Http.connectableHttpUrl(newRelicUrl)).thenReturn(false);
    Call<Void> restCall = Mockito.mock(Call.class);
    VaultConfig vaultConfig =
        VaultConfig.builder().vaultUrl(generateUuid()).accountId(generateUuid()).authToken(generateUuid()).build();
    when(restCall.execute()).thenReturn(Response.success(null));
    when(vaultRestClient.writeSecret(
             anyString(), anyString(), any(SettingVariableTypes.class), any(VaultSecretValue.class)))
        .thenReturn(restCall);
    PowerMockito.mockStatic(SecretManagementDelegateServiceImpl.class);
    PowerMockito.when(SecretManagementDelegateServiceImpl.getVaultRestClient(vaultConfig)).thenReturn(vaultRestClient);

    NewRelicValidation newRelicValidation = new NewRelicValidation(generateUuid(),
        DelegateTask.Builder.aDelegateTask()
            .withParameters(new Object[] {NewRelicConfig.builder()
                                              .newRelicUrl(newRelicUrl)
                                              .accountId(generateUuid())
                                              .apiKey(generateUuid().toCharArray())
                                              .build(),
                NewRelicDataCollectionInfo.builder()
                    .encryptedDataDetails(
                        Lists.newArrayList(EncryptedDataDetail.builder().encryptionConfig(vaultConfig).build()))
                    .build()})
            .build(),
        null);
    List<DelegateConnectionResult> validate = newRelicValidation.validate();
    assertEquals(1, validate.size());
    DelegateConnectionResult delegateConnectionResult = validate.get(0);
    assertEquals(newRelicUrl, delegateConnectionResult.getCriteria());
    assertFalse(delegateConnectionResult.isValidated());
  }
}
