package software.wings.service;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.delegate.beans.TaskData.DEFAULT_ASYNC_CALL_TIMEOUT;
import static io.harness.rule.OwnerRule.RAGHU;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.common.collect.Lists;

import io.harness.CategoryTest;
import io.harness.beans.DelegateTask;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.TaskData;
import io.harness.network.Http;
import io.harness.rule.Owner;
import io.harness.security.encryption.EncryptedDataDetail;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import retrofit2.Call;
import retrofit2.Response;
import software.wings.beans.NewRelicConfig;
import software.wings.beans.VaultConfig;
import software.wings.delegatetasks.validation.DelegateConnectionResult;
import software.wings.delegatetasks.validation.NewRelicValidation;
import software.wings.helpers.ext.vault.VaultRestClient;
import software.wings.helpers.ext.vault.VaultRestClientFactory;
import software.wings.service.impl.newrelic.NewRelicDataCollectionInfo;
import software.wings.service.impl.security.SecretManagementDelegateServiceImpl;

import java.net.SocketException;
import java.util.List;

/**
 * Created by rsingh on 7/6/18.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({SecretManagementDelegateServiceImpl.class, Http.class, VaultRestClientFactory.class})
public class NewRelicTaskScopeValidationTest extends CategoryTest {
  @Mock private VaultRestClient vaultRestClient;
  private String newRelicUrl = "https://api.newrelic.com";

  private VaultConfig vaultConfig;

  @Before
  public void setup() {
    initMocks(this);
    PowerMockito.mockStatic(Http.class);
    PowerMockito.mockStatic(VaultRestClientFactory.class);
    PowerMockito.mockStatic(SecretManagementDelegateServiceImpl.class);

    vaultConfig = VaultConfig.builder().vaultUrl(generateUuid()).authToken(generateUuid()).build();
    vaultConfig.setAccountId(generateUuid());
    PowerMockito.when(VaultRestClientFactory.create(vaultConfig)).thenReturn(vaultRestClient);
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void validationVaultReachable() throws Exception {
    PowerMockito.when(Http.connectableHttpUrl(newRelicUrl)).thenReturn(true);
    PowerMockito.when(Http.connectableHttpUrl(vaultConfig.getVaultUrl())).thenReturn(true);
    when(vaultRestClient.writeSecret(anyString(), anyString(), anyString(), anyString())).thenReturn(true);

    validate(true);
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void validationVaultUnReachable() throws Exception {
    PowerMockito.when(Http.connectableHttpUrl(newRelicUrl)).thenReturn(true);
    Call<Void> restCall = Mockito.mock(Call.class);
    doThrow(new SocketException("can't reach to vault")).when(restCall).execute();

    validate(false);
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void validationNewRelicUnReachable() throws Exception {
    PowerMockito.when(Http.connectableHttpUrl(newRelicUrl)).thenReturn(false);
    Call<Void> restCall = Mockito.mock(Call.class);
    when(restCall.execute()).thenReturn(Response.success(null));

    validate(false);
  }

  private void validate(boolean shouldBeValidated) {
    NewRelicValidation newRelicValidation = new NewRelicValidation(generateUuid(),
        DelegateTask.builder()
            .async(true)
            .data(TaskData.builder()
                      .parameters(new Object[] {NewRelicConfig.builder()
                                                    .newRelicUrl(newRelicUrl)
                                                    .accountId(generateUuid())
                                                    .apiKey(generateUuid().toCharArray())
                                                    .build(),
                          NewRelicDataCollectionInfo.builder()
                              .encryptedDataDetails(Lists.newArrayList(
                                  EncryptedDataDetail.builder().encryptionConfig(vaultConfig).build()))
                              .build()})
                      .timeout(DEFAULT_ASYNC_CALL_TIMEOUT)
                      .build())
            .build(),
        null);
    List<DelegateConnectionResult> validate = newRelicValidation.validate();
    assertThat(validate).hasSize(1);
    DelegateConnectionResult delegateConnectionResult = validate.get(0);
    assertThat(delegateConnectionResult.getCriteria()).isEqualTo(newRelicUrl);
    assertThat(delegateConnectionResult.isValidated()).isEqualTo(shouldBeValidated);
  }
}