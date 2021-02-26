package io.harness.cvng.core.utils;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.RAGHU;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import io.harness.CvNextGenTestBase;
import io.harness.category.element.UnitTests;
import io.harness.cvng.client.NextGenClient;
import io.harness.cvng.client.NextGenService;
import io.harness.cvng.client.NextGenServiceImpl.EntityKey;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.ng.core.environment.dto.EnvironmentResponseDTO;
import io.harness.ng.core.service.dto.ServiceResponseDTO;
import io.harness.rule.Owner;

import com.google.common.cache.CacheLoader;
import com.google.inject.Inject;
import java.io.IOException;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mockito.Mockito;
import retrofit2.Call;
import retrofit2.Response;

public class NextGenServiceImplTest extends CvNextGenTestBase {
  @Inject private NextGenService nextGenService;
  @Mock private NextGenClient nextGenClient;
  private String accountId;
  private String orgIdentifier;
  private String projectIdentifier;

  @Before
  public void setup() throws Exception {
    accountId = generateUuid();
    orgIdentifier = generateUuid();
    projectIdentifier = generateUuid();
    FieldUtils.writeField(nextGenService, "nextGenClient", nextGenClient, true);
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testGetEnvironment() throws IOException {
    Call<ResponseDTO<EnvironmentResponseDTO>> call = Mockito.mock(Call.class);
    when(call.clone()).thenReturn(call);
    String envIdentifier = generateUuid();
    when(nextGenClient.getEnvironment(envIdentifier, accountId, orgIdentifier, projectIdentifier)).thenReturn(call);
    when(call.execute())
        .thenReturn(Response.success(
            ResponseDTO.newResponse(EnvironmentResponseDTO.builder().identifier(envIdentifier).name("env").build())));
    EnvironmentResponseDTO environment =
        nextGenService.getEnvironment(accountId, orgIdentifier, projectIdentifier, envIdentifier);
    assertThat(environment).isNotNull();
    assertThat(environment.getIdentifier()).isEqualTo(envIdentifier);
    assertThat(environment.getName()).isEqualTo("env");

    final String newEnvIdentifier = generateUuid();
    when(nextGenClient.getEnvironment(newEnvIdentifier, accountId, orgIdentifier, projectIdentifier)).thenReturn(call);
    when(call.execute()).thenReturn(Response.success(ResponseDTO.newResponse(null)));
    assertThatThrownBy(
        () -> nextGenService.getEnvironment(accountId, orgIdentifier, projectIdentifier, newEnvIdentifier))
        .isInstanceOf(CacheLoader.InvalidCacheLoadException.class)
        .hasMessage("CacheLoader returned null for key "
            + EntityKey.builder()
                  .accountId(accountId)
                  .orgIdentifier(orgIdentifier)
                  .projectIdentifier(projectIdentifier)
                  .entityIdentifier(newEnvIdentifier)
                  .build()
            + ".");
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testGetService() throws IOException {
    Call<ResponseDTO<ServiceResponseDTO>> call = Mockito.mock(Call.class);
    when(call.clone()).thenReturn(call);
    String serviceIdentifier = generateUuid();
    when(nextGenClient.getService(serviceIdentifier, accountId, orgIdentifier, projectIdentifier)).thenReturn(call);
    when(call.execute())
        .thenReturn(Response.success(ResponseDTO.newResponse(
            ServiceResponseDTO.builder().build().builder().identifier(serviceIdentifier).name("service").build())));
    ServiceResponseDTO service =
        nextGenService.getService(accountId, orgIdentifier, projectIdentifier, serviceIdentifier);
    assertThat(service).isNotNull();
    assertThat(service.getIdentifier()).isEqualTo(serviceIdentifier);
    assertThat(service.getName()).isEqualTo("service");

    final String newServiceIdentifier = generateUuid();
    when(nextGenClient.getService(newServiceIdentifier, accountId, orgIdentifier, projectIdentifier)).thenReturn(call);
    when(call.execute()).thenReturn(Response.success(ResponseDTO.newResponse(null)));
    assertThatThrownBy(
        () -> nextGenService.getService(accountId, orgIdentifier, projectIdentifier, newServiceIdentifier))
        .isInstanceOf(CacheLoader.InvalidCacheLoadException.class)
        .hasMessage("CacheLoader returned null for key "
            + EntityKey.builder()
                  .accountId(accountId)
                  .orgIdentifier(orgIdentifier)
                  .projectIdentifier(projectIdentifier)
                  .entityIdentifier(newServiceIdentifier)
                  .build()
            + ".");
  }
}
