package software.wings.service.impl.trigger;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.api.DeploymentType.SSH;
import static software.wings.beans.AwsInfrastructureMapping.Builder.anAwsInfrastructureMapping;
import static software.wings.beans.EntityType.ENVIRONMENT;
import static software.wings.beans.EntityType.INFRASTRUCTURE_MAPPING;
import static software.wings.beans.EntityType.SERVICE;
import static software.wings.beans.Variable.VariableBuilder.aVariable;
import static software.wings.beans.WorkflowExecution.WorkflowExecutionBuilder.aWorkflowExecution;
import static software.wings.beans.WorkflowType.ORCHESTRATION;
import static software.wings.beans.WorkflowType.PIPELINE;
import static software.wings.beans.artifact.Artifact.Builder.anArtifact;
import static software.wings.beans.artifact.ArtifactFile.Builder.anArtifactFile;
import static software.wings.beans.trigger.ArtifactSelection.Type.ARTIFACT_SOURCE;
import static software.wings.beans.trigger.ArtifactSelection.Type.LAST_COLLECTED;
import static software.wings.beans.trigger.ArtifactSelection.Type.LAST_DEPLOYED;
import static software.wings.beans.trigger.ArtifactSelection.Type.PIPELINE_SOURCE;
import static software.wings.beans.trigger.ArtifactSelection.Type.WEBHOOK_VARIABLE;
import static software.wings.beans.trigger.WebhookEventType.PULL_REQUEST;
import static software.wings.beans.trigger.WebhookSource.BITBUCKET;
import static software.wings.beans.trigger.WebhookSource.GITHUB;
import static software.wings.dl.PageResponse.PageResponseBuilder.aPageResponse;
import static software.wings.service.impl.trigger.TriggerServiceTestHelper.artifact;
import static software.wings.service.impl.trigger.TriggerServiceTestHelper.assertWebhookToken;
import static software.wings.service.impl.trigger.TriggerServiceTestHelper.buildArtifactTrigger;
import static software.wings.service.impl.trigger.TriggerServiceTestHelper.buildJenkinsArtifactStream;
import static software.wings.service.impl.trigger.TriggerServiceTestHelper.buildNewInstanceTrigger;
import static software.wings.service.impl.trigger.TriggerServiceTestHelper.buildPipeline;
import static software.wings.service.impl.trigger.TriggerServiceTestHelper.buildPipelineCondTrigger;
import static software.wings.service.impl.trigger.TriggerServiceTestHelper.buildScheduledCondTrigger;
import static software.wings.service.impl.trigger.TriggerServiceTestHelper.buildWebhookCondTrigger;
import static software.wings.service.impl.trigger.TriggerServiceTestHelper.buildWorkflow;
import static software.wings.service.impl.trigger.TriggerServiceTestHelper.buildWorkflowArtifactTrigger;
import static software.wings.service.impl.trigger.TriggerServiceTestHelper.buildWorkflowWebhookTrigger;
import static software.wings.service.impl.trigger.TriggerServiceTestHelper.setPipelineStages;
import static software.wings.sm.ExecutionStatus.SUCCESS;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_FILTER;
import static software.wings.utils.WingsTestConstants.ARTIFACT_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_SOURCE_NAME;
import static software.wings.utils.WingsTestConstants.ARTIFACT_STREAM_ID;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.ENV_NAME;
import static software.wings.utils.WingsTestConstants.FILE_ID;
import static software.wings.utils.WingsTestConstants.FILE_NAME;
import static software.wings.utils.WingsTestConstants.INFRA_MAPPING_ID;
import static software.wings.utils.WingsTestConstants.PIPELINE_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.TRIGGER_ID;
import static software.wings.utils.WingsTestConstants.TRIGGER_NAME;
import static software.wings.utils.WingsTestConstants.WORKFLOW_ID;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import io.harness.distribution.idempotence.IdempotentLock;
import io.harness.distribution.idempotence.UnableToRegisterIdempotentOperationException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.quartz.JobDetail;
import org.quartz.TriggerKey;
import software.wings.WingsBaseTest;
import software.wings.beans.Environment;
import software.wings.beans.ExecutionArgs;
import software.wings.beans.Pipeline;
import software.wings.beans.Service;
import software.wings.beans.TemplateExpression;
import software.wings.beans.WebHookToken;
import software.wings.beans.Workflow;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.JenkinsArtifactStream;
import software.wings.beans.trigger.ArtifactSelection;
import software.wings.beans.trigger.ArtifactTriggerCondition;
import software.wings.beans.trigger.NewInstanceTriggerCondition;
import software.wings.beans.trigger.PipelineTriggerCondition;
import software.wings.beans.trigger.ScheduledTriggerCondition;
import software.wings.beans.trigger.Trigger;
import software.wings.beans.trigger.WebHookTriggerCondition;
import software.wings.beans.trigger.WebhookEventType;
import software.wings.beans.trigger.WebhookParameters;
import software.wings.beans.trigger.WebhookSource;
import software.wings.common.MongoIdempotentRegistry;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.exception.WingsException;
import software.wings.scheduler.JobScheduler;
import software.wings.scheduler.ScheduledTriggerJob;
import software.wings.service.intfc.ArtifactCollectionService;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.TriggerService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.WorkflowService;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Created by sgurubelli on 10/26/17.
 */
public class TriggerServiceTest extends WingsBaseTest {
  @Mock private JobScheduler jobScheduler;
  @Mock private PipelineService pipelineService;
  @Mock private WorkflowExecutionService workflowExecutionService;
  @Mock private ArtifactStreamService artifactStreamService;
  @Mock private ArtifactService artifactService;
  @Mock private ArtifactCollectionService artifactCollectionService;
  @Mock private ServiceResourceService serviceResourceService;
  @Mock private WorkflowService workflowService;
  @Mock private InfrastructureMappingService infrastructureMappingService;
  @Mock private MongoIdempotentRegistry<String> idempotentRegistry;
  @Mock private EnvironmentService environmentService;

  @Inject @InjectMocks private TriggerService triggerService;

  Trigger webhookConditionTrigger = buildWebhookCondTrigger();

  Trigger artifactConditionTrigger = buildArtifactTrigger();

  Trigger workflowArtifactConditionTrigger = buildWorkflowArtifactTrigger();

  Trigger pipelineCondTrigger = buildPipelineCondTrigger();

  Trigger scheduledConditionTrigger = buildScheduledCondTrigger();

  Trigger workflowWebhookConditionTrigger = buildWorkflowWebhookTrigger();

  Trigger newInstanceTrigger = buildNewInstanceTrigger();

  Pipeline pipeline = buildPipeline();

  Workflow workflow = buildWorkflow();

  JenkinsArtifactStream artifactStream = buildJenkinsArtifactStream();

  @Before
  public void setUp() throws UnableToRegisterIdempotentOperationException {
    Pipeline pipeline = buildPipeline();
    when(pipelineService.readPipeline(APP_ID, PIPELINE_ID, true)).thenReturn(pipeline);
    when(pipelineService.readPipeline(APP_ID, PIPELINE_ID, false)).thenReturn(pipeline);
    when(workflowService.readWorkflow(APP_ID, WORKFLOW_ID)).thenReturn(buildWorkflow());
    when(artifactStreamService.get(APP_ID, ARTIFACT_STREAM_ID)).thenReturn(buildJenkinsArtifactStream());
    when(serviceResourceService.get(APP_ID, SERVICE_ID, false))
        .thenReturn(Service.builder().uuid(SERVICE_ID).name("Catalog").build());
    when(idempotentRegistry.create(any(), any(), any(), any()))
        .thenReturn(IdempotentLock.<String>builder().registry(idempotentRegistry).resultData(Optional.empty()).build());
    when(artifactService.getArtifactByBuildNumber(
             APP_ID, ARTIFACT_STREAM_ID, artifactStream.getSourceName(), ARTIFACT_FILTER, false))
        .thenReturn(artifact);
  }

  @Test
  public void shouldListTriggers() {
    Trigger trigger = triggerService.save(artifactConditionTrigger);
    assertThat(trigger).isNotNull();
    PageRequest<Trigger> pageRequest = new PageRequest<>();
    PageResponse<Trigger> triggers = triggerService.list(pageRequest);
    assertThat(triggers.size()).isEqualTo(1);
  }

  @Test
  public void shouldGet() {
    Trigger trigger = triggerService.save(webhookConditionTrigger);
    assertThat(trigger).isNotNull();

    Trigger savedTrigger = triggerService.get(APP_ID, trigger.getUuid());
    assertThat(savedTrigger.getUuid()).isEqualTo(trigger.getUuid());
    assertThat(savedTrigger.getName()).isEqualTo(TRIGGER_NAME);
    assertThat(savedTrigger.getAppId()).isEqualTo(APP_ID);
  }

  @Test
  public void shouldGetExcludeHostsWithSameArtifact() {
    webhookConditionTrigger.setExcludeHostsWithSameArtifact(true);
    Trigger trigger = triggerService.save(webhookConditionTrigger);
    assertThat(trigger).isNotNull();

    Trigger savedTrigger = triggerService.get(APP_ID, trigger.getUuid());
    assertThat(savedTrigger.getUuid()).isEqualTo(trigger.getUuid());
    assertThat(savedTrigger.getName()).isEqualTo(TRIGGER_NAME);
    assertThat(savedTrigger.getAppId()).isEqualTo(APP_ID);
  }
  @Test
  public void shouldSaveArtifactConditionTrigger() {
    Trigger trigger = triggerService.save(artifactConditionTrigger);

    assertThat(trigger.getUuid()).isNotEmpty();
    assertThat(trigger.getCondition()).isInstanceOf(ArtifactTriggerCondition.class);
    assertThat(((ArtifactTriggerCondition) trigger.getCondition()).getArtifactFilter())
        .isNotNull()
        .isEqualTo(ARTIFACT_FILTER);
    assertThat(((ArtifactTriggerCondition) trigger.getCondition()).getArtifactStreamId())
        .isNotNull()
        .isEqualTo(ARTIFACT_STREAM_ID);
  }

