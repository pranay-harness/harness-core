package io.harness.cvng.activity.source.services.impl;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.KAMAL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.CvNextGenTestBase;
import io.harness.category.element.UnitTests;
import io.harness.cvng.activity.beans.Cd10ValidateMappingParams;
import io.harness.cvng.activity.entities.CD10ActivitySource;
import io.harness.cvng.activity.source.services.api.ActivitySourceService;
import io.harness.cvng.activity.source.services.api.CD10ActivitySourceService;
import io.harness.cvng.beans.activity.cd10.CD10ActivitySourceDTO;
import io.harness.cvng.beans.activity.cd10.CD10EnvMappingDTO;
import io.harness.cvng.beans.activity.cd10.CD10ServiceMappingDTO;
import io.harness.cvng.verificationjob.entities.VerificationJob;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import java.util.HashSet;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class CD10ActivitySourceServiceImplTest extends CvNextGenTestBase {
  private String accountId;
  private String orgIdentifier;
  private String projectIdentifier;
  private String appId;

  @Inject private ActivitySourceService activitySourceService;
  @Inject private CD10ActivitySourceService cd10ActivitySourceService;

  @Before
  public void setUp() {
    accountId = generateUuid();
    orgIdentifier = generateUuid();
    projectIdentifier = generateUuid();
    appId = generateUuid();
  }

  @Test
  @Owner(developers = KAMAL)
  @Category({UnitTests.class})
  public void getNextGenEnvIdentifier() {
    String activitySourceUUID = activitySourceService.create(accountId, createActivitySourceDTO());
    CD10ActivitySource cd10ActivitySource =
        (CD10ActivitySource) activitySourceService.getActivitySource(activitySourceUUID);
    cd10ActivitySource.getEnvMappings().forEach(envMapping
        -> assertThat(cd10ActivitySourceService.getNextGenEnvIdentifier(
                          accountId, orgIdentifier, projectIdentifier, appId, envMapping.getEnvId()))
               .isEqualTo(envMapping.getEnvIdentifier()));
    assertThatThrownBy(()
                           -> cd10ActivitySourceService.getNextGenEnvIdentifier(
                               accountId, orgIdentifier, projectIdentifier, appId, generateUuid()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("No envId to envIdentifier mapping exists");
  }

  @Test(expected = Test.None.class)
  @Owner(developers = KAMAL)
  @Category({UnitTests.class})
  public void testValidateMapping_mappingMatchesServiceEnvValues() {
    String activitySourceUUID = activitySourceService.create(accountId, createActivitySourceDTO());
    CD10ActivitySource cd10ActivitySource =
        (CD10ActivitySource) activitySourceService.getActivitySource(activitySourceUUID);

    CD10ServiceMappingDTO serviceMappingDTO = cd10ActivitySource.getServiceMappings().iterator().next();
    CD10EnvMappingDTO envMappingDTO = cd10ActivitySource.getEnvMappings().iterator().next();
    cd10ActivitySourceService.validateMapping(
        Cd10ValidateMappingParams.builder()
            .cd10AppId(appId)
            .cd10ServiceId(serviceMappingDTO.getServiceId())
            .cd10EnvId(envMappingDTO.getEnvId())
            .activitySourceIdentifier(cd10ActivitySource.getIdentifier())
            .accountId(accountId)
            .projectIdentifier(projectIdentifier)
            .orgIdentifier(orgIdentifier)
            .serviceIdentifier(VerificationJob.RuntimeParameter.builder()
                                   .isRuntimeParam(false)
                                   .value(serviceMappingDTO.getServiceIdentifier())
                                   .build())
            .environmentIdentifier(VerificationJob.RuntimeParameter.builder()
                                       .isRuntimeParam(false)
                                       .value(envMappingDTO.getEnvIdentifier())
                                       .build())
            .build());
  }

  @Test
  @Owner(developers = KAMAL)
  @Category({UnitTests.class})
  public void testValidateMapping_failWithMismatch() {
    String activitySourceUUID = activitySourceService.create(accountId, createActivitySourceDTO());
    CD10ActivitySource cd10ActivitySource =
        (CD10ActivitySource) activitySourceService.getActivitySource(activitySourceUUID);

    CD10ServiceMappingDTO serviceMappingDTO = cd10ActivitySource.getServiceMappings().iterator().next();
    CD10EnvMappingDTO envMappingDTO = cd10ActivitySource.getEnvMappings().iterator().next();
    assertThatThrownBy(
        ()
            -> cd10ActivitySourceService.validateMapping(
                Cd10ValidateMappingParams.builder()
                    .cd10AppId(appId)
                    .cd10ServiceId(serviceMappingDTO.getServiceId())
                    .cd10EnvId(envMappingDTO.getEnvId())
                    .activitySourceIdentifier(cd10ActivitySource.getIdentifier())
                    .accountId(accountId)
                    .projectIdentifier(projectIdentifier)
                    .orgIdentifier(orgIdentifier)
                    .serviceIdentifier(
                        VerificationJob.RuntimeParameter.builder().isRuntimeParam(false).value(generateUuid()).build())
                    .environmentIdentifier(VerificationJob.RuntimeParameter.builder()
                                               .isRuntimeParam(false)
                                               .value(envMappingDTO.getEnvIdentifier())
                                               .build())
                    .build()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Next gen Service identifier does not match CD 1.0 service mapping");

    assertThatThrownBy(
        ()
            -> cd10ActivitySourceService.validateMapping(
                Cd10ValidateMappingParams.builder()
                    .cd10AppId(appId)
                    .cd10ServiceId(serviceMappingDTO.getServiceId())
                    .cd10EnvId(envMappingDTO.getEnvId())
                    .activitySourceIdentifier(cd10ActivitySource.getIdentifier())
                    .accountId(accountId)
                    .projectIdentifier(projectIdentifier)
                    .orgIdentifier(orgIdentifier)
                    .serviceIdentifier(VerificationJob.RuntimeParameter.builder()
                                           .isRuntimeParam(false)
                                           .value(serviceMappingDTO.getServiceIdentifier())
                                           .build())
                    .environmentIdentifier(
                        VerificationJob.RuntimeParameter.builder().isRuntimeParam(false).value(generateUuid()).build())
                    .build()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Next gen env identifier does not match CD 1.0 env mappings");
  }

  @Test(expected = Test.None.class)
  @Owner(developers = KAMAL)
  @Category({UnitTests.class})
  public void testValidateMapping_serviceEnvAreRuntimeParams() {
    String activitySourceUUID = activitySourceService.create(accountId, createActivitySourceDTO());
    CD10ActivitySource cd10ActivitySource =
        (CD10ActivitySource) activitySourceService.getActivitySource(activitySourceUUID);

    CD10ServiceMappingDTO serviceMappingDTO = cd10ActivitySource.getServiceMappings().iterator().next();
    CD10EnvMappingDTO envMappingDTO = cd10ActivitySource.getEnvMappings().iterator().next();
    cd10ActivitySourceService.validateMapping(
        Cd10ValidateMappingParams.builder()
            .cd10AppId(appId)
            .cd10ServiceId(serviceMappingDTO.getServiceId())
            .cd10EnvId(envMappingDTO.getEnvId())
            .activitySourceIdentifier(cd10ActivitySource.getIdentifier())
            .accountId(accountId)
            .projectIdentifier(projectIdentifier)
            .orgIdentifier(orgIdentifier)
            .serviceIdentifier(VerificationJob.RuntimeParameter.builder().isRuntimeParam(true).build())
            .environmentIdentifier(VerificationJob.RuntimeParameter.builder().isRuntimeParam(true).build())
            .build());
  }

  @Test
  @Owner(developers = KAMAL)
  @Category({UnitTests.class})
  public void getNextGenEnvIdentifier_noCDMappingPresent() {
    String activitySourceUUID = activitySourceService.create(accountId, createActivitySourceDTO());
    CD10ActivitySource cd10ActivitySource =
        (CD10ActivitySource) activitySourceService.getActivitySource(activitySourceUUID);
    cd10ActivitySource.getEnvMappings().forEach(envMapping
        -> assertThatThrownBy(()
                                  -> cd10ActivitySourceService.getNextGenEnvIdentifier(
                                      accountId, orgIdentifier, "project", appId, envMapping.getEnvId()))
               .isInstanceOf(NullPointerException.class)
               .hasMessage("No CD 1.0 mapping defined for projectIdentifier: project"));
  }

  @Test
  @Owner(developers = KAMAL)
  @Category({UnitTests.class})
  public void getNextGenServiceIdentifier() {
    String activitySourceUUID = activitySourceService.create(accountId, createActivitySourceDTO());
    CD10ActivitySource cd10ActivitySource =
        (CD10ActivitySource) activitySourceService.getActivitySource(activitySourceUUID);
    cd10ActivitySource.getServiceMappings().forEach(serviceMappingDTO
        -> assertThat(cd10ActivitySourceService.getNextGenServiceIdentifier(
                          accountId, orgIdentifier, projectIdentifier, appId, serviceMappingDTO.getServiceId()))
               .isEqualTo(serviceMappingDTO.getServiceIdentifier()));
    assertThatThrownBy(()
                           -> cd10ActivitySourceService.getNextGenServiceIdentifier(
                               accountId, orgIdentifier, projectIdentifier, appId, generateUuid()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("No serviceId to serviceIdentifier mapping exists");
  }

  @Test
  @Owner(developers = KAMAL)
  @Category({UnitTests.class})
  public void getNextGenServiceIdentifier_noCDMappingPresent() {
    String activitySourceUUID = activitySourceService.create(accountId, createActivitySourceDTO());
    CD10ActivitySource cd10ActivitySource =
        (CD10ActivitySource) activitySourceService.getActivitySource(activitySourceUUID);
    cd10ActivitySource.getServiceMappings().forEach(serviceMappingDTO
        -> assertThatThrownBy(()
                                  -> cd10ActivitySourceService.getNextGenServiceIdentifier(
                                      accountId, orgIdentifier, "project", appId, serviceMappingDTO.getServiceId()))
               .isInstanceOf(NullPointerException.class)
               .hasMessage("No CD 1.0 mapping defined for projectIdentifier: project"));
  }

  private CD10ActivitySourceDTO createActivitySourceDTO() {
    String identifier = CD10ActivitySource.HARNESS_CD_10_ACTIVITY_SOURCE_IDENTIFIER;
    Set<CD10EnvMappingDTO> cd10EnvMappingDTOS = new HashSet<>();
    Set<CD10ServiceMappingDTO> cd10ServiceMappingDTOS = new HashSet<>();
    for (int i = 0; i < 4; i++) {
      cd10EnvMappingDTOS.add(createEnvMapping(appId, generateUuid(), generateUuid()));
      cd10ServiceMappingDTOS.add(createServiceMapping(appId, generateUuid(), generateUuid()));
    }
    CD10ActivitySourceDTO cd10ActivitySourceDTO = CD10ActivitySourceDTO.builder()
                                                      .identifier(identifier)
                                                      .name("some-name")
                                                      .orgIdentifier(orgIdentifier)
                                                      .projectIdentifier(projectIdentifier)
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
