package io.harness.cvng.activity.source.services.impl;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.PRAVEEN;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CvNextGenTest;
import io.harness.category.element.UnitTests;
import io.harness.cvng.activity.entities.CD10ActivitySource;
import io.harness.cvng.activity.source.services.api.ActivitySourceService;
import io.harness.cvng.activity.source.services.api.CD10ActivitySourceService;
import io.harness.cvng.beans.activity.cd10.CD10ActivitySourceDTO;
import io.harness.cvng.beans.activity.cd10.CD10EnvMappingDTO;
import io.harness.cvng.beans.activity.cd10.CD10ServiceMappingDTO;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import java.util.HashSet;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class CD10ActivitySourceServiceImplTest extends CvNextGenTest {
  private String accountId;
  private String orgIdentifier;
  private String projectIdentifier;
  private String appId;

  @Inject private ActivitySourceService activitySourceService;
  @Inject private CD10ActivitySourceService cd10ActivitySourceService;

  @Before
  public void setUp() throws Exception {
    accountId = generateUuid();
    orgIdentifier = generateUuid();
    projectIdentifier = generateUuid();
    appId = generateUuid();
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category({UnitTests.class})
  public void testGet_accountIdAppId() {
    String activitySourceUUID = activitySourceService.saveActivitySource(
        accountId, orgIdentifier, projectIdentifier, createActivitySourceDTO());

    CD10ActivitySourceDTO activitySourceDTO =
        (CD10ActivitySourceDTO) cd10ActivitySourceService.get(accountId, projectIdentifier, appId);

    assertThat(activitySourceDTO).isNotNull();
    assertThat(activitySourceDTO.getEnvMappings().size()).isEqualTo(10);
    assertThat(activitySourceDTO.getServiceMappings().size()).isEqualTo(10);
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category({UnitTests.class})
  public void testGet_serviceEnv() {
    String activitySourceUUID = activitySourceService.saveActivitySource(
        accountId, orgIdentifier, projectIdentifier, createActivitySourceDTO());
    CD10ActivitySource activitySource =
        (CD10ActivitySource) activitySourceService.getActivitySource(activitySourceUUID);
    String envId = activitySource.getEnvMappings().iterator().next().getEnvId();
    String envIdentifier = activitySource.getEnvMappings().iterator().next().getEnvIdentifier();
    String serviceId = activitySource.getServiceMappings().iterator().next().getServiceId();
    String serviceIdentidier = activitySource.getServiceMappings().iterator().next().getServiceIdentifier();
    CD10ActivitySourceDTO activitySourceDTO =
        (CD10ActivitySourceDTO) cd10ActivitySourceService.get(accountId, projectIdentifier, appId, envId, serviceId);

    assertThat(activitySourceDTO).isNotNull();
    assertThat(activitySourceDTO.getEnvMappings().size()).isEqualTo(1);
    assertThat(activitySourceDTO.getServiceMappings().size()).isEqualTo(1);

    String envIdFromDto = activitySourceDTO.getEnvMappings().iterator().next().getEnvId();
    String envIdentifierFromDto = activitySourceDTO.getEnvMappings().iterator().next().getEnvIdentifier();
    String serviceIdFromDto = activitySourceDTO.getServiceMappings().iterator().next().getServiceId();
    String serviceIdentidierFromDto = activitySourceDTO.getServiceMappings().iterator().next().getServiceIdentifier();

    assertThat(envId).isEqualTo(envIdFromDto);
    assertThat(serviceId).isEqualTo(serviceIdFromDto);
    assertThat(envIdentifier).isEqualTo(envIdentifierFromDto);
    assertThat(serviceIdentidier).isEqualTo(serviceIdentidierFromDto);
  }

  private CD10ActivitySourceDTO createActivitySourceDTO() {
    String identifier = generateUuid();
    Set<CD10EnvMappingDTO> cd10EnvMappingDTOS = new HashSet<>();
    Set<CD10ServiceMappingDTO> cd10ServiceMappingDTOS = new HashSet<>();
    for (int i = 0; i < 10; i++) {
      cd10EnvMappingDTOS.add(createEnvMapping(appId, generateUuid(), generateUuid()));
      cd10ServiceMappingDTOS.add(createServiceMapping(appId, generateUuid(), generateUuid()));
    }
    CD10ActivitySourceDTO cd10ActivitySourceDTO = CD10ActivitySourceDTO.builder()
                                                      .identifier(identifier)
                                                      .name("some-name")
                                                      .envMappings(cd10EnvMappingDTOS)
                                                      .serviceMappings(cd10ServiceMappingDTOS)
                                                      .build();

    return cd10ActivitySourceDTO;
  }

  private CD10EnvMappingDTO createEnvMapping(String appId, String envId, String envIdentifier) {
    return CD10EnvMappingDTO.builder().appId(appId).envId(envId).envIdentifier(envIdentifier).build();
  }

  private CD10ServiceMappingDTO createServiceMapping(String appId, String serviceId, String serviceIdentifier) {
    return CD10ServiceMappingDTO.builder()
        .appId(appId)
        .serviceId(serviceId)
        .serviceIdentifier(serviceIdentifier)
        .build();
  }
}