  @Test
  public void shouldSaveWorkflowArtifactWConditionTrigger() {
    Trigger trigger = triggerService.save(workflowArtifactConditionTrigger);
    assertThat(trigger.getUuid()).isNotEmpty();
    assertThat(trigger.getWorkflowId()).isEqualTo(WORKFLOW_ID);
    assertThat(trigger.getWorkflowType()).isEqualTo(ORCHESTRATION);
    assertThat(trigger.getCondition()).isInstanceOf(ArtifactTriggerCondition.class);
    assertThat(((ArtifactTriggerCondition) trigger.getCondition()).getArtifactFilter())
        .isNotNull()
        .isEqualTo(ARTIFACT_FILTER);
    assertThat(((ArtifactTriggerCondition) trigger.getCondition()).getArtifactStreamId())
        .isNotNull()
        .isEqualTo(ARTIFACT_STREAM_ID);
    assertThat(((ArtifactTriggerCondition) trigger.getCondition()).isRegex()).isFalse();
  }

  @Test
  public void shouldUpdateArtifactConditionTrigger() {
    Trigger trigger = triggerService.save(artifactConditionTrigger);
    assertThat(trigger).isNotNull();
    assertThat(trigger.getUuid()).isNotEmpty();

    Trigger savedTrigger = triggerService.get(trigger.getAppId(), trigger.getUuid());
    assertThat(savedTrigger).isNotNull();
    assertThat(savedTrigger.getCondition()).isInstanceOf(ArtifactTriggerCondition.class);

    ArtifactTriggerCondition artifactTriggerCondition = (ArtifactTriggerCondition) savedTrigger.getCondition();
    artifactTriggerCondition.setRegex(true);

    savedTrigger.setArtifactSelections(
        asList(ArtifactSelection.builder().serviceId(SERVICE_ID).type(ARTIFACT_SOURCE).build(),
            ArtifactSelection.builder()
                .type(LAST_COLLECTED)
                .serviceId(SERVICE_ID)
                .artifactStreamId(ARTIFACT_STREAM_ID)
                .artifactFilter(ARTIFACT_FILTER)
                .build()));

    Trigger updatedTrigger = triggerService.update(savedTrigger);

    assertThat(updatedTrigger.getUuid()).isNotEmpty();
    assertThat(updatedTrigger.getCondition()).isInstanceOf(ArtifactTriggerCondition.class);
    assertThat(((ArtifactTriggerCondition) updatedTrigger.getCondition()).getArtifactFilter())
        .isNotNull()
        .isEqualTo(ARTIFACT_FILTER);
    assertThat(((ArtifactTriggerCondition) updatedTrigger.getCondition()).getArtifactStreamId())
        .isNotNull()
        .isEqualTo(ARTIFACT_STREAM_ID);
    assertThat(updatedTrigger.getArtifactSelections())
        .isNotNull()
        .extracting(artifactSelection -> artifactSelection.getType())
        .contains(ARTIFACT_SOURCE);

    assertThat(((ArtifactTriggerCondition) updatedTrigger.getCondition()).isRegex()).isTrue();
  }
  @Test
  public void shouldUpdateWorkflowArtifactConditionTrigger() {
    Trigger trigger = triggerService.save(workflowArtifactConditionTrigger);
    assertThat(trigger).isNotNull();
    assertThat(trigger.getUuid()).isNotEmpty();

    Trigger savedWorkflowTrigger = triggerService.get(trigger.getAppId(), trigger.getUuid());
    assertThat(savedWorkflowTrigger).isNotNull();
    assertThat(savedWorkflowTrigger.getCondition()).isInstanceOf(ArtifactTriggerCondition.class);

    savedWorkflowTrigger.setArtifactSelections(
        asList(ArtifactSelection.builder().serviceId(SERVICE_ID).type(ARTIFACT_SOURCE).build(),
            ArtifactSelection.builder()
                .type(LAST_COLLECTED)
                .serviceId(SERVICE_ID)
                .artifactStreamId(ARTIFACT_STREAM_ID)
                .artifactFilter(ARTIFACT_FILTER)
                .build(),
            ArtifactSelection.builder().serviceId(SERVICE_ID).type(LAST_DEPLOYED).workflowId(WORKFLOW_ID).build()));

    when(workflowService.readWorkflowWithoutOrchestration(trigger.getAppId(), WORKFLOW_ID)).thenReturn(buildWorkflow());

    Trigger updatedTrigger = triggerService.update(savedWorkflowTrigger);

    assertThat(updatedTrigger.getUuid()).isNotEmpty();
    assertThat(updatedTrigger.getCondition()).isInstanceOf(ArtifactTriggerCondition.class);
    assertThat(((ArtifactTriggerCondition) updatedTrigger.getCondition()).getArtifactFilter())
        .isNotNull()
        .isEqualTo(ARTIFACT_FILTER);
    assertThat(((ArtifactTriggerCondition) updatedTrigger.getCondition()).getArtifactStreamId())
        .isNotNull()
        .isEqualTo(ARTIFACT_STREAM_ID);
    assertThat(updatedTrigger.getArtifactSelections())
        .isNotNull()
        .extracting(artifactSelection -> artifactSelection.getType())
        .contains(ARTIFACT_SOURCE, LAST_DEPLOYED, LAST_COLLECTED);
    assertThat(updatedTrigger.getWorkflowType()).isEqualTo(ORCHESTRATION);

    verify(workflowService).readWorkflowWithoutOrchestration(trigger.getAppId(), WORKFLOW_ID);
  }

  @Test
  public void shouldSavePipelineConditionTrigger() {
    Trigger trigger = triggerService.save(pipelineCondTrigger);
    assertThat(trigger.getUuid()).isNotEmpty();
    assertThat(trigger.getCondition()).isInstanceOf(PipelineTriggerCondition.class);
    assertThat(((PipelineTriggerCondition) trigger.getCondition()).getPipelineId()).isNotNull().isEqualTo(PIPELINE_ID);
  }

  @Test
  public void shouldUpdatePipelineConditionTrigger() {
    Trigger savedPipelineCondTrigger = triggerService.save(pipelineCondTrigger);

    savedPipelineCondTrigger.setArtifactSelections(
        asList(ArtifactSelection.builder().type(PIPELINE_SOURCE).serviceId(SERVICE_ID).build(),
            ArtifactSelection.builder().serviceId(SERVICE_ID).type(LAST_DEPLOYED).pipelineId(PIPELINE_ID).build()));

    Trigger updatedTrigger = triggerService.update(savedPipelineCondTrigger);

    assertThat(updatedTrigger.getUuid()).isEqualTo(TRIGGER_ID);
    assertThat(updatedTrigger.getCondition()).isInstanceOf(PipelineTriggerCondition.class);
    assertThat(updatedTrigger.getArtifactSelections())
        .isNotNull()
        .extracting(artifactSelection -> artifactSelection.getType())
        .contains(PIPELINE_SOURCE, LAST_DEPLOYED);

    verify(pipelineService, times(3)).readPipeline(APP_ID, PIPELINE_ID, false);
  }

  @Test
  public void shouldSaveScheduledConditionTrigger() {
    Trigger trigger = triggerService.save(scheduledConditionTrigger);
    Trigger savedScheduledTrigger = triggerService.get(trigger.getAppId(), trigger.getUuid());

    assertThat(savedScheduledTrigger.getUuid()).isNotEmpty();
    assertThat(savedScheduledTrigger.getCondition()).isInstanceOf(ScheduledTriggerCondition.class);
    assertThat(((ScheduledTriggerCondition) trigger.getCondition()).getCronDescription()).isNotNull();
    assertThat(((ScheduledTriggerCondition) trigger.getCondition()).getCronExpression())
        .isNotNull()
        .isEqualTo("* * * * ?");
    verify(jobScheduler).scheduleJob(any(JobDetail.class), any(org.quartz.Trigger.class));
  }

  @Test
  public void shouldUpdateScheduledConditionTrigger() {
    scheduledConditionTrigger = triggerService.save(scheduledConditionTrigger);
    scheduledConditionTrigger.setArtifactSelections(asList(ArtifactSelection.builder()
                                                               .type(LAST_COLLECTED)
                                                               .serviceId(SERVICE_ID)
                                                               .artifactStreamId(ARTIFACT_STREAM_ID)
                                                               .artifactFilter(ARTIFACT_FILTER)
                                                               .build(),
        ArtifactSelection.builder().type(LAST_DEPLOYED).serviceId(SERVICE_ID).workflowId(PIPELINE_ID).build()));

    Trigger updatedTrigger = triggerService.update(scheduledConditionTrigger);
    assertThat(updatedTrigger.getUuid()).isEqualTo(TRIGGER_ID);
    assertThat(updatedTrigger.getCondition()).isInstanceOf(ScheduledTriggerCondition.class);
    assertThat(((ScheduledTriggerCondition) updatedTrigger.getCondition()).getCronDescription()).isNotNull();
    assertThat(((ScheduledTriggerCondition) updatedTrigger.getCondition()).getCronExpression())
        .isNotNull()
        .isEqualTo("* * * * ?");
    assertThat(updatedTrigger.getArtifactSelections())
        .isNotNull()
        .extracting(artifactSelection -> artifactSelection.getType())
        .contains(LAST_COLLECTED, LAST_DEPLOYED);
    verify(jobScheduler).rescheduleJob(any(TriggerKey.class), any(org.quartz.Trigger.class));
  }

