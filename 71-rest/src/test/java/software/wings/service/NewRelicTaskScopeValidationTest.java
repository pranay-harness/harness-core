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
import software.wings.helpers.ext.vault.VaultRestClientFactory;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.newrelic.NewRelicDataCollectionInfo;
import software.wings.service.impl.security.SecretManagementDelegateServiceImpl;
import software.wings.settings.SettingValue.SettingVariableTypes;

import java.net.SocketException;
import java.util.List;

/**
 * Created by rsingh on 7/6/18.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({SecretManagementDelegateServiceImpl.class, Http.class, VaultRestClientFactory.class})
public class NewRelicTaskScopeValidationTest extends WingsBaseTest {
  @Mock private VaultRestClient vaultRestClient;
  private String newRelicUrl = "https://api.newrelic.com";

  private VaultConfig vaultConfig;

  @Before
  public void setup() {
    initMocks(this);
    PowerMockito.mockStatic(Http.class);
    PowerMockito.mockStatic(VaultRestClientFactory.class);
    PowerMockito.mockStatic(SecretManagementDelegateServiceImpl.class);

    vaultConfig =
        VaultConfig.builder().vaultUrl(generateUuid()).accountId(generateUuid()).authToken(generateUuid()).build();
    PowerMockito.when(VaultRestClientFactory.create(vaultConfig)).thenReturn(vaultRestClient);
  }

  @Test
  public void validationVaultReachable() throws Exception {
    PowerMockito.when(Http.connectableHttpUrl(newRelicUrl)).thenReturn(true);
    when(vaultRestClient.writeSecret(anyString(), anyString(), any(SettingVariableTypes.class), any(String.class)))
        .thenReturn(true);

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

  @Test
  public void validationVaultUnReachable() throws Exception {
    PowerMockito.when(Http.connectableHttpUrl(newRelicUrl)).thenReturn(true);
    Call<Void> restCall = Mockito.mock(Call.class);
    doThrow(new SocketException("can't reach to vault")).when(restCall).execute();

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

  @Test
  public void validationNewRelicUnReachable() throws Exception {
    PowerMockito.when(Http.connectableHttpUrl(newRelicUrl)).thenReturn(false);
    Call<Void> restCall = Mockito.mock(Call.class);
    when(restCall.execute()).thenReturn(Response.success(null));

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
