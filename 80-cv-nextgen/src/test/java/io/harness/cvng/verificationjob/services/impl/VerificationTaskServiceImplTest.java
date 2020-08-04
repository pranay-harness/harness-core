package io.harness.cvng.verificationjob.services.impl;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.KAMAL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.inject.Inject;

import io.harness.CvNextGenTest;
import io.harness.category.element.UnitTests;
import io.harness.cvng.beans.DataSourceType;
import io.harness.cvng.client.VerificationManagerService;
import io.harness.cvng.core.beans.CVMonitoringCategory;
import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.core.entities.DataCollectionTask;
import io.harness.cvng.core.entities.SplunkCVConfig;
import io.harness.cvng.core.services.api.CVConfigService;
import io.harness.cvng.core.services.api.DataCollectionTaskService;
import io.harness.cvng.models.VerificationType;
import io.harness.cvng.verificationjob.beans.Sensitivity;
import io.harness.cvng.verificationjob.beans.TestVerificationJobDTO;
import io.harness.cvng.verificationjob.beans.VerificationJobDTO;
import io.harness.cvng.verificationjob.beans.VerificationTaskDTO;
import io.harness.cvng.verificationjob.entities.VerificationTask;
import io.harness.cvng.verificationjob.services.api.VerificationJobService;
import io.harness.cvng.verificationjob.services.api.VerificationTaskService;
import io.harness.rule.Owner;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.util.UUID;

public class VerificationTaskServiceImplTest extends CvNextGenTest {
  @Inject private VerificationJobService verificationJobService;
  @Inject private VerificationTaskService verificationTaskService;
  @Inject private CVConfigService cvConfigService;
  @Mock private VerificationManagerService verificationManagerService;
  @Inject private DataCollectionTaskService dataCollectionTaskService;
  private String accountId;
  private String verificationJobIdentifier;
  private long deploymentStartTimeMs;
  private String connectorId;
  private String dataCollectionTaskId;
  @Before
  public void setup() throws IllegalAccessException {
    MockitoAnnotations.initMocks(this);
    verificationJobIdentifier = generateUuid();
    accountId = generateUuid();
    accountId = generateUuid();
    deploymentStartTimeMs = Instant.parse("2020-07-27T10:44:06.390Z").toEpochMilli();
    connectorId = generateUuid();
    dataCollectionTaskId = generateUuid();
    FieldUtils.writeField(verificationTaskService, "verificationManagerService", verificationManagerService, true);
    when(verificationManagerService.createDeploymentVerificationDataCollectionTask(any(), any(), any(), any()))
        .thenReturn(dataCollectionTaskId);
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testCreate_withInvalidJobIdentifier() {
    assertThatThrownBy(() -> verificationTaskService.create(accountId, newVerificationTask()))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("No Job exists for verificationJobIdentifier: '"
            + "" + verificationJobIdentifier + "'");
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testCreate_withValidJobIdentifier() {
    verificationJobService.upsert(accountId, newVerificationJob());
    VerificationTaskDTO verificationTaskDTO = newVerificationTask();
    String verificationTaskId = verificationTaskService.create(accountId, verificationTaskDTO);
    VerificationTaskDTO saved = verificationTaskService.get(verificationTaskId);
    assertThat(saved.getVerificationJobIdentifier()).isEqualTo(verificationTaskDTO.getVerificationJobIdentifier());
    assertThat(saved.getDeploymentStartTimeMs()).isEqualTo(verificationTaskDTO.getDeploymentStartTimeMs());
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void createDataCollectionTasks_validatePerpetualTaskCreationWithCorrectParams() {
    verificationJobService.upsert(accountId, newVerificationJob());
    cvConfigService.save(newCVConfig());
    String verificationTaskId = verificationTaskService.create(accountId, newVerificationTask());
    VerificationTask verificationTask = verificationTaskService.getVerificationTask(verificationTaskId);
    verificationTaskService.createDataCollectionTasks(verificationTask);
    verify(verificationManagerService)
        .createDeploymentVerificationDataCollectionTask(eq(accountId), eq(verificationTaskId), eq(connectorId),
            eq(getDataCollectionWorkerId(verificationTaskId, connectorId)));
    VerificationTask saved = verificationTaskService.getVerificationTask(verificationTaskId);
    assertThat(saved.getDataCollectionTaskIds()).isEqualTo(Lists.newArrayList(dataCollectionTaskId));
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void createDataCollectionTasks_validateDataCollectionTasksCreation() {
    verificationJobService.upsert(accountId, newVerificationJob());
    cvConfigService.save(newCVConfig());
    String verificationTaskId = verificationTaskService.create(accountId, newVerificationTask());
    VerificationTask verificationTask = verificationTaskService.getVerificationTask(verificationTaskId);
    verificationTaskService.createDataCollectionTasks(verificationTask);
    String workerId = getDataCollectionWorkerId(verificationTaskId, connectorId);
    DataCollectionTask firstTask = dataCollectionTaskService.getNextTask(accountId, workerId).get();
    assertThat(firstTask).isNotNull();
    assertThat(firstTask.getStartTime()).isEqualTo(Instant.parse("2020-07-27T10:44:00Z"));
    assertThat(firstTask.getEndTime()).isEqualTo(Instant.parse("2020-07-27T10:45:00Z"));
  }

  private String getDataCollectionWorkerId(String verificationTaskId, String connectorId) {
    return UUID.nameUUIDFromBytes((verificationTaskId + ":" + connectorId).getBytes(Charsets.UTF_8)).toString();
  }
  private VerificationJobDTO newVerificationJob() {
    TestVerificationJobDTO testVerificationJobDTO = new TestVerificationJobDTO();
    testVerificationJobDTO.setIdentifier(verificationJobIdentifier);
    testVerificationJobDTO.setJobName(generateUuid());
    testVerificationJobDTO.setDataSources(Lists.newArrayList(DataSourceType.SPLUNK));
    testVerificationJobDTO.setBaselineVerificationTaskIdentifier(null);
    testVerificationJobDTO.setSensitivity(Sensitivity.MEDIUM);
    testVerificationJobDTO.setServiceIdentifier(generateUuid());
    testVerificationJobDTO.setEnvIdentifier(generateUuid());
    testVerificationJobDTO.setBaselineVerificationTaskIdentifier(generateUuid());
    testVerificationJobDTO.setDuration("15m");
    return testVerificationJobDTO;
  }

  private CVConfig newCVConfig() {
    SplunkCVConfig cvConfig = new SplunkCVConfig();
    cvConfig.setQuery("exception");
    cvConfig.setServiceInstanceIdentifier("serviceInstanceIdentifier");
    cvConfig.setVerificationType(VerificationType.LOG);
    cvConfig.setAccountId(accountId);
    cvConfig.setConnectorId(connectorId);
    cvConfig.setServiceIdentifier(generateUuid());
    cvConfig.setEnvIdentifier(generateUuid());
    cvConfig.setProjectIdentifier(generateUuid());
    cvConfig.setGroupId("groupId");
    cvConfig.setCategory(CVMonitoringCategory.PERFORMANCE);
    cvConfig.setProductName("productName");
    return cvConfig;
  }

  private VerificationTaskDTO newVerificationTask() {
    return VerificationTaskDTO.builder()
        .verificationJobIdentifier(verificationJobIdentifier)
        .deploymentStartTimeMs(deploymentStartTimeMs)
        .build();
  }
}