  @Test
  public void shouldSaveWebhookConditionTrigger() {
    Pipeline pipeline = buildPipeline();
    setPipelineStages(pipeline);

    Trigger trigger = triggerService.save(buildWebhookCondTrigger());
    assertThat(trigger.getUuid()).isEqualTo(TRIGGER_ID);
    assertThat(trigger.getCondition()).isInstanceOf(WebHookTriggerCondition.class);
    assertThat(((WebHookTriggerCondition) trigger.getCondition()).getWebHookToken()).isNotNull();
    assertThat(((WebHookTriggerCondition) trigger.getCondition()).getWebHookToken().getWebHookToken()).isNotNull();
    verify(pipelineService, times(2)).readPipeline(APP_ID, PIPELINE_ID, true);
  }

  @Test
  public void shouldSaveWorkflowWebhookConditionTrigger() {
    Trigger trigger = triggerService.save(workflowWebhookConditionTrigger);
    Trigger savedTrigger = triggerService.get(APP_ID, trigger.getUuid());
    assertWebhookToken(savedTrigger);
  }

  @Test
  public void shouldSavePipelineWebhookConditionTrigger() {
    setPipelineStages(pipeline);
    when(pipelineService.readPipeline(APP_ID, PIPELINE_ID, true)).thenReturn(pipeline);

    Trigger trigger = triggerService.save(webhookConditionTrigger);

    Trigger savedTrigger = triggerService.get(APP_ID, trigger.getUuid());
    assertWebhookToken(savedTrigger);
  }

  @Test
  public void shouldUpdateWebhookConditionTrigger() {
    setWebhookArtifactSelections();

    webhookConditionTrigger = triggerService.save(webhookConditionTrigger);

    Trigger updatedTrigger = triggerService.update(webhookConditionTrigger);
    assertThat(updatedTrigger.getUuid()).isEqualTo(TRIGGER_ID);
    assertThat(updatedTrigger.getCondition()).isInstanceOf(WebHookTriggerCondition.class);
    assertThat(((WebHookTriggerCondition) updatedTrigger.getCondition()).getWebHookToken().getPayload()).isNotNull();
    assertThat(((WebHookTriggerCondition) updatedTrigger.getCondition()).getWebHookToken().getWebHookToken())
        .isNotNull();
    assertThat(updatedTrigger.getArtifactSelections())
        .isNotNull()
        .extracting(artifactSelection -> artifactSelection.getType())
        .contains(LAST_COLLECTED, LAST_DEPLOYED);
  }

  private void setWebhookArtifactSelections() {
    webhookConditionTrigger.setArtifactSelections(asList(ArtifactSelection.builder()
                                                             .type(LAST_COLLECTED)
                                                             .artifactStreamId(ARTIFACT_STREAM_ID)
                                                             .serviceId(SERVICE_ID)
                                                             .artifactFilter(ARTIFACT_FILTER)
                                                             .build(),
        ArtifactSelection.builder().type(LAST_DEPLOYED).serviceId(SERVICE_ID).workflowId(PIPELINE_ID).build()));
  }

  @Test
  public void shouldUpdateScheduledConditionTriggerToOtherType() {
    Trigger trigger = triggerService.save(scheduledConditionTrigger);
    assertThat(trigger.getUuid()).isEqualTo(TRIGGER_ID);
    assertThat(trigger.getCondition()).isInstanceOf(ScheduledTriggerCondition.class);
    assertThat(((ScheduledTriggerCondition) trigger.getCondition()).getCronDescription()).isNotNull();
    assertThat(((ScheduledTriggerCondition) trigger.getCondition()).getCronExpression())
        .isNotNull()
        .isEqualTo("* * * * ?");
    verify(jobScheduler).scheduleJob(any(JobDetail.class), any(org.quartz.Trigger.class));

    setWebhookArtifactSelections();

    Trigger updatedTrigger = triggerService.update(webhookConditionTrigger);

    assertThat(updatedTrigger.getCondition()).isInstanceOf(WebHookTriggerCondition.class);
    assertThat(((WebHookTriggerCondition) updatedTrigger.getCondition()).getWebHookToken().getPayload()).isNotNull();
    assertThat(((WebHookTriggerCondition) updatedTrigger.getCondition()).getWebHookToken()).isNotNull();
    assertThat(updatedTrigger.getArtifactSelections())
        .isNotNull()
        .extracting(artifactSelection -> artifactSelection.getType())
        .contains(LAST_COLLECTED, LAST_DEPLOYED);
    verify(jobScheduler).deleteJob(TRIGGER_ID, ScheduledTriggerJob.GROUP);
  }

  @Test
  public void shouldUpdateOtherConditionTriggerToScheduled() {
    Trigger trigger = triggerService.save(artifactConditionTrigger);
    assertThat(trigger.getUuid()).isNotEmpty();
    assertThat(trigger.getCondition()).isInstanceOf(ArtifactTriggerCondition.class);
    assertThat(((ArtifactTriggerCondition) trigger.getCondition()).getArtifactFilter())
        .isNotNull()
        .isEqualTo(ARTIFACT_FILTER);
    assertThat(((ArtifactTriggerCondition) trigger.getCondition()).getArtifactStreamId())
        .isNotNull()
        .isEqualTo(ARTIFACT_STREAM_ID);

    Trigger updatedTrigger = triggerService.update(scheduledConditionTrigger);
    assertThat(updatedTrigger.getUuid()).isEqualTo(TRIGGER_ID);
    assertThat(updatedTrigger.getCondition()).isInstanceOf(ScheduledTriggerCondition.class);
    assertThat(((ScheduledTriggerCondition) updatedTrigger.getCondition()).getCronDescription()).isNotNull();
    assertThat(((ScheduledTriggerCondition) updatedTrigger.getCondition()).getCronExpression())
        .isNotNull()
        .isEqualTo("* * * * ?");
    verify(jobScheduler).scheduleJob(any(JobDetail.class), any(org.quartz.Trigger.class));
  }

  @Test
  public void shouldDeleteScheduleTrigger() {
    triggerService.delete(APP_ID, TRIGGER_ID);
  }

  @Test
  public void shouldDeleteArtifactTrigger() {
    triggerService.delete(APP_ID, TRIGGER_ID);
  }

  @Test
  public void shouldDeleteTriggersForPipeline() {
    Trigger trigger = triggerService.save(pipelineCondTrigger);
    assertThat(trigger).isNotNull();
    assertThat(trigger.getUuid()).isEqualTo(TRIGGER_ID);
    assertThat(trigger.getAppId()).isEqualTo(APP_ID);
    triggerService.pruneByPipeline(APP_ID, PIPELINE_ID);
  }

  @Test
  public void shouldDeleteTriggersForWorkflow() {
    Trigger trigger = triggerService.save(workflowWebhookConditionTrigger);
    assertThat(trigger).isNotNull();
    assertThat(trigger.getUuid()).isEqualTo(TRIGGER_ID);
    assertThat(trigger.getAppId()).isEqualTo(APP_ID);
    assertThat(trigger.getWorkflowId()).isEqualTo(WORKFLOW_ID);
    triggerService.pruneByWorkflow(APP_ID, WORKFLOW_ID);
  }

  @Test
  public void shouldDeleteTriggersForArtifactStream() {
    Trigger trigger = triggerService.save(artifactConditionTrigger);
    assertThat(trigger).isNotNull();
    assertThat(trigger.getUuid()).isEqualTo(TRIGGER_ID);
    assertThat(trigger.getAppId()).isEqualTo(APP_ID);

    triggerService.pruneByArtifactStream(APP_ID, ARTIFACT_STREAM_ID);
  }

  @Test
  public void shouldDeleteTriggersByApp() {
    Trigger trigger = triggerService.save(scheduledConditionTrigger);
    assertThat(trigger).isNotNull();
    assertThat(trigger.getUuid()).isEqualTo(TRIGGER_ID);
    assertThat(trigger.getAppId()).isEqualTo(APP_ID);
    triggerService.pruneByApplication(APP_ID);
  }

  @Test
  public void shouldGenerateWebHookToken() {
    triggerService.save(webhookConditionTrigger);
    when(pipelineService.readPipeline(APP_ID, PIPELINE_ID, true)).thenReturn(pipeline);
    WebHookToken webHookToken = triggerService.generateWebHookToken(APP_ID, TRIGGER_ID);
    assertThat(webHookToken).isNotNull();
    assertThat(webHookToken.getPayload()).contains("application");
  }

  @Test
  public void shouldTriggerExecutionPostArtifactCollectionAsync() {
    Artifact artifact = anArtifact()
                            .withAppId(APP_ID)
                            .withUuid(ARTIFACT_ID)
                            .withArtifactStreamId(ARTIFACT_STREAM_ID)
                            .withMetadata(ImmutableMap.of("buildNo", ARTIFACT_FILTER))
                            .build();
    when(workflowExecutionService.triggerPipelineExecution(anyString(), anyString(), any(ExecutionArgs.class)))
        .thenReturn(aWorkflowExecution().withAppId(APP_ID).withStatus(SUCCESS).build());

    triggerService.save(artifactConditionTrigger);

    triggerService.triggerExecutionPostArtifactCollectionAsync(artifact);
    verify(workflowExecutionService).triggerPipelineExecution(anyString(), anyString(), any(ExecutionArgs.class));
  }

