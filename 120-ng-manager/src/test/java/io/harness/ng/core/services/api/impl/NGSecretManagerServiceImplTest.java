package io.harness.ng.core.services.api.impl;

import static io.github.benas.randombeans.api.EnhancedRandom.random;
import static io.harness.rule.OwnerRule.PHOENIKX;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;

import io.harness.category.element.UnitTests;
import io.harness.ng.core.BaseTest;
import io.harness.ng.core.remote.client.rest.SecretManagerClient;
import io.harness.rest.RestResponse;
import io.harness.rule.Owner;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import retrofit2.Call;
import retrofit2.Response;
import software.wings.beans.SecretManagerConfig;
import software.wings.beans.VaultConfig;

import java.io.IOException;
import java.util.List;

public class NGSecretManagerServiceImplTest extends BaseTest {
  @Mock SecretManagerClient secretManagerClient;
  private final String ACCOUNT_ID = "ACCOUNT_ID";
  private final String KMS_ID = "KMS_ID";

  private NGSecretManagerServiceImpl ngSecretManagerService;

  @Before
  public void doSetup() {
    ngSecretManagerService = new NGSecretManagerServiceImpl(secretManagerClient);
  }

  @Test
  @Owner(developers = PHOENIKX)
  @Category(UnitTests.class)
  public void testCreateSecretManager() throws IOException {
    String kmsId = randomAlphabetic(20);
    Call<RestResponse<String>> request = mock(Call.class);

    when(secretManagerClient.createOrUpdateSecretManager(any(), any())).thenReturn(request);
    when(request.execute()).thenReturn(Response.success(new RestResponse<>(kmsId)));

    String savedKmsId = ngSecretManagerService.saveOrUpdateSecretManager(ACCOUNT_ID, random(VaultConfig.class));

    Assertions.assertThat(savedKmsId).isEqualTo(kmsId);
  }

  @Test
  @Owner(developers = PHOENIKX)
  @Category(UnitTests.class)
  public void testDeleteSecretManager() throws IOException {
    Call<RestResponse<Boolean>> request = mock(Call.class);

    when(secretManagerClient.deleteSecretManager(any(), any())).thenReturn(request);
    when(request.execute()).thenReturn(Response.success(new RestResponse<>(true)));

    boolean success = ngSecretManagerService.deleteSecretManager(ACCOUNT_ID, KMS_ID);

    Assertions.assertThat(success).isTrue();
  }

  @Test
  @Owner(developers = PHOENIKX)
  @Category(UnitTests.class)
  public void testListSecretManagers() throws IOException {
    List<SecretManagerConfig> secretManagerConfigs = Lists.newArrayList(random(VaultConfig.class));
    Call<RestResponse<List<SecretManagerConfig>>> request = mock(Call.class);

    when(secretManagerClient.getSecretManagersForAccount(any())).thenReturn(request);
    when(request.execute()).thenReturn(Response.success(new RestResponse<>(secretManagerConfigs)));

    List<SecretManagerConfig> secretManagerConfigList = ngSecretManagerService.listSecretManagers(ACCOUNT_ID);

    Assertions.assertThat(secretManagerConfigList).isNotEmpty();
    Assertions.assertThat(secretManagerConfigList).isEqualTo(secretManagerConfigs);
  }
}
