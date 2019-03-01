package software.wings.service;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static software.wings.beans.DelegateTask.DEFAULT_ASYNC_CALL_TIMEOUT;

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

import java.net.SocketException;
import java.util.List;

/**
 * Created by rsingh on 7/6/18.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({SecretManagementDelegateServiceImpl.class, Http.class, VaultRestClientFactory.class})
public class NewRelicTaskScopeValidationTest {
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
    PowerMockito.when(Http.connectableHttpUrl(vaultConfig.getVaultUrl())).thenReturn(true);
    when(vaultRestClient.writeSecret(anyString(), anyString(), anyString())).thenReturn(true);

    validate(true);
  }

  @Test
  public void validationVaultUnReachable() throws Exception {
    PowerMockito.when(Http.connectableHttpUrl(newRelicUrl)).thenReturn(true);
    Call<Void> restCall = Mockito.mock(Call.class);
    doThrow(new SocketException("can't reach to vault")).when(restCall).execute();

    validate(false);
  }

  @Test
  public void validationNewRelicUnReachable() throws Exception {
    PowerMockito.when(Http.connectableHttpUrl(newRelicUrl)).thenReturn(false);
    Call<Void> restCall = Mockito.mock(Call.class);
    when(restCall.execute()).thenReturn(Response.success(null));

    validate(false);
  }

  private void validate(boolean shouldBeValidated) {
    NewRelicValidation newRelicValidation = new NewRelicValidation(generateUuid(),
        DelegateTask.Builder.aDelegateTask()
            .async(true)
            .parameters(new Object[] {NewRelicConfig.builder()
                                          .newRelicUrl(newRelicUrl)
                                          .accountId(generateUuid())
                                          .apiKey(generateUuid().toCharArray())
                                          .build(),
                NewRelicDataCollectionInfo.builder()
                    .encryptedDataDetails(
                        Lists.newArrayList(EncryptedDataDetail.builder().encryptionConfig(vaultConfig).build()))
                    .build()})
            .timeout(DEFAULT_ASYNC_CALL_TIMEOUT)
            .build(),
        null);
    List<DelegateConnectionResult> validate = newRelicValidation.validate();
    assertEquals(1, validate.size());
    DelegateConnectionResult delegateConnectionResult = validate.get(0);
    assertEquals(newRelicUrl, delegateConnectionResult.getCriteria());
    assertEquals(shouldBeValidated, delegateConnectionResult.isValidated());
  }
}