  @Test
  public void shouldTriggerExecutionPostArtifactCollectionWithFileNotMatchesArtifactFilter() {
    Artifact artifact = anArtifact()
                            .withAppId(APP_ID)
                            .withUuid(ARTIFACT_ID)
                            .withArtifactStreamId(ARTIFACT_STREAM_ID)
                            .withMetadata(ImmutableMap.of("buildNo", ARTIFACT_FILTER))
                            .withArtifactFiles(singletonList(
                                anArtifactFile().withAppId(APP_ID).withFileUuid(FILE_ID).withName(FILE_NAME).build()))
                            .build();
    when(workflowExecutionService.triggerPipelineExecution(anyString(), anyString(), any(ExecutionArgs.class)))
        .thenReturn(aWorkflowExecution().withAppId(APP_ID).withStatus(SUCCESS).build());

    triggerService.triggerExecutionPostArtifactCollectionAsync(artifact);
    verify(workflowExecutionService, times(0))
        .triggerPipelineExecution(anyString(), anyString(), any(ExecutionArgs.class));
  }

  @Test
  public void shouldTriggerExecutionPostArtifactCollectionWithFileRegexNotStartsWith() {
    Artifact artifact = anArtifact()
                            .withAppId(APP_ID)
                            .withUuid(ARTIFACT_ID)
                            .withArtifactStreamId(ARTIFACT_STREAM_ID)
                            .withMetadata(ImmutableMap.of("buildNo", ARTIFACT_FILTER))
                            .withArtifactFiles(singletonList(
                                anArtifactFile().withAppId(APP_ID).withFileUuid(FILE_ID).withName(FILE_NAME).build()))
                            .build();
    ArtifactTriggerCondition artifactTriggerCondition =
        (ArtifactTriggerCondition) artifactConditionTrigger.getCondition();
    artifactTriggerCondition.setRegex(true);
    artifactTriggerCondition.setArtifactFilter("^(?!release)");

    triggerService.save(artifactConditionTrigger);

    when(workflowExecutionService.triggerPipelineExecution(anyString(), anyString(), any(ExecutionArgs.class)))
        .thenReturn(aWorkflowExecution().withAppId(APP_ID).withStatus(SUCCESS).build());

    triggerService.triggerExecutionPostArtifactCollectionAsync(artifact);
    verify(workflowExecutionService).triggerPipelineExecution(anyString(), anyString(), any(ExecutionArgs.class));
  }

  @Test
  public void shouldTriggerExecutionPostArtifactCollectionRegexNotStartsWith() {
    Artifact artifact = anArtifact()
                            .withAppId(APP_ID)
                            .withUuid(ARTIFACT_ID)
                            .withArtifactStreamId(ARTIFACT_STREAM_ID)
                            .withMetadata(ImmutableMap.of("buildNo", ARTIFACT_FILTER))
                            .build();
    ArtifactTriggerCondition artifactTriggerCondition =
        (ArtifactTriggerCondition) artifactConditionTrigger.getCondition();
    artifactTriggerCondition.setRegex(true);
    artifactTriggerCondition.setArtifactFilter("^(?!release)");

    triggerService.save(artifactConditionTrigger);

    when(workflowExecutionService.triggerPipelineExecution(anyString(), anyString(), any(ExecutionArgs.class)))
        .thenReturn(aWorkflowExecution().withAppId(APP_ID).withStatus(SUCCESS).build());

    triggerService.triggerExecutionPostArtifactCollectionAsync(artifact);
    verify(workflowExecutionService).triggerPipelineExecution(anyString(), anyString(), any(ExecutionArgs.class));
  }

  @Test
  public void shouldTriggerExecutionPostArtifactCollectionRegexMatch() {
    Artifact artifact = anArtifact()
                            .withAppId(APP_ID)
                            .withUuid(ARTIFACT_ID)
                            .withArtifactStreamId(ARTIFACT_STREAM_ID)
                            .withMetadata(ImmutableMap.of("buildNo", "release2345"))
                            .build();
    ArtifactTriggerCondition artifactTriggerCondition =
        (ArtifactTriggerCondition) artifactConditionTrigger.getCondition();
    artifactTriggerCondition.setRegex(true);
    artifactTriggerCondition.setArtifactFilter("^release");

    triggerService.save(artifactConditionTrigger);

    when(workflowExecutionService.triggerPipelineExecution(anyString(), anyString(), any(ExecutionArgs.class)))
        .thenReturn(aWorkflowExecution().withAppId(APP_ID).withStatus(SUCCESS).build());

    triggerService.triggerExecutionPostArtifactCollectionAsync(artifact);
    verify(workflowExecutionService).triggerPipelineExecution(anyString(), anyString(), any(ExecutionArgs.class));
  }

  @Test
  public void shouldTriggerExecutionPostArtifactCollectionRegexDoesNotMatch() {
    Artifact artifact = anArtifact()
                            .withAppId(APP_ID)
                            .withUuid(ARTIFACT_ID)
                            .withArtifactStreamId(ARTIFACT_STREAM_ID)
                            .withMetadata(ImmutableMap.of("buildNo", "@33release23"))
                            .build();
    ArtifactTriggerCondition artifactTriggerCondition =
        (ArtifactTriggerCondition) artifactConditionTrigger.getCondition();
    artifactTriggerCondition.setRegex(true);
    artifactTriggerCondition.setArtifactFilter("^release");

    when(workflowExecutionService.triggerPipelineExecution(anyString(), anyString(), any(ExecutionArgs.class)))
        .thenReturn(aWorkflowExecution().withAppId(APP_ID).withStatus(SUCCESS).build());

    triggerService.triggerExecutionPostArtifactCollectionAsync(artifact);
    verify(workflowExecutionService, times(0))
        .triggerPipelineExecution(anyString(), anyString(), any(ExecutionArgs.class));
  }

  @Test
  public void shouldTriggerExecutionPostArtifactCollectionWithArtifactMatchesArtifactFilter() {
    triggerService.save(artifactConditionTrigger);
    Artifact artifact = anArtifact()
                            .withAppId(APP_ID)
                            .withUuid(ARTIFACT_ID)
                            .withArtifactStreamId(ARTIFACT_STREAM_ID)
                            .withMetadata(ImmutableMap.of("buildNo", ARTIFACT_FILTER))
                            .build();
    when(workflowExecutionService.triggerPipelineExecution(anyString(), anyString(), any(ExecutionArgs.class)))
        .thenReturn(aWorkflowExecution().withAppId(APP_ID).withStatus(SUCCESS).build());

    triggerService.triggerExecutionPostArtifactCollectionAsync(artifact);
    verify(workflowExecutionService).triggerPipelineExecution(anyString(), anyString(), any(ExecutionArgs.class));
  }

  @Test
  public void shouldTriggerExecutionPostArtifactCollectionWithArtifactSelections() {
    Artifact artifact = anArtifact()
                            .withAppId(APP_ID)
                            .withUuid(ARTIFACT_ID)
                            .withArtifactStreamId(ARTIFACT_STREAM_ID)
                            .withServiceIds(singletonList(SERVICE_ID))
                            .withMetadata(ImmutableMap.of("buildNo", ARTIFACT_FILTER))
                            .build();
    artifactConditionTrigger.setArtifactSelections(
        asList(ArtifactSelection.builder().serviceId(SERVICE_ID).type(ARTIFACT_SOURCE).build(),
            ArtifactSelection.builder()
                .type(LAST_COLLECTED)
                .serviceId(SERVICE_ID)
                .artifactStreamId(ARTIFACT_STREAM_ID)
                .artifactFilter(ARTIFACT_FILTER)
                .build(),
            ArtifactSelection.builder().type(LAST_DEPLOYED).serviceId(SERVICE_ID).workflowId(PIPELINE_ID).build()));

    triggerService.save(artifactConditionTrigger);

    when(artifactStreamService.get(APP_ID, ARTIFACT_STREAM_ID)).thenReturn(artifactStream);
    when(artifactService.fetchLastCollectedApprovedArtifactForArtifactStream(
             APP_ID, ARTIFACT_STREAM_ID, artifactStream.getSourceName()))
        .thenReturn(artifact);
    when(workflowExecutionService.triggerPipelineExecution(anyString(), anyString(), any(ExecutionArgs.class)))
        .thenReturn(aWorkflowExecution().withAppId(APP_ID).withStatus(SUCCESS).build());

    ExecutionArgs executionArgs = new ExecutionArgs();
    executionArgs.setArtifacts(singletonList(artifact));
    when(workflowExecutionService.listExecutions(any(PageRequest.class), anyBoolean()))
        .thenReturn(aPageResponse()
                        .withResponse(singletonList(
                            aWorkflowExecution().withAppId(APP_ID).withExecutionArgs(executionArgs).build()))
                        .build());

    triggerService.triggerExecutionPostArtifactCollectionAsync(artifact);
    verify(workflowExecutionService).triggerPipelineExecution(anyString(), anyString(), any(ExecutionArgs.class));
    verify(artifactStreamService, times(3)).get(APP_ID, ARTIFACT_STREAM_ID);
    verify(artifactService)
        .getArtifactByBuildNumber(
            APP_ID, ARTIFACT_STREAM_ID, buildJenkinsArtifactStream().getSourceName(), ARTIFACT_FILTER, false);
  }

