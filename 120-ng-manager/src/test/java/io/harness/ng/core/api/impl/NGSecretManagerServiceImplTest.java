package io.harness.ng.core.api.impl;

import static io.github.benas.randombeans.api.EnhancedRandom.random;
import static io.harness.rule.OwnerRule.PHOENIKX;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rest.RestResponse;
import io.harness.rule.Owner;
import io.harness.secretmanagerclient.dto.SecretManagerConfigDTO;
import io.harness.secretmanagerclient.dto.VaultConfigDTO;
import io.harness.secretmanagerclient.remote.SecretManagerClient;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import retrofit2.Call;
import retrofit2.Response;

import java.io.IOException;
import java.util.List;

public class NGSecretManagerServiceImplTest extends CategoryTest {
  @Mock SecretManagerClient secretManagerClient;
  private final String ACCOUNT_IDENTIFIER = "ACCOUNT_ID";
  private final String ORG_IDENTIFIER = "ACCOUNT_ID";
  private final String PROJECT_IDENTIFIER = "ACCOUNT_ID";
  private final String KMS_IDENTIFIER = "KMS_ID";

  private NGSecretManagerServiceImpl ngSecretManagerService;

  @Before
  public void doSetup() {
    MockitoAnnotations.initMocks(this);
    ngSecretManagerService = new NGSecretManagerServiceImpl(secretManagerClient);
  }

  @Test
  @Owner(developers = PHOENIKX)
  @Category(UnitTests.class)
  public void testCreateSecretManager() throws IOException {
    SecretManagerConfigDTO dto = random(VaultConfigDTO.class);
    Call<RestResponse<SecretManagerConfigDTO>> request = mock(Call.class);

    when(secretManagerClient.createSecretManager(any())).thenReturn(request);
    when(request.execute()).thenReturn(Response.success(new RestResponse<>(dto)));

    SecretManagerConfigDTO savedDTO = ngSecretManagerService.createSecretManager(random(VaultConfigDTO.class));

    Assertions.assertThat(savedDTO).isEqualTo(dto);
  }

  @Test
  @Owner(developers = PHOENIKX)
  @Category(UnitTests.class)
  public void testDeleteSecretManager() throws IOException {
    Call<RestResponse<Boolean>> request = mock(Call.class);

    when(secretManagerClient.deleteSecretManager(any(), any(), any(), any())).thenReturn(request);
    when(request.execute()).thenReturn(Response.success(new RestResponse<>(true)));

    boolean success = ngSecretManagerService.deleteSecretManager(
        KMS_IDENTIFIER, ACCOUNT_IDENTIFIER, ORG_IDENTIFIER, PROJECT_IDENTIFIER);

    Assertions.assertThat(success).isTrue();
  }

  @Test
  @Owner(developers = PHOENIKX)
  @Category(UnitTests.class)
  public void testListSecretManagers() throws IOException {
    List<SecretManagerConfigDTO> secretManagerConfigs = Lists.newArrayList(random(VaultConfigDTO.class));
    Call<RestResponse<List<SecretManagerConfigDTO>>> request = mock(Call.class);

    when(secretManagerClient.listSecretManagers(any(), any(), any())).thenReturn(request);
    when(request.execute()).thenReturn(Response.success(new RestResponse<>(secretManagerConfigs)));

    List<SecretManagerConfigDTO> secretManagerConfigList =
        ngSecretManagerService.listSecretManagers(ACCOUNT_IDENTIFIER, ORG_IDENTIFIER, PROJECT_IDENTIFIER);

    Assertions.assertThat(secretManagerConfigList).isNotEmpty();
    Assertions.assertThat(secretManagerConfigList).isEqualTo(secretManagerConfigs);
  }
}
