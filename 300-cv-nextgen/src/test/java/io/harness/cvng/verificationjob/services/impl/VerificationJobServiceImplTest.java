package io.harness.cvng.verificationjob.services.impl;

import static io.harness.cvng.CVConstants.DEFAULT_HEALTH_JOB_ID;
import static io.harness.cvng.verificationjob.CVVerificationJobConstants.RUNTIME_STRING;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.DEEPAK;
import static io.harness.rule.OwnerRule.KAMAL;
import static io.harness.rule.OwnerRule.PRAVEEN;
import static io.harness.rule.OwnerRule.RAGHU;
import static io.harness.rule.OwnerRule.VUK;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CvNextGenTestBase;
import io.harness.category.element.UnitTests;
import io.harness.cvng.beans.DataSourceType;
import io.harness.cvng.beans.job.Sensitivity;
import io.harness.cvng.beans.job.TestVerificationJobDTO;
import io.harness.cvng.beans.job.VerificationJobDTO;
import io.harness.cvng.beans.job.VerificationJobType;
import io.harness.cvng.client.NextGenService;
import io.harness.cvng.core.services.api.CVEventService;
import io.harness.cvng.verificationjob.entities.HealthVerificationJob;
import io.harness.cvng.verificationjob.entities.VerificationJob;
import io.harness.cvng.verificationjob.services.api.VerificationJobService;
import io.harness.ng.core.environment.dto.EnvironmentResponseDTO;
import io.harness.ng.core.service.dto.ServiceResponseDTO;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class VerificationJobServiceImplTest extends CvNextGenTestBase {
  @Mock private NextGenService nextGenService;
  @Mock private CVEventService cvEventService;
  @Inject private HPersistence hPersistence;
  @Inject private VerificationJobService verificationJobService;

  private String identifier;
  private String accountId;
  private String orgIdentifier;
  private String projectIdentifier;

  @Before
  public void setup() throws IllegalAccessException {
    MockitoAnnotations.initMocks(this);
    orgIdentifier = generateUuid();
    projectIdentifier = generateUuid();
    identifier = "test-verification-harness";
    accountId = generateUuid();
    FieldUtils.writeField(verificationJobService, "nextGenService", nextGenService, true);
    FieldUtils.writeField(verificationJobService, "cvEventService", cvEventService, true);
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testUpsert_newJobCreation() {
    VerificationJobDTO verificationJobDTO = createDTO();
    verificationJobService.upsert(accountId, verificationJobDTO);
    VerificationJobDTO inserted = verificationJobService.getVerificationJobDTO(
        accountId, orgIdentifier, projectIdentifier, verificationJobDTO.getIdentifier());

    // url will be newly generated, so wont be present.
    VerificationJob insertedJob = verificationJobService.getVerificationJob(
        accountId, orgIdentifier, projectIdentifier, verificationJobDTO.getIdentifier());
    assertThat(inserted.getVerificationJobUrl()).isEqualTo(insertedJob.getVerificationJobUrl());
    inserted.setVerificationJobUrl(null);
    assertThat(inserted).isEqualTo(verificationJobDTO);
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testUpsert_invalidJob() {
    VerificationJobDTO verificationJobDTO = createDTO();
    verificationJobDTO.setEnvIdentifier(null);
    assertThatThrownBy(() -> verificationJobService.upsert(accountId, verificationJobDTO))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("envIdentifier should not be null");
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testUpsert_updateExisting() {
    VerificationJobDTO verificationJobDTO = createDTO();
    verificationJobService.upsert(accountId, verificationJobDTO);
    VerificationJobDTO inserted = verificationJobService.getVerificationJobDTO(
        accountId, orgIdentifier, projectIdentifier, verificationJobDTO.getIdentifier());
    // url will be newly generated, so wont be present.
    VerificationJob insertedJob = verificationJobService.getVerificationJob(
        accountId, orgIdentifier, projectIdentifier, verificationJobDTO.getIdentifier());
    assertThat(inserted.getVerificationJobUrl()).isEqualTo(insertedJob.getVerificationJobUrl());
    inserted.setVerificationJobUrl(null);
    assertThat(inserted).isEqualTo(verificationJobDTO);
    verificationJobDTO.setEnvIdentifier("updated_env");
    verificationJobService.upsert(accountId, verificationJobDTO);
    VerificationJobDTO updated = verificationJobService.getVerificationJobDTO(
        accountId, orgIdentifier, projectIdentifier, verificationJobDTO.getIdentifier());
    assertThat(updated).isNotEqualTo(inserted);
    assertThat(updated.getVerificationJobUrl()).isEqualTo(insertedJob.getVerificationJobUrl());
    updated.setVerificationJobUrl(null);
    assertThat(updated).isEqualTo(verificationJobDTO);
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testGetVerificationJobDTO_invalidIdentifier() {
    VerificationJobDTO updated =
        verificationJobService.getVerificationJobDTO(accountId, orgIdentifier, projectIdentifier, "invalid");
    assertThat(updated).isEqualTo(null);
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testGetVerificationJobDTO_validIdentifier() {
    VerificationJobDTO verificationJobDTO = createDTO();
    verificationJobService.upsert(accountId, verificationJobDTO);
    VerificationJobDTO updated = verificationJobService.getVerificationJobDTO(
        accountId, orgIdentifier, projectIdentifier, verificationJobDTO.getIdentifier());
    // url will be newly generated, so wont be present. So we will validate that separately.
    VerificationJob inserted = verificationJobService.getVerificationJob(
        accountId, orgIdentifier, projectIdentifier, verificationJobDTO.getIdentifier());
    assertThat(updated.getVerificationJobUrl()).isEqualTo(inserted.getVerificationJobUrl());
    updated.setVerificationJobUrl(null);
    assertThat(updated).isEqualTo(verificationJobDTO);
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testDelete_validIdentifier() {
    VerificationJobDTO verificationJobDTO = createDTO();
    verificationJobService.upsert(accountId, verificationJobDTO);
    verificationJobService.delete(accountId, orgIdentifier, projectIdentifier, verificationJobDTO.getIdentifier());
    assertThat(verificationJobService.getVerificationJobDTO(
                   accountId, orgIdentifier, projectIdentifier, verificationJobDTO.getIdentifier()))
        .isEqualTo(null);

    ArgumentCaptor<VerificationJob> argumentCaptor = ArgumentCaptor.forClass(VerificationJob.class);
    verify(cvEventService, times(1)).sendVerificationJobServiceDeleteEvent(argumentCaptor.capture());
    verify(cvEventService, times(1)).sendVerificationJobEnvironmentDeleteEvent(argumentCaptor.capture());
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void testDelete_dontSendEventsWithRunTimeParams() {
    VerificationJobDTO verificationJobDTO = createDTO();
    verificationJobDTO.setEnvIdentifier(RUNTIME_STRING);
    verificationJobDTO.setServiceIdentifier(RUNTIME_STRING);

    verificationJobService.upsert(accountId, verificationJobDTO);
    verificationJobService.delete(accountId, orgIdentifier, projectIdentifier, verificationJobDTO.getIdentifier());
    assertThat(verificationJobService.getVerificationJobDTO(
                   accountId, orgIdentifier, projectIdentifier, verificationJobDTO.getIdentifier()))
        .isEqualTo(null);

    ArgumentCaptor<VerificationJob> argumentCaptor = ArgumentCaptor.forClass(VerificationJob.class);
    verify(cvEventService, times(0)).sendVerificationJobServiceDeleteEvent(argumentCaptor.capture());
    verify(cvEventService, times(0)).sendVerificationJobEnvironmentDeleteEvent(argumentCaptor.capture());
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testUpsert_newJobCreationWithRuntimeParams() {
    VerificationJobDTO verificationJobDTO = createDTOWithRuntimeParams();
    verificationJobService.upsert(accountId, verificationJobDTO);
    VerificationJobDTO inserted = verificationJobService.getVerificationJobDTO(
        accountId, orgIdentifier, projectIdentifier, verificationJobDTO.getIdentifier());
    // url will be newly generated, so wont be present.
    VerificationJob insertedJob = verificationJobService.getVerificationJob(
        accountId, orgIdentifier, projectIdentifier, verificationJobDTO.getIdentifier());
    assertThat(inserted.getVerificationJobUrl()).isEqualTo(insertedJob.getVerificationJobUrl());
    inserted.setVerificationJobUrl(null);
    assertThat(inserted).isEqualTo(verificationJobDTO);
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testList_empty() {
    assertThat(verificationJobService.list(accountId, generateUuid(), generateUuid(), 0, 2, generateUuid()).isEmpty());
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testList_notEmpty_FilterByVerificationJobNameOnly() {
    VerificationJobDTO verificationJobDTO = createDTOWithRuntimeParams();
    verificationJobService.upsert(accountId, verificationJobDTO);

    mockFilterByEnvAndServiceResponsesDTOs(verificationJobDTO);

    List<VerificationJobDTO> verificationJobDTOList = verificationJobService
                                                          .list(accountId, verificationJobDTO.getProjectIdentifier(),
                                                              verificationJobDTO.getOrgIdentifier(), 0, 10, "job-Name")
                                                          .getContent();

    assertThat(verificationJobDTOList).isNotNull();
    assertThat(verificationJobDTOList.size()).isEqualTo(1);

    assertThat(verificationJobDTOList.get(0).getIdentifier()).isEqualTo("test-verification-harness");
    assertThat(verificationJobDTOList.get(0).getJobName()).isEqualTo("job-Name");

    // With filter call
    verificationJobDTOList = verificationJobService
                                 .list(accountId, verificationJobDTO.getProjectIdentifier(),
                                     verificationJobDTO.getOrgIdentifier(), 0, 10, "job-")
                                 .getContent();
    assertThat(verificationJobDTOList.size()).isEqualTo(1);

    verificationJobDTOList = verificationJobService
                                 .list(accountId, verificationJobDTO.getProjectIdentifier(),
                                     verificationJobDTO.getOrgIdentifier(), 0, 10, "qwert")
                                 .getContent();
    assertThat(verificationJobDTOList.size()).isEqualTo(0);
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testList_noCallNextGenWhenFilterEmpty() {
    VerificationJobDTO verificationJobDTO = createDTOWithoutRuntimeParams();
    verificationJobService.upsert(accountId, verificationJobDTO);

    verificationJobService
        .list(accountId, verificationJobDTO.getProjectIdentifier(), verificationJobDTO.getOrgIdentifier(), 0, 10, null)
        .getContent();
    verify(nextGenService, never()).getEnvironment(anyString(), anyString(), anyString(), anyString());
    verify(nextGenService, never()).getService(anyString(), anyString(), anyString(), anyString());
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void testList_notEmpty_FilterByVerificationAndEnvAndServicesJobNames() {
    VerificationJobDTO verificationJobDTO = createDTOWithoutRuntimeParams();
    verificationJobService.upsert(accountId, verificationJobDTO);

    mockFilterByEnvAndServiceResponsesDTOs(verificationJobDTO);

    List<VerificationJobDTO> verificationJobDTOList = verificationJobService
                                                          .list(accountId, verificationJobDTO.getProjectIdentifier(),
                                                              verificationJobDTO.getOrgIdentifier(), 0, 10, "job-Name")
                                                          .getContent();

    assertThat(verificationJobDTOList).isNotNull();
    assertThat(verificationJobDTOList.size()).isEqualTo(1);

    assertThat(verificationJobDTOList.get(0).getIdentifier()).isEqualTo("test-verification-harness");
    assertThat(verificationJobDTOList.get(0).getJobName()).isEqualTo("job-Name");

    // With filter call
    verificationJobDTOList = verificationJobService
                                 .list(accountId, verificationJobDTO.getProjectIdentifier(),
                                     verificationJobDTO.getOrgIdentifier(), 0, 10, "job-")
                                 .getContent();
    assertThat(verificationJobDTOList.size()).isEqualTo(1);

    verificationJobDTOList = verificationJobService
                                 .list(accountId, verificationJobDTO.getProjectIdentifier(),
                                     verificationJobDTO.getOrgIdentifier(), 0, 10, "qwert")
                                 .getContent();
    assertThat(verificationJobDTOList.size()).isEqualTo(0);

    // With filter call Environment name
    verificationJobDTOList = verificationJobService
                                 .list(accountId, verificationJobDTO.getProjectIdentifier(),
                                     verificationJobDTO.getOrgIdentifier(), 0, 10, "testEnv")
                                 .getContent();
    assertThat(verificationJobDTOList.size()).isEqualTo(1);

    // With filter call Service name
    verificationJobDTOList = verificationJobService
                                 .list(accountId, verificationJobDTO.getProjectIdentifier(),
                                     verificationJobDTO.getOrgIdentifier(), 0, 10, "testSer")
                                 .getContent();
    assertThat(verificationJobDTOList.size()).isEqualTo(1);
  }

  private void mockFilterByEnvAndServiceResponsesDTOs(VerificationJobDTO verificationJobDTO) {
    EnvironmentResponseDTO environmentResponseDTO = EnvironmentResponseDTO.builder()
                                                        .accountId(accountId)
                                                        .projectIdentifier(verificationJobDTO.getProjectIdentifier())
                                                        .orgIdentifier(verificationJobDTO.getOrgIdentifier())
                                                        .name(verificationJobDTO.getEnvIdentifier())
                                                        .build();

    ServiceResponseDTO serviceResponseDTO = ServiceResponseDTO.builder()
                                                .accountId(accountId)
                                                .projectIdentifier(verificationJobDTO.getProjectIdentifier())
                                                .orgIdentifier(verificationJobDTO.getOrgIdentifier())
                                                .name(verificationJobDTO.getServiceIdentifier())
                                                .build();

    when(nextGenService.getEnvironment(accountId, verificationJobDTO.getOrgIdentifier(),
             verificationJobDTO.getProjectIdentifier(), verificationJobDTO.getEnvIdentifier()))
        .thenReturn(environmentResponseDTO);

    when(nextGenService.getService(accountId, verificationJobDTO.getOrgIdentifier(),
             verificationJobDTO.getProjectIdentifier(), verificationJobDTO.getServiceIdentifier()))
        .thenReturn(serviceResponseDTO);
  }

  private VerificationJobDTO createDTO() {
    TestVerificationJobDTO testVerificationJobDTO = new TestVerificationJobDTO();
    testVerificationJobDTO.setIdentifier(identifier);
    testVerificationJobDTO.setJobName(generateUuid());
    testVerificationJobDTO.setDataSources(Lists.newArrayList(DataSourceType.APP_DYNAMICS));
    testVerificationJobDTO.setMonitoringSources(Arrays.asList(generateUuid()));
    testVerificationJobDTO.setBaselineVerificationJobInstanceId(null);
    testVerificationJobDTO.setSensitivity(Sensitivity.MEDIUM.name());
    testVerificationJobDTO.setServiceIdentifier(generateUuid());
    testVerificationJobDTO.setEnvIdentifier(generateUuid());
    testVerificationJobDTO.setBaselineVerificationJobInstanceId(generateUuid());
    testVerificationJobDTO.setDuration("15m");
    testVerificationJobDTO.setProjectIdentifier(projectIdentifier);
    testVerificationJobDTO.setOrgIdentifier(orgIdentifier);
    return testVerificationJobDTO;
  }

  private VerificationJobDTO createDTOWithRuntimeParams() {
    TestVerificationJobDTO testVerificationJobDTO = (TestVerificationJobDTO) createDTO();
    testVerificationJobDTO.setIdentifier(identifier);
    testVerificationJobDTO.setEnvIdentifier(RUNTIME_STRING);
    testVerificationJobDTO.setServiceIdentifier(RUNTIME_STRING);
    testVerificationJobDTO.setJobName("job-Name");
    return testVerificationJobDTO;
  }

  private VerificationJobDTO createDTOWithoutRuntimeParams() {
    TestVerificationJobDTO testVerificationJobDTO = (TestVerificationJobDTO) createDTO();
    testVerificationJobDTO.setIdentifier(identifier);
    testVerificationJobDTO.setEnvIdentifier("testEnv");
    testVerificationJobDTO.setServiceIdentifier("testSer");
    testVerificationJobDTO.setJobName("job-Name");
    return testVerificationJobDTO;
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testdoesAVerificationJobExistsForThisProjectWhenNoJobExists() {
    String orgIdentifier = "orgIdentifier";
    String projectIdentifier = "projectIdentifier";
    boolean doesAVerificationJobExists =
        verificationJobService.doesAVerificationJobExistsForThisProject(accountId, orgIdentifier, projectIdentifier);
    assertThat(doesAVerificationJobExists).isFalse();
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testdoesAVerificationJobExistsForThisProjectWhenAJobExists() {
    String orgIdentifier = "orgIdentifier";
    String projectIdentifier = "projectIdentifier";
    verificationJobService.save(
        createVerificationJob(orgIdentifier, projectIdentifier, "serviceIdentifier", VerificationJobType.HEALTH));
    boolean doesAVerificationJobExists =
        verificationJobService.doesAVerificationJobExistsForThisProject(accountId, orgIdentifier, projectIdentifier);
    assertThat(doesAVerificationJobExists).isTrue();
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void testSaveVerificationJob() {
    String orgIdentifier = "orgIdentifier";
    String projectIdentifier = "projectIdentifier";
    String serviceIdentifier = "serviceIdentifier";

    VerificationJob verificationJob =
        createVerificationJob(orgIdentifier, projectIdentifier, serviceIdentifier, VerificationJobType.HEALTH);

    verificationJobService.save(verificationJob);

    VerificationJob retrieveVerificationJob = hPersistence.get(VerificationJob.class, verificationJob.getUuid());
    assertThat(retrieveVerificationJob).isNotNull();
    assertThat(retrieveVerificationJob.getOrgIdentifier()).isEqualTo("orgIdentifier");
    assertThat(retrieveVerificationJob.getProjectIdentifier()).isEqualTo("projectIdentifier");
    assertThat(retrieveVerificationJob.getServiceIdentifier()).isEqualTo("serviceIdentifier");
    assertThat(retrieveVerificationJob.getJobName()).isEqualTo("job-name");
    assertThat(retrieveVerificationJob.getDuration()).isEqualTo(Duration.ZERO);

    ArgumentCaptor<VerificationJob> argumentCaptor = ArgumentCaptor.forClass(VerificationJob.class);
    verify(cvEventService, times(1)).sendVerificationJobServiceCreateEvent(argumentCaptor.capture());
    verify(cvEventService, times(1)).sendVerificationJobEnvironmentCreateEvent(argumentCaptor.capture());
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void testSaveVerificationJob_dontSendEventsWithRunTimeParams() {
    String orgIdentifier = "orgIdentifier";
    String projectIdentifier = "projectIdentifier";
    String serviceIdentifier = "serviceIdentifier";

    VerificationJob verificationJob = createVerificationJobWithRunTimeParams(
        orgIdentifier, projectIdentifier, serviceIdentifier, VerificationJobType.HEALTH);

    verificationJobService.save(verificationJob);

    VerificationJob retrieveVerificationJob = hPersistence.get(VerificationJob.class, verificationJob.getUuid());
    assertThat(retrieveVerificationJob).isNotNull();
    assertThat(retrieveVerificationJob.getOrgIdentifier()).isEqualTo("orgIdentifier");
    assertThat(retrieveVerificationJob.getProjectIdentifier()).isEqualTo("projectIdentifier");
    assertThat(retrieveVerificationJob.getServiceIdentifier()).isEqualTo("serviceIdentifier");
    assertThat(retrieveVerificationJob.getJobName()).isEqualTo("job-name");
    assertThat(retrieveVerificationJob.getDuration()).isEqualTo(Duration.ZERO);

    ArgumentCaptor<VerificationJob> argumentCaptor = ArgumentCaptor.forClass(VerificationJob.class);
    verify(cvEventService, times(0)).sendVerificationJobServiceCreateEvent(argumentCaptor.capture());
    verify(cvEventService, times(0)).sendVerificationJobEnvironmentCreateEvent(argumentCaptor.capture());
  }

  private VerificationJob createVerificationJob(
      String orgIdentifier, String projectIdentifier, String serviceIdentifier, VerificationJobType type) {
    HealthVerificationJob verificationJob = new HealthVerificationJob();
    verificationJob.setAccountId(accountId);
    verificationJob.setJobName("job-name");
    verificationJob.setIdentifier(generateUuid());
    verificationJob.setOrgIdentifier(orgIdentifier);
    verificationJob.setProjectIdentifier(projectIdentifier);
    verificationJob.setServiceIdentifier(serviceIdentifier, false);
    verificationJob.setEnvIdentifier(generateUuid(), false);
    verificationJob.setDuration(Duration.ZERO);
    return verificationJob;
  }

  private VerificationJob createVerificationJobWithRunTimeParams(
      String orgIdentifier, String projectIdentifier, String serviceIdentifier, VerificationJobType type) {
    HealthVerificationJob verificationJob = new HealthVerificationJob();
    verificationJob.setAccountId(accountId);
    verificationJob.setJobName("job-name");
    verificationJob.setIdentifier(generateUuid());
    verificationJob.setOrgIdentifier(orgIdentifier);
    verificationJob.setProjectIdentifier(projectIdentifier);
    verificationJob.setServiceIdentifier(serviceIdentifier, true);
    verificationJob.setEnvIdentifier(generateUuid(), true);
    verificationJob.setDuration(Duration.ZERO);
    return verificationJob;
  }

  @Test
  @Owner(developers = DEEPAK)
  @Category(UnitTests.class)
  public void testGetNumberOfServicesUndergoingHealthVerification() {
    String orgIdentifier = "orgIdentifier";
    String projectIdentifier = "projectIdentifier";
    verificationJobService.save(
        createVerificationJob(orgIdentifier, projectIdentifier, "serviceIdentifier 1", VerificationJobType.HEALTH));
    verificationJobService.save(
        createVerificationJob(orgIdentifier, projectIdentifier, "serviceIdentifier 2", VerificationJobType.HEALTH));
    int numberOfServices = verificationJobService.getNumberOfServicesUndergoingHealthVerification(
        accountId, orgIdentifier, projectIdentifier);
    assertThat(numberOfServices).isEqualTo(2);
  }
  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testGetOrCreateDefaultHealthVerificationJob_firstTime() {
    String orgIdentifier = "orgIdentifier";
    String projectIdentifier = "projectIdentifier";
    VerificationJob verificationJob =
        verificationJobService.getOrCreateDefaultHealthVerificationJob(accountId, orgIdentifier, projectIdentifier);
    assertThat(verificationJob).isNotNull();
    assertThat(verificationJob.isDefaultJob()).isTrue();
    assertThat(verificationJob.getIdentifier()).isEqualTo(projectIdentifier + "_" + DEFAULT_HEALTH_JOB_ID);
    assertThat(verificationJob.getServiceIdentifier()).isEqualTo("${service}");
    assertThat(verificationJob.getEnvIdentifier()).isEqualTo("${environment}");
    assertThat(verificationJob.getDuration().toMinutes()).isEqualTo(Duration.ofMinutes(15).toMinutes());
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testGetOrCreateDefaultHealthVerificationJob_secondTime() {
    String orgIdentifier = "orgIdentifier";
    String projectIdentifier = "projectIdentifier";
    VerificationJob verificationJob =
        verificationJobService.getOrCreateDefaultHealthVerificationJob(accountId, orgIdentifier, projectIdentifier);
    VerificationJob verificationJobSecond =
        verificationJobService.getOrCreateDefaultHealthVerificationJob(accountId, orgIdentifier, projectIdentifier);

    assertThat(verificationJob.getCreatedAt()).isEqualTo(verificationJobSecond.getCreatedAt());
    assertThat(verificationJob.getLastUpdatedAt()).isEqualTo(verificationJobSecond.getLastUpdatedAt());
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testGetByUrl() {
    VerificationJobDTO verificationJobDTO = createDTOWithRuntimeParams();
    verificationJobService.upsert(accountId, verificationJobDTO);
    VerificationJob inserted = verificationJobService.getVerificationJob(
        accountId, orgIdentifier, projectIdentifier, verificationJobDTO.getIdentifier());

    VerificationJob byUrl = verificationJobService.getByUrl(accountId, inserted.getVerificationJobUrl());
    assertThat(byUrl.getUuid()).isEqualTo(inserted.getUuid());
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testGetDTOByUrl_nonRuntimeParam() {
    VerificationJobDTO verificationJobDTO = createDTOWithoutRuntimeParams();
    mockFilterByEnvAndServiceResponsesDTOs(verificationJobDTO);

    verificationJobService.upsert(accountId, verificationJobDTO);
    VerificationJob inserted = verificationJobService.getVerificationJob(
        accountId, orgIdentifier, projectIdentifier, verificationJobDTO.getIdentifier());
    VerificationJobDTO byUrl = verificationJobService.getDTOByUrl(accountId, inserted.getVerificationJobUrl());
    assertThat(byUrl.getServiceName()).isEqualTo(inserted.getServiceIdentifier());
    assertThat(byUrl.getServiceIdentifier()).isEqualTo(inserted.getServiceIdentifier());
    assertThat(byUrl.getEnvName()).isEqualTo(inserted.getEnvIdentifier());
    assertThat(byUrl.getEnvIdentifier()).isEqualTo(inserted.getEnvIdentifier());
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testGetDTOByUrl_withRuntimeParam() {
    VerificationJobDTO verificationJobDTO = createDTOWithRuntimeParams();
    mockFilterByEnvAndServiceResponsesDTOs(verificationJobDTO);

    verificationJobService.upsert(accountId, verificationJobDTO);
    VerificationJob inserted = verificationJobService.getVerificationJob(
        accountId, orgIdentifier, projectIdentifier, verificationJobDTO.getIdentifier());

    VerificationJobDTO byUrl = verificationJobService.getDTOByUrl(accountId, inserted.getVerificationJobUrl());
    assertThat(byUrl.getServiceName()).isEqualTo(null);
    assertThat(byUrl.getServiceIdentifier()).isEqualTo(inserted.getServiceIdentifier());
    assertThat(byUrl.getEnvName()).isEqualTo(null);
    assertThat(byUrl.getEnvIdentifier()).isEqualTo(inserted.getEnvIdentifier());
  }

  @Test(expected = NullPointerException.class)
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testGetByUrl_nullAccount() {
    VerificationJobDTO verificationJobDTO = createDTOWithRuntimeParams();
    verificationJobService.upsert(accountId, verificationJobDTO);
    VerificationJob inserted = verificationJobService.getVerificationJob(
        accountId, orgIdentifier, projectIdentifier, verificationJobDTO.getIdentifier());

    VerificationJob byUrl = verificationJobService.getByUrl(null, inserted.getVerificationJobUrl());
  }

  @Test(expected = NullPointerException.class)
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testGetByUrl_nullUrl() {
    VerificationJobDTO verificationJobDTO = createDTOWithRuntimeParams();
    verificationJobService.upsert(accountId, verificationJobDTO);
    VerificationJob inserted = verificationJobService.getVerificationJob(
        accountId, orgIdentifier, projectIdentifier, verificationJobDTO.getIdentifier());

    VerificationJob byUrl = verificationJobService.getByUrl(accountId, null);
  }
}