  @Test
  public void shouldTriggerWorkflowExecutionPostArtifactCollectionWithArtifactSelections() {
    Artifact artifact = anArtifact()
                            .withAppId(APP_ID)
                            .withUuid(ARTIFACT_ID)
                            .withArtifactStreamId(ARTIFACT_STREAM_ID)
                            .withServiceIds(singletonList(SERVICE_ID))
                            .withMetadata(ImmutableMap.of("buildNo", ARTIFACT_FILTER))
                            .build();
    workflowArtifactConditionTrigger.setArtifactSelections(
        asList(ArtifactSelection.builder().serviceId(SERVICE_ID).type(ARTIFACT_SOURCE).build(),
            ArtifactSelection.builder()
                .type(LAST_COLLECTED)
                .serviceId(SERVICE_ID)
                .artifactStreamId(ARTIFACT_STREAM_ID)
                .artifactFilter(ARTIFACT_FILTER)
                .build(),
            ArtifactSelection.builder().type(LAST_DEPLOYED).serviceId(SERVICE_ID).workflowId(WORKFLOW_ID).build()));

    when(workflowService.readWorkflowWithoutOrchestration(APP_ID, WORKFLOW_ID)).thenReturn(workflow);

    triggerService.save(workflowArtifactConditionTrigger);

    ArtifactStream artifactStream = buildJenkinsArtifactStream();
    when(artifactStreamService.get(APP_ID, ARTIFACT_STREAM_ID)).thenReturn(artifactStream);
    when(artifactService.fetchLastCollectedApprovedArtifactForArtifactStream(
             APP_ID, ARTIFACT_STREAM_ID, artifactStream.getSourceName()))
        .thenReturn(artifact);

    when(workflowExecutionService.triggerEnvExecution(anyString(), anyString(), any(ExecutionArgs.class)))
        .thenReturn(aWorkflowExecution().withAppId(APP_ID).withStatus(SUCCESS).build());

    ExecutionArgs executionArgs = new ExecutionArgs();
    executionArgs.setArtifacts(singletonList(artifact));
    when(workflowExecutionService.listExecutions(any(PageRequest.class), anyBoolean()))
        .thenReturn(aPageResponse()
                        .withResponse(singletonList(
                            aWorkflowExecution().withAppId(APP_ID).withExecutionArgs(executionArgs).build()))
                        .build());

    triggerService.triggerExecutionPostArtifactCollectionAsync(artifact);

    verify(workflowExecutionService).triggerEnvExecution(anyString(), anyString(), any(ExecutionArgs.class));
    verify(artifactStreamService, times(3)).get(APP_ID, ARTIFACT_STREAM_ID);
    verify(artifactService)
        .getArtifactByBuildNumber(APP_ID, ARTIFACT_STREAM_ID, artifactStream.getSourceName(), ARTIFACT_FILTER, false);
    verify(workflowExecutionService).obtainLastGoodDeployedArtifacts(APP_ID, WORKFLOW_ID);
    verify(workflowService).readWorkflowWithoutOrchestration(APP_ID, WORKFLOW_ID);
  }

  @Test
  public void shouldTriggerTemplateWorkflowExecution() {
    Workflow workflow = buildWorkflow();
    workflow.getOrchestrationWorkflow().getUserVariables().add(
        aVariable().withName("Environment").withValue(ENV_ID).withEntityType(ENVIRONMENT).build());

    workflowArtifactConditionTrigger.setArtifactSelections(
        asList(ArtifactSelection.builder().serviceId(SERVICE_ID).type(ARTIFACT_SOURCE).build(),
            ArtifactSelection.builder()
                .type(LAST_COLLECTED)
                .serviceId(SERVICE_ID)
                .artifactStreamId(ARTIFACT_STREAM_ID)
                .artifactFilter(ARTIFACT_FILTER)
                .build(),
            ArtifactSelection.builder().type(LAST_DEPLOYED).serviceId(SERVICE_ID).workflowId(WORKFLOW_ID).build()));

    workflowArtifactConditionTrigger.setWorkflowVariables(
        ImmutableMap.of("Environment", ENV_ID, "Service", SERVICE_ID, "ServiceInfraStructure", INFRA_MAPPING_ID));

    when(artifactStreamService.get(APP_ID, ARTIFACT_STREAM_ID)).thenReturn(buildJenkinsArtifactStream());

    when(workflowService.readWorkflowWithoutOrchestration(APP_ID, WORKFLOW_ID)).thenReturn(buildWorkflow());

    triggerService.save(workflowArtifactConditionTrigger);

    when(workflowExecutionService.triggerEnvExecution(anyString(), anyString(), any(ExecutionArgs.class)))
        .thenReturn(aWorkflowExecution().withAppId(APP_ID).withStatus(SUCCESS).build());

    ExecutionArgs executionArgs = new ExecutionArgs();
    executionArgs.setArtifacts(singletonList(artifact));
    executionArgs.setWorkflowVariables(ImmutableMap.of("MyVar", "MyVal"));

    when(workflowExecutionService.listExecutions(any(PageRequest.class), anyBoolean()))
        .thenReturn(aPageResponse()
                        .withResponse(singletonList(
                            aWorkflowExecution().withAppId(APP_ID).withExecutionArgs(executionArgs).build()))
                        .build());

    triggerService.triggerExecutionPostArtifactCollectionAsync(artifact);

    verify(workflowExecutionService).triggerEnvExecution(anyString(), anyString(), any(ExecutionArgs.class));
    verify(artifactStreamService, times(3)).get(APP_ID, ARTIFACT_STREAM_ID);
    verify(artifactService)
        .getArtifactByBuildNumber(
            APP_ID, ARTIFACT_STREAM_ID, buildJenkinsArtifactStream().getSourceName(), ARTIFACT_FILTER, false);
    verify(workflowExecutionService).obtainLastGoodDeployedArtifacts(APP_ID, WORKFLOW_ID);
    verify(workflowService, times(2)).readWorkflow(APP_ID, WORKFLOW_ID);
    verify(workflowService).readWorkflowWithoutOrchestration(APP_ID, WORKFLOW_ID);
  }

  @Test
  public void shouldTriggerExecutionPostPipelineCompletion() {
    Artifact artifact = anArtifact()
                            .withAppId(APP_ID)
                            .withUuid(ARTIFACT_ID)
                            .withArtifactStreamId(ARTIFACT_STREAM_ID)
                            .withServiceIds(singletonList(SERVICE_ID))
                            .withMetadata(ImmutableMap.of("buildNo", ARTIFACT_FILTER))
                            .build();

    pipelineCondTrigger.setArtifactSelections(
        asList(ArtifactSelection.builder().type(PIPELINE_SOURCE).serviceId(SERVICE_ID).build(),
            ArtifactSelection.builder().serviceId(SERVICE_ID).type(LAST_DEPLOYED).pipelineId(PIPELINE_ID).build()));

    triggerService.save(pipelineCondTrigger);

    pipelineCompletionMocks(singletonList(artifact));

    when(workflowExecutionService.obtainLastGoodDeployedArtifacts(APP_ID, PIPELINE_ID)).thenReturn(asList(artifact));

    triggerService.triggerExecutionPostPipelineCompletionAsync(APP_ID, PIPELINE_ID);
    verify(workflowExecutionService).triggerPipelineExecution(anyString(), anyString(), any(ExecutionArgs.class));
  }

  @Test
  public void shouldNotTriggerExecutionPostPipelineCompletion() {
    pipelineCompletionMocks(singletonList(artifact));

    triggerService.triggerExecutionPostPipelineCompletionAsync(APP_ID, PIPELINE_ID);
    verify(workflowExecutionService, times(0))
        .triggerPipelineExecution(anyString(), anyString(), any(ExecutionArgs.class));
  }

  private void pipelineCompletionMocks(List<Artifact> artifacts) {
    ExecutionArgs executionArgs = new ExecutionArgs();
    executionArgs.setArtifacts(artifacts);

    when(workflowExecutionService.triggerPipelineExecution(anyString(), anyString(), any(ExecutionArgs.class)))
        .thenReturn(aWorkflowExecution().withAppId(APP_ID).withStatus(SUCCESS).build());
    when(workflowExecutionService.listExecutions(any(PageRequest.class), anyBoolean()))
        .thenReturn(aPageResponse()
                        .withResponse(singletonList(
                            aWorkflowExecution().withAppId(APP_ID).withExecutionArgs(executionArgs).build()))
                        .build());
  }

  @Test
  public void shouldTriggerExecutionPostPipelineCompletionWithArtifactSelections() {
    Artifact artifact = anArtifact()
                            .withAppId(APP_ID)
                            .withUuid(ARTIFACT_ID)
                            .withArtifactStreamId(ARTIFACT_STREAM_ID)
                            .withServiceIds(singletonList(SERVICE_ID))
                            .withMetadata(ImmutableMap.of("buildNo", ARTIFACT_FILTER))
                            .build();
    pipelineCondTrigger.setArtifactSelections(
        asList(ArtifactSelection.builder().serviceId(SERVICE_ID).type(PIPELINE_SOURCE).build(),
            ArtifactSelection.builder()
                .type(LAST_COLLECTED)
                .serviceId(SERVICE_ID)
                .artifactStreamId(ARTIFACT_STREAM_ID)
                .artifactFilter(ARTIFACT_FILTER)
                .build(),
            ArtifactSelection.builder().type(LAST_DEPLOYED).serviceId(SERVICE_ID).workflowId(PIPELINE_ID).build()));

    triggerService.save(pipelineCondTrigger);

    when(artifactStreamService.get(APP_ID, ARTIFACT_STREAM_ID)).thenReturn(buildJenkinsArtifactStream());
    when(artifactService.fetchLastCollectedApprovedArtifactForArtifactStream(
             APP_ID, ARTIFACT_STREAM_ID, buildJenkinsArtifactStream().getSourceName()))
        .thenReturn(artifact);
    when(workflowExecutionService.triggerPipelineExecution(anyString(), anyString(), any(ExecutionArgs.class)))
        .thenReturn(aWorkflowExecution().withAppId(APP_ID).withStatus(SUCCESS).build());

    ExecutionArgs executionArgs = new ExecutionArgs();
    executionArgs.setArtifacts(singletonList(artifact));
    when(workflowExecutionService.listExecutions(any(PageRequest.class), anyBoolean()))
        .thenReturn(aPageResponse()
                        .withResponse(singletonList(
                            aWorkflowExecution().withAppId(APP_ID).withExecutionArgs(executionArgs).build()))
                        .build());

    triggerService.triggerExecutionPostPipelineCompletionAsync(APP_ID, PIPELINE_ID);
    verify(workflowExecutionService).triggerPipelineExecution(anyString(), anyString(), any(ExecutionArgs.class));
    verify(artifactStreamService, times(2)).get(APP_ID, ARTIFACT_STREAM_ID);
    verify(artifactService)
        .getArtifactByBuildNumber(
            APP_ID, ARTIFACT_STREAM_ID, buildJenkinsArtifactStream().getSourceName(), ARTIFACT_FILTER, false);
  }

