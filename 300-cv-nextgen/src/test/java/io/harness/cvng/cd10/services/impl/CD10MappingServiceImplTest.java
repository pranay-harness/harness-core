package io.harness.cvng.cd10.services.impl;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.KAMAL;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CvNextGenTest;
import io.harness.category.element.UnitTests;
import io.harness.cvng.cd10.beans.CD10EnvMappingDTO;
import io.harness.cvng.cd10.beans.CD10MappingsDTO;
import io.harness.cvng.cd10.beans.CD10ServiceMappingDTO;
import io.harness.cvng.cd10.services.api.CD10MappingService;
import io.harness.rule.Owner;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import java.util.HashSet;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class CD10MappingServiceImplTest extends CvNextGenTest {
  @Inject private CD10MappingService cd10MappingService;
  private String accountId;
  private String orgIdentifier;
  private String projectIdentifier;
  @Before
  public void setup() {
    accountId = generateUuid();
    orgIdentifier = generateUuid();
    projectIdentifier = generateUuid();
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testCreate_withListAPI() {
    Set<CD10EnvMappingDTO> cd10EnvMappingDTOS = new HashSet<>();
    Set<String> appIds = new HashSet<>();
    for (int i = 0; i < 10; i++) {
      String appId = generateUuid();
      appIds.add(appId);
      cd10EnvMappingDTOS.add(createEnvMapping(appId, generateUuid(), generateUuid()));
    }
    CD10MappingsDTO cd10MappingsDTO = CD10MappingsDTO.builder()
                                          .orgIdentifier(orgIdentifier)
                                          .projectIdentifier(projectIdentifier)
                                          .accountId(accountId)
                                          .envMappings(cd10EnvMappingDTOS)
                                          .build();
    cd10MappingService.create(accountId, cd10MappingsDTO);
    assertThat(cd10MappingService.list(accountId, orgIdentifier, projectIdentifier)).isEqualTo(cd10MappingsDTO);
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testCreate_withListAPIDelete() {
    Set<CD10ServiceMappingDTO> cd10serviceMappingDTOS = new HashSet<>();
    Set<String> appIds = new HashSet<>();
    for (int i = 0; i < 10; i++) {
      String appId = generateUuid();
      appIds.add(appId);
      cd10serviceMappingDTOS.add(createServiceMapping(appId, generateUuid(), generateUuid()));
    }
    CD10MappingsDTO cd10MappingsDTO = CD10MappingsDTO.builder()
                                          .orgIdentifier(orgIdentifier)
                                          .projectIdentifier(projectIdentifier)
                                          .accountId(accountId)
                                          .serviceMappings(cd10serviceMappingDTOS)
                                          .build();
    cd10MappingService.create(accountId, cd10MappingsDTO);
    assertThat(cd10MappingService.list(accountId, orgIdentifier, projectIdentifier)).isEqualTo(cd10MappingsDTO);
    CD10MappingsDTO emptyMapping = CD10MappingsDTO.builder()
                                       .orgIdentifier(orgIdentifier)
                                       .projectIdentifier(projectIdentifier)
                                       .accountId(accountId)
                                       .serviceMappings(new HashSet<>())
                                       .build();
    cd10MappingService.create(accountId, emptyMapping);

    assertThat(cd10MappingService.list(accountId, orgIdentifier, projectIdentifier).getEnvMappings()).isEmpty();
    assertThat(cd10MappingService.list(accountId, orgIdentifier, projectIdentifier).getServiceMappings()).isEmpty();
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testCreate_withListAPIDeleteOneApp() {
    CD10MappingsDTO cd10MappingsDTO =
        CD10MappingsDTO.builder()
            .orgIdentifier(orgIdentifier)
            .projectIdentifier(projectIdentifier)
            .accountId(accountId)
            .envMappings(Sets.newHashSet(
                CD10EnvMappingDTO.builder().appId("app1").envIdentifier("envIdentifier1").envId("env1").build(),
                CD10EnvMappingDTO.builder().appId("app2").envIdentifier("envIdentifier2").envId("env2").build(),
                CD10EnvMappingDTO.builder().appId("app1").envIdentifier("envIdentifier3").envId("env3").build(),
                CD10EnvMappingDTO.builder().appId("app2").envIdentifier("envIdentifier14").envId("env4").build()))
            .build();
    cd10MappingService.create(accountId, cd10MappingsDTO);
    assertThat(cd10MappingService.list(accountId, orgIdentifier, projectIdentifier)).isEqualTo(cd10MappingsDTO);
    CD10MappingsDTO cd10MappingsDTOOnlyWithApp1 =
        CD10MappingsDTO.builder()
            .orgIdentifier(orgIdentifier)
            .projectIdentifier(projectIdentifier)
            .accountId(accountId)
            .envMappings(Sets.newHashSet(
                CD10EnvMappingDTO.builder().appId("app1").envIdentifier("envIdentifier1").envId("env1").build()))
            .serviceMappings(Sets.newHashSet(CD10ServiceMappingDTO.builder()
                                                 .appId("app1")
                                                 .serviceId("serviceId1")
                                                 .serviceIdentifier("serviceIdetifier1")
                                                 .build()))
            .build();
    cd10MappingService.create(accountId, cd10MappingsDTOOnlyWithApp1);

    assertThat(cd10MappingService.list(accountId, orgIdentifier, projectIdentifier))
        .isEqualTo(cd10MappingsDTOOnlyWithApp1);
  }
  private CD10EnvMappingDTO createEnvMapping(String appId, String envId, String envIdentifier) {
    return CD10EnvMappingDTO.builder().appId(appId).envId(envId).envIdentifier(envIdentifier).build();
  }

  private CD10ServiceMappingDTO createServiceMapping(String appId, String serviceId, String serviceIdetifier) {
    return CD10ServiceMappingDTO.builder()
        .appId(appId)
        .serviceId(serviceId)
        .serviceIdentifier(serviceIdetifier)
        .build();
  }
}