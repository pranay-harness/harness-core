package io.harness.cvng.core.utils;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.RAGHU;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import io.harness.CvNextGenTest;
import io.harness.category.element.UnitTests;
import io.harness.cvng.client.NextGenService;
import io.harness.ng.core.environment.dto.EnvironmentResponseDTO;
import io.harness.ng.core.service.dto.ServiceResponseDTO;
import io.harness.rule.Owner;

import com.google.common.cache.CacheLoader;
import com.google.inject.Inject;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;

public class EnvironmentServiceCacheTest extends CvNextGenTest {
  @Inject private EnvironmentServiceCache environmentServiceCache;
  @Mock private NextGenService nextGenService;
  private String accountId;
  private String orgIdentifier;
  private String projectIdentifier;

  @Before
  public void setup() throws Exception {
    accountId = generateUuid();
    orgIdentifier = generateUuid();
    projectIdentifier = generateUuid();
    FieldUtils.writeField(environmentServiceCache, "nextGenService", nextGenService, true);
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testGetEnvironment() {
    String envIdentifier = generateUuid();
    when(nextGenService.getEnvironment(envIdentifier, accountId, orgIdentifier, projectIdentifier))
        .thenReturn(EnvironmentResponseDTO.builder().identifier(envIdentifier).name("env").build());
    EnvironmentResponseDTO environment =
        environmentServiceCache.getEnvironment(accountId, orgIdentifier, projectIdentifier, envIdentifier);
    assertThat(environment).isNotNull();
    assertThat(environment.getIdentifier()).isEqualTo(envIdentifier);
    assertThat(environment.getName()).isEqualTo("env");

    final String newEnvIdentifier = generateUuid();
    assertThatThrownBy(
        () -> environmentServiceCache.getEnvironment(accountId, orgIdentifier, projectIdentifier, newEnvIdentifier))
        .isInstanceOf(CacheLoader.InvalidCacheLoadException.class)
        .hasMessage("CacheLoader returned null for key "
            + EnvironmentServiceCache.EntityKey.builder()
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
  public void testGetService() {
    String serviceIdentifier = generateUuid();
    when(nextGenService.getService(serviceIdentifier, accountId, orgIdentifier, projectIdentifier))
        .thenReturn(
            ServiceResponseDTO.builder().build().builder().identifier(serviceIdentifier).name("service").build());
    ServiceResponseDTO service =
        environmentServiceCache.getService(accountId, orgIdentifier, projectIdentifier, serviceIdentifier);
    assertThat(service).isNotNull();
    assertThat(service.getIdentifier()).isEqualTo(serviceIdentifier);
    assertThat(service.getName()).isEqualTo("service");

    final String newServiceIdentifier = generateUuid();
    assertThatThrownBy(
        () -> environmentServiceCache.getService(accountId, orgIdentifier, projectIdentifier, newServiceIdentifier))
        .isInstanceOf(CacheLoader.InvalidCacheLoadException.class)
        .hasMessage("CacheLoader returned null for key "
            + EnvironmentServiceCache.EntityKey.builder()
                  .accountId(accountId)
                  .orgIdentifier(orgIdentifier)
                  .projectIdentifier(projectIdentifier)
                  .entityIdentifier(newServiceIdentifier)
                  .build()
            + ".");
  }
}