  @Test
  public void shouldTriggerScheduledExecution() throws UnableToRegisterIdempotentOperationException {
    Artifact artifact = anArtifact()
                            .withAppId(APP_ID)
                            .withUuid(ARTIFACT_ID)
                            .withArtifactStreamId(ARTIFACT_STREAM_ID)
                            .withServiceIds(singletonList(SERVICE_ID))
                            .withMetadata(ImmutableMap.of("buildNo", ARTIFACT_FILTER))
                            .build();
    scheduledTriggerMocks();
    when(workflowExecutionService.obtainLastGoodDeployedArtifacts(APP_ID, PIPELINE_ID)).thenReturn(asList(artifact));

    triggerService.save(scheduledConditionTrigger);

    triggerService.triggerScheduledExecutionAsync(scheduledConditionTrigger, new Date());

    verify(idempotentRegistry).create(any(), any(), any(), any());
    verify(workflowExecutionService, times(1))
        .triggerPipelineExecution(anyString(), anyString(), any(ExecutionArgs.class));
  }

  @Test
  public void shouldTriggerScheduledExecutionIfNoArtifacts() throws UnableToRegisterIdempotentOperationException {
    scheduledTriggerMocks();
    triggerService.save(scheduledConditionTrigger);

    triggerService.triggerScheduledExecutionAsync(scheduledConditionTrigger, new Date());

    verify(idempotentRegistry).create(any(), any(), any(), any());
    verify(workflowExecutionService).triggerPipelineExecution(anyString(), anyString(), any(ExecutionArgs.class));
  }

  private void scheduledTriggerMocks() {
    ExecutionArgs executionArgs = new ExecutionArgs();
    executionArgs.setArtifacts(singletonList(artifact));

    when(workflowExecutionService.triggerPipelineExecution(anyString(), anyString(), any(ExecutionArgs.class)))
        .thenReturn(aWorkflowExecution().withAppId(APP_ID).withStatus(SUCCESS).build());
    when(workflowExecutionService.listExecutions(any(PageRequest.class), anyBoolean()))
        .thenReturn(aPageResponse()
                        .withResponse(singletonList(
                            aWorkflowExecution().withAppId(APP_ID).withExecutionArgs(executionArgs).build()))
                        .build());
  }

  @Test
  public void shouldTriggerScheduledExecutionWithArtifactSelections()
      throws UnableToRegisterIdempotentOperationException {
    Artifact artifact = anArtifact()
                            .withAppId(APP_ID)
                            .withUuid(ARTIFACT_ID)
                            .withArtifactStreamId(ARTIFACT_STREAM_ID)
                            .withServiceIds(singletonList(SERVICE_ID))
                            .withMetadata(ImmutableMap.of("buildNo", ARTIFACT_FILTER))
                            .build();
    ExecutionArgs executionArgs = new ExecutionArgs();
    executionArgs.setArtifacts(singletonList(artifact));

    scheduledConditionTrigger.setArtifactSelections(asList(ArtifactSelection.builder()
                                                               .type(LAST_COLLECTED)
                                                               .serviceId(SERVICE_ID)
                                                               .artifactStreamId(ARTIFACT_STREAM_ID)
                                                               .artifactFilter(ARTIFACT_FILTER)
                                                               .build(),
        ArtifactSelection.builder().type(LAST_DEPLOYED).serviceId(SERVICE_ID).workflowId(PIPELINE_ID).build()));

    triggerService.save(scheduledConditionTrigger);

    when(artifactStreamService.get(APP_ID, ARTIFACT_STREAM_ID)).thenReturn(buildJenkinsArtifactStream());
    when(artifactService.fetchLastCollectedApprovedArtifactForArtifactStream(
             APP_ID, ARTIFACT_STREAM_ID, buildJenkinsArtifactStream().getSourceName()))
        .thenReturn(artifact);
    when(workflowExecutionService.triggerPipelineExecution(anyString(), anyString(), any(ExecutionArgs.class)))
        .thenReturn(aWorkflowExecution().withAppId(APP_ID).withStatus(SUCCESS).build());

    triggerService.triggerScheduledExecutionAsync(scheduledConditionTrigger, new Date());
    verify(workflowExecutionService).triggerPipelineExecution(anyString(), anyString(), any(ExecutionArgs.class));
    verify(artifactStreamService, times(2)).get(APP_ID, ARTIFACT_STREAM_ID);
    verify(artifactService)
        .getArtifactByBuildNumber(
            APP_ID, ARTIFACT_STREAM_ID, buildJenkinsArtifactStream().getSourceName(), ARTIFACT_FILTER, false);
    verify(idempotentRegistry).create(any(), any(), any(), any());
  }

  @Test
  public void shouldTriggerArtifactCollectionForWebhookTrigger() {
    Artifact artifact = anArtifact()
                            .withAppId(APP_ID)
                            .withUuid(ARTIFACT_ID)
                            .withArtifactStreamId(ARTIFACT_STREAM_ID)
                            .withServiceIds(singletonList(SERVICE_ID))
                            .withMetadata(ImmutableMap.of("buildNo", ARTIFACT_FILTER))
                            .build();

    ExecutionArgs executionArgs = new ExecutionArgs();
    executionArgs.setArtifacts(singletonList(artifact));

    setArtifactSelectionsForWebhookTrigger();

    triggerService.save(webhookConditionTrigger);

    when(artifactStreamService.get(APP_ID, ARTIFACT_STREAM_ID)).thenReturn(buildJenkinsArtifactStream());
    when(artifactService.fetchLatestArtifactForArtifactStream(APP_ID, ARTIFACT_STREAM_ID, ARTIFACT_SOURCE_NAME))
        .thenReturn(artifact);

    triggerService.triggerExecutionByWebHook(
        APP_ID, webhookConditionTrigger.getWebHookToken(), ImmutableMap.of("Catalog", "123"), new HashMap<>());

    when(artifactCollectionService.collectNewArtifacts(APP_ID, ARTIFACT_STREAM_ID)).thenReturn(Arrays.asList(artifact));
    verify(artifactCollectionService).collectNewArtifacts(APP_ID, ARTIFACT_STREAM_ID);
    verify(artifactService).fetchLastCollectedArtifact(APP_ID, ARTIFACT_STREAM_ID, ARTIFACT_SOURCE_NAME);
  }

  @Test
  public void shouldTriggerExecutionByWebhookWithNoBuildNumber() {
    Artifact artifact = anArtifact()
                            .withAppId(APP_ID)
                            .withUuid(ARTIFACT_ID)
                            .withArtifactStreamId(ARTIFACT_STREAM_ID)
                            .withServiceIds(singletonList(SERVICE_ID))
                            .withMetadata(ImmutableMap.of("buildNo", ARTIFACT_FILTER))
                            .build();
    ExecutionArgs executionArgs = new ExecutionArgs();
    executionArgs.setArtifacts(singletonList(artifact));

    setArtifactSelectionsForWebhookTrigger();

    triggerService.save(webhookConditionTrigger);

    when(artifactStreamService.get(APP_ID, ARTIFACT_STREAM_ID)).thenReturn(buildJenkinsArtifactStream());
    when(artifactService.fetchLastCollectedApprovedArtifactForArtifactStream(
             APP_ID, ARTIFACT_STREAM_ID, buildJenkinsArtifactStream().getSourceName()))
        .thenReturn(artifact);
    when(workflowExecutionService.triggerPipelineExecution(anyString(), anyString(), any(ExecutionArgs.class)))
        .thenReturn(aWorkflowExecution().withAppId(APP_ID).withStatus(SUCCESS).build());
    when(workflowExecutionService.listExecutions(any(PageRequest.class), anyBoolean()))
        .thenReturn(aPageResponse()
                        .withResponse(singletonList(
                            aWorkflowExecution().withAppId(APP_ID).withExecutionArgs(executionArgs).build()))
                        .build());
    when(artifactStreamService.get(APP_ID, ARTIFACT_STREAM_ID)).thenReturn(buildJenkinsArtifactStream());
    when(pipelineService.readPipeline(APP_ID, PIPELINE_ID, true)).thenReturn(buildPipeline());
    when(artifactService.getArtifactByBuildNumber(any(), any(), anyString())).thenReturn(artifact);

    triggerService.triggerExecutionByWebHook(
        APP_ID, webhookConditionTrigger.getWebHookToken(), ImmutableMap.of("Catalog", "123"), new HashMap<>());

    verify(workflowExecutionService).triggerPipelineExecution(anyString(), anyString(), any(ExecutionArgs.class));
    verify(artifactStreamService, times(7)).get(APP_ID, ARTIFACT_STREAM_ID);
    verify(artifactService)
        .getArtifactByBuildNumber(
            APP_ID, ARTIFACT_STREAM_ID, buildJenkinsArtifactStream().getSourceName(), ARTIFACT_FILTER, false);
  }

