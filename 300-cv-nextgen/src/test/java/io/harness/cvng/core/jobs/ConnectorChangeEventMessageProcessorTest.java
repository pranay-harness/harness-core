package io.harness.cvng.core.jobs;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.KAMAL;
import static io.harness.rule.OwnerRule.VUK;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CvNextGenTestBase;
import io.harness.category.element.UnitTests;
import io.harness.cvng.activity.entities.KubernetesActivitySource;
import io.harness.cvng.activity.source.services.api.ActivitySourceService;
import io.harness.cvng.activity.source.services.api.KubernetesActivitySourceService;
import io.harness.cvng.beans.CVMonitoringCategory;
import io.harness.cvng.beans.DataSourceType;
import io.harness.cvng.beans.activity.KubernetesActivitySourceDTO;
import io.harness.cvng.beans.activity.KubernetesActivitySourceDTO.KubernetesActivitySourceConfig;
import io.harness.cvng.beans.job.Sensitivity;
import io.harness.cvng.beans.job.TestVerificationJobDTO;
import io.harness.cvng.beans.job.VerificationJobDTO;
import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.core.entities.MonitoringSourcePerpetualTask;
import io.harness.cvng.core.entities.SplunkCVConfig;
import io.harness.cvng.core.services.api.CVConfigService;
import io.harness.cvng.core.services.api.DataCollectionTaskService;
import io.harness.cvng.core.services.api.MonitoringSourcePerpetualTaskService;
import io.harness.cvng.core.services.api.VerificationTaskService;
import io.harness.cvng.models.VerificationType;
import io.harness.cvng.verificationjob.entities.VerificationJob;
import io.harness.cvng.verificationjob.entities.VerificationJobInstance;
import io.harness.cvng.verificationjob.services.api.VerificationJobInstanceService;
import io.harness.cvng.verificationjob.services.api.VerificationJobService;
import io.harness.encryption.Scope;
import io.harness.eventsframework.entity_crud.EntityChangeDTO;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.protobuf.StringValue;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public class ConnectorChangeEventMessageProcessorTest extends CvNextGenTestBase {
  @Inject private ConnectorChangeEventMessageProcessor connectorChangeEventMessageProcessor;
  @Inject private CVConfigService cvConfigService;
  @Mock private DataCollectionTaskService dataCollectionTaskService;
  @Mock private KubernetesActivitySourceService kubernetesActivitySourceService;
  @Inject private VerificationJobService verificationJobService;
  @Inject private VerificationJobInstanceService verificationJobInstanceService;
  @Inject private VerificationTaskService verificationTaskService;
  @Inject private ActivitySourceService activitySourceService;
  @Inject private MonitoringSourcePerpetualTaskService monitoringSourcePerpetualTaskService;
  @Inject private HPersistence hPersistence;

  private String accountIdentifier;
  private String orgIdentifier;
  private String projectIdentifier;
  private String connectorIdentifier;
  private long deploymentStartTimeMs;

  @Before
  public void setup() throws IllegalAccessException {
    MockitoAnnotations.initMocks(this);
    FieldUtils.writeField(
        connectorChangeEventMessageProcessor, "dataCollectionTaskService", dataCollectionTaskService, true);
    FieldUtils.writeField(
        connectorChangeEventMessageProcessor, "kubernetesActivitySourceService", kubernetesActivitySourceService, true);
    accountIdentifier = generateUuid();
    orgIdentifier = generateUuid();
    projectIdentifier = generateUuid();
    connectorIdentifier = "connectorIdentifier";
    deploymentStartTimeMs = System.currentTimeMillis();
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testProcessUpdateAction_resetLiveMonitoringPerpetualTask() throws IllegalAccessException {
    MonitoringSourcePerpetualTaskService monitoringSourcePerpetualTaskService =
        Mockito.mock(MonitoringSourcePerpetualTaskService.class);
    MonitoringSourcePerpetualTask monitoringSourcePerpetualTask = MonitoringSourcePerpetualTask.builder()
                                                                      .accountId(accountIdentifier)
                                                                      .orgIdentifier(orgIdentifier)
                                                                      .projectIdentifier(projectIdentifier)
                                                                      .connectorIdentifier(connectorIdentifier)
                                                                      .monitoringSourceIdentifier(generateUuid())
                                                                      .perpetualTaskId(generateUuid())
                                                                      .build();
    when(monitoringSourcePerpetualTaskService.listByConnectorIdentifier(
             accountIdentifier, orgIdentifier, projectIdentifier, connectorIdentifier, Scope.PROJECT))
        .thenReturn(Lists.newArrayList(monitoringSourcePerpetualTask));

    FieldUtils.writeField(connectorChangeEventMessageProcessor, "monitoringSourcePerpetualTaskService",
        monitoringSourcePerpetualTaskService, true);
    connectorChangeEventMessageProcessor.processUpdateAction(
        EntityChangeDTO.newBuilder()
            .setAccountIdentifier(StringValue.newBuilder().setValue(accountIdentifier).build())
            .setOrgIdentifier(StringValue.newBuilder().setValue(orgIdentifier).build())
            .setProjectIdentifier(StringValue.newBuilder().setValue(projectIdentifier).build())
            .setIdentifier(StringValue.newBuilder().setValue(connectorIdentifier).build())
            .build());
    verify(monitoringSourcePerpetualTaskService, times(1))
        .resetLiveMonitoringPerpetualTask(monitoringSourcePerpetualTask);
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void testProcessUpdateAction_resetLiveMonitoringPerpetualTaskForKubernetesActivitySource() {
    KubernetesActivitySourceDTO kubernetesActivitySourceDTO = createKubernetesActivitySourceDTO();

    String kubernetesSourceId = activitySourceService.saveActivitySource(
        accountIdentifier, orgIdentifier, projectIdentifier, kubernetesActivitySourceDTO);

    KubernetesActivitySource kubernetesActivitySource =
        (KubernetesActivitySource) activitySourceService.getActivitySource(kubernetesSourceId);

    assertThat(kubernetesActivitySource).isNotNull();

    connectorChangeEventMessageProcessor.processUpdateAction(
        EntityChangeDTO.newBuilder()
            .setAccountIdentifier(StringValue.newBuilder().setValue(accountIdentifier).build())
            .setOrgIdentifier(StringValue.newBuilder().setValue(orgIdentifier).build())
            .setProjectIdentifier(StringValue.newBuilder().setValue(projectIdentifier).build())
            .setIdentifier(StringValue.newBuilder().setValue(connectorIdentifier).build())
            .build());

    verify(kubernetesActivitySourceService, times(1))
        .findByConnectorIdentifier(
            accountIdentifier, orgIdentifier, projectIdentifier, connectorIdentifier, Scope.PROJECT);
  }

  private KubernetesActivitySourceDTO createKubernetesActivitySourceDTO() {
    return KubernetesActivitySourceDTO.builder()
        .identifier(generateUuid())
        .name("some-name")
        .connectorIdentifier(connectorIdentifier)
        .activitySourceConfigs(Sets.newHashSet(KubernetesActivitySourceConfig.builder()
                                                   .serviceIdentifier(generateUuid())
                                                   .envIdentifier(generateUuid())
                                                   .namespace(generateUuid())
                                                   .workloadName(generateUuid())
                                                   .build()))
        .build();
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testProcessUpdateAction_verificationJobInstance() throws IllegalAccessException {
    CVConfig cvConfig = createCVConfig();
    cvConfigService.save(cvConfig);
    VerificationJobInstance verificationJobInstance1 = createVerificationJobInstance(
        cvConfig.getUuid(), "devVerificationJobInstance", "dev", VerificationJobInstance.ExecutionStatus.RUNNING);
    createVerificationJobInstance(
        cvConfig.getUuid(), "prodVerificationJobInstance1", "prod", VerificationJobInstance.ExecutionStatus.QUEUED);
    createVerificationJobInstance(
        cvConfig.getUuid(), "prodVerificationJobInstance2", "prod", VerificationJobInstance.ExecutionStatus.FAILED);
    verificationJobInstanceService = spy(verificationJobInstanceService);
    FieldUtils.writeField(
        connectorChangeEventMessageProcessor, "verificationJobInstanceService", verificationJobInstanceService, true);
    doNothing().when(verificationJobInstanceService).resetVerificationJobPerpetualTasks(any());
    connectorChangeEventMessageProcessor.processUpdateAction(
        EntityChangeDTO.newBuilder()
            .setAccountIdentifier(StringValue.newBuilder().setValue(accountIdentifier).build())
            .setOrgIdentifier(StringValue.newBuilder().setValue(orgIdentifier).build())
            .setProjectIdentifier(StringValue.newBuilder().setValue(projectIdentifier).build())
            .setIdentifier(StringValue.newBuilder().setValue(connectorIdentifier).build())
            .build());

    verify(verificationJobInstanceService, times(1)).resetVerificationJobPerpetualTasks(cvConfig);
  }

  private CVConfig createCVConfig() {
    SplunkCVConfig cvConfig = new SplunkCVConfig();
    fillCommon(cvConfig);
    cvConfig.setQuery("exception");
    cvConfig.setServiceInstanceIdentifier(generateUuid());
    return cvConfig;
  }

  private void fillCommon(CVConfig cvConfig) {
    cvConfig.setVerificationType(VerificationType.LOG);
    cvConfig.setAccountId(accountIdentifier);
    cvConfig.setConnectorIdentifier(connectorIdentifier);
    cvConfig.setServiceIdentifier("service");
    cvConfig.setEnvIdentifier("env");
    cvConfig.setOrgIdentifier(orgIdentifier);
    cvConfig.setProjectIdentifier(projectIdentifier);
    cvConfig.setIdentifier("splunk_test");
    cvConfig.setMonitoringSourceName(generateUuid());
    cvConfig.setCategory(CVMonitoringCategory.PERFORMANCE);
    cvConfig.setProductName(generateUuid());
    cvConfig.setMonitoringSourceName(generateUuid());
  }

  private VerificationJobInstance createVerificationJobInstance(String cvConfigId, String verificationJobIdentifier,
      String envIdentifier, VerificationJobInstance.ExecutionStatus executionStatus) {
    verificationJobService.upsert(
        accountIdentifier, newTestVerificationJobDTO(verificationJobIdentifier, envIdentifier));
    VerificationJob verificationJob = verificationJobService.getVerificationJob(
        accountIdentifier, orgIdentifier, projectIdentifier, verificationJobIdentifier);
    VerificationJobInstance verificationJobInstance =
        VerificationJobInstance.builder()
            .accountId(accountIdentifier)
            .executionStatus(executionStatus)
            .verificationJobIdentifier(verificationJobIdentifier)
            .deploymentStartTime(Instant.ofEpochMilli(deploymentStartTimeMs))
            .resolvedJob(verificationJob)
            .createdAt(deploymentStartTimeMs + Duration.ofMinutes(2).toMillis())
            .startTime(Instant.ofEpochMilli(deploymentStartTimeMs + Duration.ofMinutes(2).toMillis()))
            .build();
    verificationJobInstanceService.create(verificationJobInstance);
    verificationTaskService.create(accountIdentifier, cvConfigId, verificationJobInstance.getUuid());
    return verificationJobInstance;
  }

  private VerificationJobDTO newTestVerificationJobDTO(String verificationJobIdentifier, String envIdentifier) {
    TestVerificationJobDTO verificationJobDTO = new TestVerificationJobDTO();
    fillCommonFields(verificationJobIdentifier, envIdentifier, verificationJobDTO);
    verificationJobDTO.setBaselineVerificationJobInstanceId(generateUuid());
    verificationJobDTO.setSensitivity(Sensitivity.MEDIUM.name());

    return verificationJobDTO;
  }

  private void fillCommonFields(
      String verificationJobIdentifier, String envIdentifier, VerificationJobDTO verificationJobDTO) {
    verificationJobDTO.setIdentifier(verificationJobIdentifier);
    verificationJobDTO.setJobName(generateUuid());
    verificationJobDTO.setDataSources(Lists.newArrayList(DataSourceType.SPLUNK));
    verificationJobDTO.setMonitoringSources(Arrays.asList("splunk_test"));
    verificationJobDTO.setServiceIdentifier(generateUuid());
    verificationJobDTO.setOrgIdentifier(orgIdentifier);
    verificationJobDTO.setProjectIdentifier(projectIdentifier);
    verificationJobDTO.setEnvIdentifier(envIdentifier);
    verificationJobDTO.setDuration("15m");
  }
}