  private void setArtifactSelectionsForWebhookTrigger() {
    webhookConditionTrigger.setArtifactSelections(asList(ArtifactSelection.builder()
                                                             .type(WEBHOOK_VARIABLE)
                                                             .serviceId(SERVICE_ID)
                                                             .artifactStreamId(ARTIFACT_STREAM_ID)
                                                             .artifactSourceName(ARTIFACT_SOURCE_NAME)
                                                             .build(),
        ArtifactSelection.builder()
            .type(LAST_COLLECTED)
            .serviceId(SERVICE_ID)
            .artifactStreamId(ARTIFACT_STREAM_ID)
            .artifactFilter(ARTIFACT_FILTER)
            .artifactSourceName(ARTIFACT_SOURCE_NAME)
            .build(),
        ArtifactSelection.builder()
            .type(LAST_COLLECTED)
            .serviceId(SERVICE_ID)
            .artifactStreamId(ARTIFACT_STREAM_ID)
            .artifactSourceName(ARTIFACT_SOURCE_NAME)
            .build(),
        ArtifactSelection.builder().type(LAST_DEPLOYED).serviceId(SERVICE_ID).workflowId(PIPELINE_ID).build()));
  }

  @Test
  public void shouldTriggerExecutionByWebhookWithBuildNumber() {
    Artifact artifact = anArtifact()
                            .withAppId(APP_ID)
                            .withUuid(ARTIFACT_ID)
                            .withArtifactStreamId(ARTIFACT_STREAM_ID)
                            .withServiceIds(singletonList(SERVICE_ID))
                            .withMetadata(ImmutableMap.of("buildNo", ARTIFACT_FILTER))
                            .build();
    ExecutionArgs executionArgs = new ExecutionArgs();
    executionArgs.setArtifacts(singletonList(artifact));

    setArtifactSelectionsForWebhookTrigger();

    triggerService.save(webhookConditionTrigger);

    when(artifactStreamService.get(APP_ID, ARTIFACT_STREAM_ID)).thenReturn(buildJenkinsArtifactStream());
    when(artifactService.fetchLastCollectedApprovedArtifactForArtifactStream(
             APP_ID, ARTIFACT_STREAM_ID, buildJenkinsArtifactStream().getSourceName()))
        .thenReturn(artifact);
    when(workflowExecutionService.triggerPipelineExecution(anyString(), anyString(), any(ExecutionArgs.class)))
        .thenReturn(aWorkflowExecution().withAppId(APP_ID).withStatus(SUCCESS).build());
    when(workflowExecutionService.listExecutions(any(PageRequest.class), anyBoolean()))
        .thenReturn(aPageResponse()
                        .withResponse(singletonList(
                            aWorkflowExecution().withAppId(APP_ID).withExecutionArgs(executionArgs).build()))
                        .build());
    when(artifactStreamService.get(APP_ID, ARTIFACT_STREAM_ID)).thenReturn(buildJenkinsArtifactStream());
    when(pipelineService.readPipeline(APP_ID, PIPELINE_ID, true)).thenReturn(buildPipeline());
    when(artifactService.getArtifactByBuildNumber(APP_ID, ARTIFACT_STREAM_ID, "123")).thenReturn(artifact);

    triggerService.triggerExecutionByWebHook(
        APP_ID, webhookConditionTrigger.getWebHookToken(), ImmutableMap.of("Catalog", "123"), new HashMap<>());

    verify(workflowExecutionService).triggerPipelineExecution(anyString(), anyString(), any(ExecutionArgs.class));
    verify(artifactStreamService, times(7)).get(APP_ID, ARTIFACT_STREAM_ID);
    verify(artifactService)
        .getArtifactByBuildNumber(
            APP_ID, ARTIFACT_STREAM_ID, buildJenkinsArtifactStream().getSourceName(), ARTIFACT_FILTER, false);

    verify(workflowExecutionService).obtainLastGoodDeployedArtifacts(APP_ID, PIPELINE_ID);
    verify(artifactService, times(2))
        .getArtifactByBuildNumber(APP_ID, ARTIFACT_STREAM_ID, ARTIFACT_SOURCE_NAME, "123", false);
  }

  @Test
  public void shouldTriggerWorkflowExecutionByWebhookWithBuildNumber() {
    Artifact artifact = anArtifact()
                            .withAppId(APP_ID)
                            .withUuid(ARTIFACT_ID)
                            .withArtifactStreamId(ARTIFACT_STREAM_ID)
                            .withServiceIds(singletonList(SERVICE_ID))
                            .withMetadata(ImmutableMap.of("buildNo", ARTIFACT_FILTER))
                            .build();
    ExecutionArgs executionArgs = new ExecutionArgs();
    executionArgs.setArtifacts(singletonList(artifact));

    workflowWebhookConditionTrigger.setArtifactSelections(asList(ArtifactSelection.builder()
                                                                     .type(WEBHOOK_VARIABLE)
                                                                     .serviceId(SERVICE_ID)
                                                                     .artifactStreamId(ARTIFACT_STREAM_ID)
                                                                     .build(),
        ArtifactSelection.builder()
            .type(LAST_COLLECTED)
            .serviceId(SERVICE_ID)
            .artifactStreamId(ARTIFACT_STREAM_ID)
            .artifactFilter(ARTIFACT_FILTER)
            .build(),
        ArtifactSelection.builder().type(LAST_DEPLOYED).serviceId(SERVICE_ID).workflowId(WORKFLOW_ID).build()));

    when(workflowService.readWorkflowWithoutOrchestration(APP_ID, WORKFLOW_ID)).thenReturn(workflow);

    workflowWebhookConditionTrigger = triggerService.save(workflowWebhookConditionTrigger);

    when(artifactStreamService.get(APP_ID, ARTIFACT_STREAM_ID)).thenReturn(buildJenkinsArtifactStream());
    when(artifactService.fetchLastCollectedApprovedArtifactForArtifactStream(
             APP_ID, ARTIFACT_STREAM_ID, artifactStream.getSourceName()))
        .thenReturn(artifact);
    when(workflowExecutionService.triggerEnvExecution(anyString(), anyString(), any(ExecutionArgs.class)))
        .thenReturn(aWorkflowExecution().withAppId(APP_ID).withStatus(SUCCESS).build());
    when(workflowExecutionService.listExecutions(any(PageRequest.class), anyBoolean()))
        .thenReturn(aPageResponse()
                        .withResponse(singletonList(
                            aWorkflowExecution().withAppId(APP_ID).withExecutionArgs(executionArgs).build()))
                        .build());
    when(artifactStreamService.get(APP_ID, ARTIFACT_STREAM_ID)).thenReturn(artifactStream);
    when(pipelineService.readPipeline(APP_ID, PIPELINE_ID, true)).thenReturn(buildPipeline());
    when(artifactService.getArtifactByBuildNumber(APP_ID, ARTIFACT_STREAM_ID, "123")).thenReturn(artifact);

    triggerService.triggerExecutionByWebHook(
        APP_ID, workflowWebhookConditionTrigger.getWebHookToken(), ImmutableMap.of("Catalog", "123"), new HashMap<>());

    verify(workflowExecutionService).triggerEnvExecution(anyString(), anyString(), any(ExecutionArgs.class));
    verify(artifactStreamService, times(5)).get(APP_ID, ARTIFACT_STREAM_ID);
    verify(artifactService)
        .getArtifactByBuildNumber(APP_ID, ARTIFACT_STREAM_ID, artifactStream.getSourceName(), ARTIFACT_FILTER, false);

    verify(workflowExecutionService).obtainLastGoodDeployedArtifacts(APP_ID, WORKFLOW_ID);
    verify(artifactService, times(2))
        .getArtifactByBuildNumber(APP_ID, ARTIFACT_STREAM_ID, ARTIFACT_SOURCE_NAME, "123", false);
    verify(workflowService).readWorkflowWithoutOrchestration(APP_ID, WORKFLOW_ID);
  }

  @Test
  public void shouldGetTriggersHasPipelineAction() {
    pipelineCondTrigger.setArtifactSelections(
        asList(ArtifactSelection.builder().serviceId(SERVICE_ID).type(PIPELINE_SOURCE).build(),
            ArtifactSelection.builder()
                .type(LAST_COLLECTED)
                .serviceId(SERVICE_ID)
                .artifactStreamId(ARTIFACT_STREAM_ID)
                .artifactFilter(ARTIFACT_FILTER)
                .build(),
            ArtifactSelection.builder().type(LAST_DEPLOYED).serviceId(SERVICE_ID).workflowId(PIPELINE_ID).build()));

    triggerService.save(pipelineCondTrigger);

    List<Trigger> triggersHasPipelineAction = triggerService.getTriggersHasPipelineAction(APP_ID, PIPELINE_ID);
    assertThat(triggersHasPipelineAction).isNotEmpty();
  }

  @Test
  public void shouldGetTriggersHasArtifactStreamAction() {
    artifactConditionTrigger.setArtifactSelections(
        asList(ArtifactSelection.builder().serviceId(SERVICE_ID).type(PIPELINE_SOURCE).build(),
            ArtifactSelection.builder()
                .type(LAST_COLLECTED)
                .serviceId(SERVICE_ID)
                .artifactStreamId(ARTIFACT_STREAM_ID)
                .artifactFilter(ARTIFACT_FILTER)
                .build(),
            ArtifactSelection.builder().type(LAST_DEPLOYED).serviceId(SERVICE_ID).workflowId(PIPELINE_ID).build()));

    triggerService.save(artifactConditionTrigger);

    List<Trigger> triggersHasArtifactStreamAction =
        triggerService.getTriggersHasArtifactStreamAction(APP_ID, ARTIFACT_STREAM_ID);

    assertThat(triggersHasArtifactStreamAction).isNotEmpty();
  }

  @Test(expected = WingsException.class)
  public void shouldValidateArtifactStreamSelections() {
    webhookConditionTrigger.setArtifactSelections(
        asList(ArtifactSelection.builder().type(LAST_COLLECTED).artifactFilter(ARTIFACT_FILTER).build(),
            ArtifactSelection.builder().type(LAST_DEPLOYED).build()));

    triggerService.save(webhookConditionTrigger);
  }

  @Test(expected = WingsException.class)
  public void shouldValidateUpdateArtifactStreamSelections() {
    webhookConditionTrigger.setArtifactSelections(
        asList(ArtifactSelection.builder().type(LAST_COLLECTED).artifactFilter(ARTIFACT_FILTER).build(),
            ArtifactSelection.builder().type(LAST_DEPLOYED).workflowId(PIPELINE_ID).build()));

    triggerService.update(webhookConditionTrigger);
  }

  @Test
  public void shouldListPipelineWebhookParameters() {
    setPipelineStages(pipeline);
    when(pipelineService.readPipeline(APP_ID, PIPELINE_ID, true)).thenReturn(pipeline);

    WebhookParameters webhookParameters =
        triggerService.listWebhookParameters(APP_ID, PIPELINE_ID, PIPELINE, BITBUCKET, PULL_REQUEST);
    assertThat(webhookParameters.getParams()).isNotNull().contains("MyVar");
    assertThat(webhookParameters.getExpressions()).isNotNull().contains(WebhookParameters.PULL_REQUEST_ID);
    verify(pipelineService).readPipeline(anyString(), anyString(), anyBoolean());
    verify(workflowService).readWorkflow(APP_ID, WORKFLOW_ID);
  }

  @Test
  public void shouldListWorkflowWebhookParameters() {
    WebhookParameters webhookParameters =
        triggerService.listWebhookParameters(APP_ID, WORKFLOW_ID, ORCHESTRATION, BITBUCKET, PULL_REQUEST);
    assertThat(webhookParameters.getParams()).isNotNull().contains("MyVar");
    assertThat(webhookParameters.getExpressions()).isNotNull().contains(WebhookParameters.PULL_REQUEST_ID);
    verify(workflowService).readWorkflow(APP_ID, WORKFLOW_ID);
  }

  @Test
  public void shouldListWorkflowGitWebhookParameters() {
    WebhookParameters webhookParameters =
        triggerService.listWebhookParameters(APP_ID, WORKFLOW_ID, ORCHESTRATION, GITHUB, PULL_REQUEST);
    assertThat(webhookParameters.getParams()).isNotNull().contains("MyVar");
    assertThat(webhookParameters.getExpressions()).isNotNull().contains(WebhookParameters.GH_PR_ID);
    verify(workflowService).readWorkflow(APP_ID, WORKFLOW_ID);
  }

  @Test
  public void shouldSaveNewInstanceTrigger() {
    when(infrastructureMappingService.get(APP_ID, INFRA_MAPPING_ID))
        .thenReturn(anAwsInfrastructureMapping()
                        .withUuid(INFRA_MAPPING_ID)
                        .withServiceId(SERVICE_ID)
                        .withDeploymentType(SSH.name())
                        .withComputeProviderType("AWS")
                        .build());

    Trigger trigger = triggerService.save(newInstanceTrigger);
    assertThat(trigger.getUuid()).isEqualTo(TRIGGER_ID);
    assertThat(trigger.getCondition()).isInstanceOf(NewInstanceTriggerCondition.class);
    assertThat(trigger.getServiceInfraWorkflows()).isNotNull();
    verify(infrastructureMappingService).get(APP_ID, INFRA_MAPPING_ID);
    verify(workflowService, times(2)).readWorkflow(APP_ID, WORKFLOW_ID);
  }

  @Test
  public void shouldTriggerExecutionByServiceInfra() {
    when(infrastructureMappingService.get(APP_ID, INFRA_MAPPING_ID))
        .thenReturn(anAwsInfrastructureMapping()
                        .withUuid(INFRA_MAPPING_ID)
                        .withServiceId(SERVICE_ID)
                        .withEnvId(ENV_ID)
                        .withDeploymentType(SSH.name())
                        .withComputeProviderType("AWS")
                        .build());

    triggerService.save(newInstanceTrigger);

    when(workflowExecutionService.fetchWorkflowExecution(APP_ID, asList(SERVICE_ID), asList(ENV_ID), WORKFLOW_ID))
        .thenReturn(aWorkflowExecution().build());
    assertThat(triggerService.triggerExecutionByServiceInfra(APP_ID, INFRA_MAPPING_ID)).isTrue();
    verify(infrastructureMappingService, times(2)).get(APP_ID, INFRA_MAPPING_ID);
    verify(workflowExecutionService).triggerEnvExecution(anyString(), anyString(), any(ExecutionArgs.class));
  }

  @Test
  public void shouldEnvironmentReferencedByTrigger() {
    artifactConditionTrigger.setWorkflowVariables(ImmutableMap.of("Environment", ENV_ID));
    triggerService.save(artifactConditionTrigger);
    List<String> triggerNames = triggerService.isEnvironmentReferenced(APP_ID, ENV_ID);
    assertThat(triggerNames).isNotEmpty();
  }

  @Test
  public void shouldTriggerWorkflowExecutionByBitBucketWebhook() {
    ExecutionArgs executionArgs = new ExecutionArgs();
    when(workflowService.readWorkflowWithoutOrchestration(APP_ID, WORKFLOW_ID)).thenReturn(workflow);

    WebHookTriggerCondition webHookTriggerCondition =
        (WebHookTriggerCondition) workflowWebhookConditionTrigger.getCondition();

    webHookTriggerCondition.setWebhookSource(WebhookSource.BITBUCKET);
    webHookTriggerCondition.setEventTypes(Arrays.asList(WebhookEventType.PULL_REQUEST));

    Trigger savedTrigger = triggerService.save(workflowWebhookConditionTrigger);
    assertThat(savedTrigger).isNotNull();
    assertThat(savedTrigger.getWorkflowVariables()).isNotEmpty().containsKey("MyVar");

    Map<String, String> parameters = new HashMap<>();
    parameters.put("MyVar", "MyValue");
    executionArgs.setWorkflowVariables(parameters);

    triggerService.triggerExecutionByWebHook(workflowWebhookConditionTrigger, parameters);

    verify(workflowExecutionService).triggerEnvExecution(anyString(), anyString(), any(ExecutionArgs.class));
  }

  @Test
  public void shouldTriggerTemplatedWorkflowExecutionByBitBucketWebhook() {
    ExecutionArgs executionArgs = new ExecutionArgs();

    workflow.setTemplateExpressions(asList(TemplateExpression.builder()
                                               .fieldName("envId")
                                               .expression("${Environment}")
                                               .metadata(ImmutableMap.of("entityType", "ENVIRONMENT"))
                                               .build(),
        TemplateExpression.builder()
            .fieldName("infraMappingId")
            .expression("${ServiceInfra_SSH}")
            .metadata(ImmutableMap.of("entityType", "INFRASTRUCTURE_MAPPING"))
            .build(),
        TemplateExpression.builder()
            .fieldName("serviceId")
            .expression("${Service}")
            .metadata(ImmutableMap.of("entityType", "SERVICE"))
            .build()));

    workflow.getOrchestrationWorkflow().getUserVariables().add(
        aVariable().withEntityType(ENVIRONMENT).withName("Environment").build());
    workflow.getOrchestrationWorkflow().getUserVariables().add(
        aVariable().withEntityType(SERVICE).withName("Service").build());
    workflow.getOrchestrationWorkflow().getUserVariables().add(
        aVariable().withEntityType(INFRASTRUCTURE_MAPPING).withName("ServiceInfra_Ssh").build());

    when(workflowService.readWorkflowWithoutOrchestration(APP_ID, WORKFLOW_ID)).thenReturn(workflow);
    when(workflowService.readWorkflow(APP_ID, WORKFLOW_ID)).thenReturn(workflow);
    when(environmentService.getEnvironmentByName(APP_ID, ENV_NAME, false))
        .thenReturn(Environment.Builder.anEnvironment().withAppId(APP_ID).withUuid(ENV_ID).build());
    when(infrastructureMappingService.get(APP_ID, INFRA_MAPPING_ID))
        .thenReturn(anAwsInfrastructureMapping()
                        .withUuid(INFRA_MAPPING_ID)
                        .withServiceId(SERVICE_ID)
                        .withDeploymentType(SSH.name())
                        .withComputeProviderType("AWS")
                        .build());

    Map<String, String> workflowVariables = new HashMap<>();
    workflowVariables.put("Environment", ENV_NAME);
    workflowVariables.put("Service", SERVICE_ID);
    workflowVariables.put("ServiceInfra_Ssh", INFRA_MAPPING_ID);

    workflowWebhookConditionTrigger.setWorkflowVariables(workflowVariables);

    WebHookTriggerCondition webHookTriggerCondition =
        (WebHookTriggerCondition) workflowWebhookConditionTrigger.getCondition();

    webHookTriggerCondition.setWebhookSource(WebhookSource.BITBUCKET);
    webHookTriggerCondition.setEventTypes(Arrays.asList(WebhookEventType.PULL_REQUEST));

    triggerService.save(workflowWebhookConditionTrigger);

    Map<String, String> parameters = new HashMap<>();
    parameters.put("MyVar", "MyValue");
    executionArgs.setWorkflowVariables(parameters);

    triggerService.triggerExecutionByWebHook(workflowWebhookConditionTrigger, parameters);

    verify(workflowExecutionService).triggerEnvExecution(anyString(), anyString(), any(ExecutionArgs.class));
    verify(environmentService).getEnvironmentByName(APP_ID, ENV_NAME, false);
    verify(infrastructureMappingService).get(APP_ID, INFRA_MAPPING_ID);
    verify(serviceResourceService).get(APP_ID, SERVICE_ID, false);
  }
}
