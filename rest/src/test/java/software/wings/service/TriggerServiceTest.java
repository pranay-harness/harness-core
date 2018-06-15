package software.wings.service;

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
import static software.wings.beans.BasicOrchestrationWorkflow.BasicOrchestrationWorkflowBuilder.aBasicOrchestrationWorkflow;
import static software.wings.beans.EntityType.ENVIRONMENT;
import static software.wings.beans.PhaseStep.PhaseStepBuilder.aPhaseStep;
import static software.wings.beans.PhaseStepType.POST_DEPLOYMENT;
import static software.wings.beans.PhaseStepType.PRE_DEPLOYMENT;
import static software.wings.beans.Variable.VariableBuilder.aVariable;
import static software.wings.beans.Workflow.WorkflowBuilder.aWorkflow;
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
import static software.wings.dl.PageResponse.PageResponseBuilder.aPageResponse;
import static software.wings.sm.ExecutionStatus.SUCCESS;
import static software.wings.sm.StateType.ENV_STATE;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_FILTER;
import static software.wings.utils.WingsTestConstants.ARTIFACT_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_SOURCE_NAME;
import static software.wings.utils.WingsTestConstants.ARTIFACT_STREAM_ID;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.FILE_ID;
import static software.wings.utils.WingsTestConstants.FILE_NAME;
import static software.wings.utils.WingsTestConstants.INFRA_MAPPING_ID;
import static software.wings.utils.WingsTestConstants.PIPELINE_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID_CHANGED;
import static software.wings.utils.WingsTestConstants.SETTING_ID;
import static software.wings.utils.WingsTestConstants.TRIGGER_ID;
import static software.wings.utils.WingsTestConstants.TRIGGER_NAME;
import static software.wings.utils.WingsTestConstants.WORKFLOW_ID;
import static software.wings.utils.WingsTestConstants.WORKFLOW_NAME;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import io.harness.distribution.idempotence.IdempotentLock;
import io.harness.distribution.idempotence.UnableToRegisterIdempotentOperationException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mongodb.morphia.query.FieldEnd;
import org.mongodb.morphia.query.Sort;
import org.quartz.JobDetail;
import org.quartz.TriggerKey;
import software.wings.WingsBaseTest;
import software.wings.beans.ExecutionArgs;
import software.wings.beans.Pipeline;
import software.wings.beans.PipelineStage;
import software.wings.beans.PipelineStage.PipelineStageElement;
import software.wings.beans.Service;
import software.wings.beans.Variable;
import software.wings.beans.WebHookToken;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.JenkinsArtifactStream;
import software.wings.beans.trigger.ArtifactSelection;
import software.wings.beans.trigger.ArtifactTriggerCondition;
import software.wings.beans.trigger.NewInstanceTriggerCondition;
import software.wings.beans.trigger.PipelineTriggerCondition;
import software.wings.beans.trigger.ScheduledTriggerCondition;
import software.wings.beans.trigger.ServiceInfraWorkflow;
import software.wings.beans.trigger.Trigger;
import software.wings.beans.trigger.WebHookTriggerCondition;
import software.wings.beans.trigger.WebhookParameters;
import software.wings.common.Constants;
import software.wings.common.MongoIdempotentRegistry;
import software.wings.dl.HQuery;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.scheduler.JobScheduler;
import software.wings.scheduler.ScheduledTriggerJob;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.TriggerService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.WorkflowService;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Created by sgurubelli on 10/26/17.
 */
public class TriggerServiceTest extends WingsBaseTest {
  @Mock private WingsPersistence wingsPersistence;
  @Mock private HQuery query;
  @Mock private FieldEnd end;
  @Mock private JobScheduler jobScheduler;
  @Mock private PipelineService pipelineService;
  @Mock private WorkflowExecutionService workflowExecutionService;
  @Mock private ArtifactStreamService artifactStreamService;
  @Mock private ArtifactService artifactService;
  @Mock private ServiceResourceService serviceResourceService;
  @Mock private WorkflowService workflowService;
  @Mock private InfrastructureMappingService infrastructureMappingService;
  @Mock private WorkflowExecution workflowExecution;
  @Mock private MongoIdempotentRegistry<String> idempotentRegistry;

  @Inject @InjectMocks private TriggerService triggerService;

  private Trigger defaultTrigger = Trigger.builder().uuid(TRIGGER_ID).appId(APP_ID).name(TRIGGER_NAME).build();

  private Trigger artifactConditionTrigger = buildArtifactTrigger();

  private Trigger workflowArtifactConditionTrigger = buildWorkflowArtifactTrigger();

  private Trigger pipelineCondTrigger = buildPipelineCondTrigger();

  private Trigger scheduledConditionTrigger = buildScheduledCondTrigger();

  private Trigger webhookConditionTrigger = buildWebhookCondTrigger();

  private Trigger workflowWebhookConditionTrigger = buildWorkflowWebhookTrigger();

  private Trigger newInstanceTrigger = buildNewInstanceTrigger();

  private Pipeline pipeline = buildPipeline();

  private Workflow workflow = buildWorkflow();

  private JenkinsArtifactStream jenkinsArtifactStream = buildJenkinsArtifactStream();

  @Before
  public void setUp() throws UnableToRegisterIdempotentOperationException {
    when(wingsPersistence.createQuery(Trigger.class)).thenReturn(query);
    when(wingsPersistence.createQuery(WorkflowExecution.class)).thenReturn(query);
    when(query.filter(any(), any())).thenReturn(query);
    when(query.field(any())).thenReturn(end);
    when(end.lessThan(any())).thenReturn(query);
    when(end.in(any())).thenReturn(query);
    when(query.order(any(Sort.class))).thenReturn(query);
    when(query.get(any())).thenReturn(workflowExecution);
    when(pipelineService.readPipeline(APP_ID, PIPELINE_ID, true)).thenReturn(pipeline);
    when(pipelineService.readPipeline(APP_ID, PIPELINE_ID, false)).thenReturn(pipeline);
    when(workflowService.readWorkflow(APP_ID, WORKFLOW_ID)).thenReturn(workflow);
    when(artifactStreamService.get(APP_ID, ARTIFACT_STREAM_ID)).thenReturn(jenkinsArtifactStream);
    when(serviceResourceService.get(APP_ID, SERVICE_ID, false))
        .thenReturn(Service.builder().uuid(SERVICE_ID).name("Catalog").build());
    when(idempotentRegistry.create(any(), any(), any(), any()))
        .thenReturn(IdempotentLock.<String>builder().registry(idempotentRegistry).resultData(Optional.empty()).build());
  }

  @Test
  public void shouldListTriggers() {
    PageRequest<Trigger> pageRequest = new PageRequest<>();
    when(wingsPersistence.query(Trigger.class, pageRequest))
        .thenReturn(aPageResponse().withResponse(asList(defaultTrigger)).build());
    PageResponse<Trigger> triggers = triggerService.list(pageRequest);
    assertThat(triggers.size()).isEqualTo(1);
  }

  @Test
  public void shouldGet() {
    when(wingsPersistence.get(Trigger.class, APP_ID, TRIGGER_ID)).thenReturn(defaultTrigger);
    Trigger trigger = triggerService.get(APP_ID, TRIGGER_ID);
    assertThat(trigger.getName()).isEqualTo(TRIGGER_NAME);
    assertThat(trigger.getAppId()).isEqualTo(APP_ID);
    assertThat(trigger.getUuid()).isEqualTo(TRIGGER_ID);
    verify(wingsPersistence).get(Trigger.class, APP_ID, TRIGGER_ID);
  }

  @Test
  public void shouldGetExcludeHostsWithSameArtifact() {
    defaultTrigger.setExcludeHostsWithSameArtifact(true);
    when(wingsPersistence.get(Trigger.class, APP_ID, TRIGGER_ID)).thenReturn(defaultTrigger);
    Trigger trigger = triggerService.get(APP_ID, TRIGGER_ID);
    assertThat(trigger.getName()).isEqualTo(TRIGGER_NAME);
    assertThat(trigger.getAppId()).isEqualTo(APP_ID);
    assertThat(trigger.getUuid()).isEqualTo(TRIGGER_ID);
    assertThat(trigger.getUuid()).isEqualTo(TRIGGER_ID);
    verify(wingsPersistence).get(Trigger.class, APP_ID, TRIGGER_ID);
  }
  @Test
  public void shouldSaveArtifactConditionTrigger() {
    when(wingsPersistence.saveAndGet(any(), any(Trigger.class))).thenReturn(artifactConditionTrigger);

    Trigger trigger = triggerService.save(artifactConditionTrigger);
    assertThat(trigger.getUuid()).isEqualTo(TRIGGER_ID);
    assertThat(trigger.getCondition()).isInstanceOf(ArtifactTriggerCondition.class);
    assertThat(((ArtifactTriggerCondition) trigger.getCondition()).getArtifactFilter())
        .isNotNull()
        .isEqualTo(ARTIFACT_FILTER);
    assertThat(((ArtifactTriggerCondition) trigger.getCondition()).getArtifactStreamId())
        .isNotNull()
        .isEqualTo(ARTIFACT_STREAM_ID);
    verify(wingsPersistence).saveAndGet(any(), any(Trigger.class));
  }

  @Test
  public void shouldSaveWorkflowArtifactWConditionTrigger() {
    when(wingsPersistence.saveAndGet(any(), any(Trigger.class))).thenReturn(workflowArtifactConditionTrigger);

    Trigger trigger = triggerService.save(workflowArtifactConditionTrigger);
    assertThat(trigger.getUuid()).isEqualTo(TRIGGER_ID);
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
    verify(wingsPersistence).saveAndGet(any(), any(Trigger.class));
  }

  @Test
  public void shouldUpdateArtifactConditionTrigger() {
    ArtifactTriggerCondition artifactTriggerCondition =
        (ArtifactTriggerCondition) artifactConditionTrigger.getCondition();
    artifactTriggerCondition.setRegex(true);
    when(wingsPersistence.saveAndGet(any(), any(Trigger.class))).thenReturn(artifactConditionTrigger);

    artifactConditionTrigger.setArtifactSelections(
        asList(ArtifactSelection.builder().serviceId(SERVICE_ID).type(ARTIFACT_SOURCE).build(),
            ArtifactSelection.builder()
                .type(LAST_COLLECTED)
                .serviceId(SERVICE_ID)
                .artifactStreamId(ARTIFACT_STREAM_ID)
                .artifactFilter(ARTIFACT_FILTER)
                .build()));

    when(wingsPersistence.get(Trigger.class, defaultTrigger.getAppId(), defaultTrigger.getUuid()))
        .thenReturn(artifactConditionTrigger);

    Trigger updatedTrigger = triggerService.update(artifactConditionTrigger);

    assertThat(updatedTrigger.getUuid()).isEqualTo(TRIGGER_ID);
    assertThat(updatedTrigger.getCondition()).isInstanceOf(ArtifactTriggerCondition.class);
    assertThat(((ArtifactTriggerCondition) updatedTrigger.getCondition()).getArtifactFilter())
        .isNotNull()
        .isEqualTo(ARTIFACT_FILTER);
    assertThat(((ArtifactTriggerCondition) updatedTrigger.getCondition()).isRegex()).isTrue();
    assertThat(((ArtifactTriggerCondition) updatedTrigger.getCondition()).getArtifactStreamId())
        .isNotNull()
        .isEqualTo(ARTIFACT_STREAM_ID);
    assertThat(updatedTrigger.getArtifactSelections())
        .isNotNull()
        .extracting(artifactSelection -> artifactSelection.getType())
        .contains(ARTIFACT_SOURCE);
    verify(wingsPersistence).saveAndGet(any(), any(Trigger.class));
  }
  @Test
  public void shouldUpdateWorkflowArtifactConditionTrigger() {
    when(wingsPersistence.saveAndGet(any(), any(Trigger.class))).thenReturn(workflowArtifactConditionTrigger);
    workflowArtifactConditionTrigger.setArtifactSelections(
        asList(ArtifactSelection.builder().serviceId(SERVICE_ID).type(ARTIFACT_SOURCE).build(),
            ArtifactSelection.builder()
                .type(LAST_COLLECTED)
                .serviceId(SERVICE_ID)
                .artifactStreamId(ARTIFACT_STREAM_ID)
                .artifactFilter(ARTIFACT_FILTER)
                .build(),
            ArtifactSelection.builder().serviceId(SERVICE_ID).type(LAST_DEPLOYED).workflowId(WORKFLOW_ID).build()));

    when(wingsPersistence.get(Trigger.class, APP_ID, TRIGGER_ID)).thenReturn(workflowArtifactConditionTrigger);

    when(wingsPersistence.get(Workflow.class, APP_ID, WORKFLOW_ID)).thenReturn(workflow);

    Trigger updatedTrigger = triggerService.update(workflowArtifactConditionTrigger);

    assertThat(updatedTrigger.getUuid()).isEqualTo(TRIGGER_ID);
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
    verify(wingsPersistence).saveAndGet(any(), any(Trigger.class));
  }

  @Test
  public void shouldSavePipelineConditionTrigger() {
    when(wingsPersistence.saveAndGet(any(), any(Trigger.class))).thenReturn(pipelineCondTrigger);
    Trigger trigger = triggerService.save(pipelineCondTrigger);
    assertThat(trigger.getUuid()).isEqualTo(TRIGGER_ID);
    assertThat(trigger.getCondition()).isInstanceOf(PipelineTriggerCondition.class);
    assertThat(((PipelineTriggerCondition) trigger.getCondition()).getPipelineId()).isNotNull().isEqualTo(PIPELINE_ID);
    verify(wingsPersistence).saveAndGet(any(), any(Trigger.class));
  }

  @Test
  public void shouldUpdatePipelineConditionTrigger() {
    when(wingsPersistence.saveAndGet(any(), any(Trigger.class))).thenReturn(pipelineCondTrigger);
    pipelineCondTrigger.setArtifactSelections(
        asList(ArtifactSelection.builder().type(PIPELINE_SOURCE).serviceId(SERVICE_ID).build(),
            ArtifactSelection.builder().serviceId(SERVICE_ID).type(LAST_DEPLOYED).pipelineId(PIPELINE_ID).build()));
    when(wingsPersistence.get(Trigger.class, defaultTrigger.getAppId(), defaultTrigger.getUuid()))
        .thenReturn(pipelineCondTrigger);

    Trigger updatedTrigger = triggerService.update(pipelineCondTrigger);

    assertThat(updatedTrigger.getUuid()).isEqualTo(TRIGGER_ID);
    assertThat(updatedTrigger.getCondition()).isInstanceOf(PipelineTriggerCondition.class);
    assertThat(updatedTrigger.getArtifactSelections())
        .isNotNull()
        .extracting(artifactSelection -> artifactSelection.getType())
        .contains(PIPELINE_SOURCE, LAST_DEPLOYED);
    verify(wingsPersistence).saveAndGet(any(), any(Trigger.class));
    verify(wingsPersistence).get(Trigger.class, updatedTrigger.getAppId(), updatedTrigger.getUuid());
  }

  @Test
  public void shouldSaveScheduledConditionTrigger() {
    when(wingsPersistence.saveAndGet(any(), any(Trigger.class))).thenReturn(scheduledConditionTrigger);

    Trigger trigger = triggerService.save(scheduledConditionTrigger);
    assertThat(trigger.getUuid()).isEqualTo(TRIGGER_ID);
    assertThat(trigger.getCondition()).isInstanceOf(ScheduledTriggerCondition.class);
    assertThat(((ScheduledTriggerCondition) trigger.getCondition()).getCronDescription()).isNotNull();
    assertThat(((ScheduledTriggerCondition) trigger.getCondition()).getCronExpression())
        .isNotNull()
        .isEqualTo("* * * * ?");
    verify(wingsPersistence).saveAndGet(any(), any(Trigger.class));
    verify(jobScheduler).scheduleJob(any(JobDetail.class), any(org.quartz.Trigger.class));
  }

  @Test
  public void shouldUpdateScheduledConditionTrigger() {
    when(wingsPersistence.saveAndGet(any(), any(Trigger.class))).thenReturn(scheduledConditionTrigger);
    scheduledConditionTrigger.setArtifactSelections(asList(ArtifactSelection.builder()
                                                               .type(LAST_COLLECTED)
                                                               .serviceId(SERVICE_ID)
                                                               .artifactStreamId(ARTIFACT_STREAM_ID)
                                                               .artifactFilter(ARTIFACT_FILTER)
                                                               .build(),
        ArtifactSelection.builder().type(LAST_DEPLOYED).serviceId(SERVICE_ID).workflowId(PIPELINE_ID).build()));

    when(wingsPersistence.get(Trigger.class, defaultTrigger.getAppId(), defaultTrigger.getUuid()))
        .thenReturn(scheduledConditionTrigger);

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
    verify(wingsPersistence).saveAndGet(any(), any(Trigger.class));
    verify(jobScheduler).rescheduleJob(any(TriggerKey.class), any(org.quartz.Trigger.class));
  }

  @Test
  public void shouldSaveWebhookConditionTrigger() {
    when(wingsPersistence.saveAndGet(any(), any(Trigger.class))).thenReturn(webhookConditionTrigger);
    setPipelineStages();

    Trigger trigger = triggerService.save(webhookConditionTrigger);
    assertThat(trigger.getUuid()).isEqualTo(TRIGGER_ID);
    assertThat(trigger.getCondition()).isInstanceOf(WebHookTriggerCondition.class);
    assertThat(((WebHookTriggerCondition) trigger.getCondition()).getWebHookToken()).isNotNull();
    assertThat(((WebHookTriggerCondition) trigger.getCondition()).getWebHookToken().getWebHookToken()).isNotNull();
    verify(wingsPersistence).saveAndGet(any(), any(Trigger.class));
    verify(pipelineService, times(2)).readPipeline(APP_ID, PIPELINE_ID, true);
  }

  @Test
  public void shouldSaveWorkflowWebhookConditionTrigger() {
    when(wingsPersistence.saveAndGet(any(), any(Trigger.class))).thenReturn(workflowWebhookConditionTrigger);

    Trigger trigger = triggerService.save(workflowWebhookConditionTrigger);
    assertThat(trigger.getUuid()).isEqualTo(TRIGGER_ID);
    assertThat(trigger.getCondition()).isInstanceOf(WebHookTriggerCondition.class);
    assertThat(((WebHookTriggerCondition) trigger.getCondition()).getWebHookToken()).isNotNull();
    assertThat(((WebHookTriggerCondition) trigger.getCondition()).getWebHookToken().getWebHookToken()).isNotNull();
    verify(wingsPersistence).saveAndGet(any(), any(Trigger.class));
  }

  @Test
  public void shouldUpdateWebhookConditionTrigger() {
    when(wingsPersistence.saveAndGet(any(), any(Trigger.class))).thenReturn(webhookConditionTrigger);
    webhookConditionTrigger.setArtifactSelections(asList(ArtifactSelection.builder()
                                                             .type(LAST_COLLECTED)
                                                             .artifactStreamId(ARTIFACT_STREAM_ID)
                                                             .serviceId(SERVICE_ID)
                                                             .artifactFilter(ARTIFACT_FILTER)
                                                             .build(),
        ArtifactSelection.builder().type(LAST_DEPLOYED).serviceId(SERVICE_ID).workflowId(PIPELINE_ID).build()));

    when(wingsPersistence.get(Trigger.class, defaultTrigger.getAppId(), defaultTrigger.getUuid()))
        .thenReturn(webhookConditionTrigger);

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
    verify(wingsPersistence).saveAndGet(any(), any(Trigger.class));
  }

  @Test
  public void shouldUpdateScheduledConditionTriggerToOtherType() {
    when(wingsPersistence.saveAndGet(any(), any(Trigger.class))).thenReturn(scheduledConditionTrigger);
    Trigger trigger = triggerService.save(scheduledConditionTrigger);
    assertThat(trigger.getUuid()).isEqualTo(TRIGGER_ID);
    assertThat(trigger.getCondition()).isInstanceOf(ScheduledTriggerCondition.class);
    assertThat(((ScheduledTriggerCondition) trigger.getCondition()).getCronDescription()).isNotNull();
    assertThat(((ScheduledTriggerCondition) trigger.getCondition()).getCronExpression())
        .isNotNull()
        .isEqualTo("* * * * ?");
    verify(wingsPersistence).saveAndGet(any(), any(Trigger.class));
    verify(jobScheduler).scheduleJob(any(JobDetail.class), any(org.quartz.Trigger.class));

    when(wingsPersistence.get(Trigger.class, trigger.getAppId(), trigger.getUuid()))
        .thenReturn(scheduledConditionTrigger);

    webhookConditionTrigger.setArtifactSelections(asList(ArtifactSelection.builder()
                                                             .type(LAST_COLLECTED)
                                                             .artifactStreamId(ARTIFACT_STREAM_ID)
                                                             .serviceId(SERVICE_ID)
                                                             .artifactFilter(ARTIFACT_FILTER)
                                                             .build(),
        ArtifactSelection.builder().type(LAST_DEPLOYED).serviceId(SERVICE_ID).workflowId(PIPELINE_ID).build()));

    when(wingsPersistence.saveAndGet(any(), any(Trigger.class))).thenReturn(webhookConditionTrigger);
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
    when(wingsPersistence.saveAndGet(any(), any(Trigger.class))).thenReturn(artifactConditionTrigger);
    Trigger trigger = triggerService.save(artifactConditionTrigger);
    assertThat(trigger.getUuid()).isEqualTo(TRIGGER_ID);
    assertThat(trigger.getCondition()).isInstanceOf(ArtifactTriggerCondition.class);
    assertThat(((ArtifactTriggerCondition) trigger.getCondition()).getArtifactFilter())
        .isNotNull()
        .isEqualTo(ARTIFACT_FILTER);
    assertThat(((ArtifactTriggerCondition) trigger.getCondition()).getArtifactStreamId())
        .isNotNull()
        .isEqualTo(ARTIFACT_STREAM_ID);
    verify(wingsPersistence).saveAndGet(any(), any(Trigger.class));

    when(wingsPersistence.get(Trigger.class, trigger.getAppId(), trigger.getUuid()))
        .thenReturn(artifactConditionTrigger);
    when(wingsPersistence.saveAndGet(any(), any(Trigger.class))).thenReturn(scheduledConditionTrigger);

    Trigger updatedTrigger = triggerService.update(scheduledConditionTrigger);
    assertThat(updatedTrigger.getUuid()).isEqualTo(TRIGGER_ID);
    assertThat(updatedTrigger.getCondition()).isInstanceOf(ScheduledTriggerCondition.class);
    assertThat(((ScheduledTriggerCondition) updatedTrigger.getCondition()).getCronDescription()).isNotNull();
    assertThat(((ScheduledTriggerCondition) updatedTrigger.getCondition()).getCronExpression())
        .isNotNull()
        .isEqualTo("* * * * ?");
    verify(wingsPersistence, times(2)).saveAndGet(any(), any(Trigger.class));
    verify(jobScheduler).scheduleJob(any(JobDetail.class), any(org.quartz.Trigger.class));
  }

  @Test
  public void shouldDeleteScheduleTrigger() {
    when(wingsPersistence.get(Trigger.class, APP_ID, TRIGGER_ID)).thenReturn(scheduledConditionTrigger);
    when(wingsPersistence.delete(Trigger.class, TRIGGER_ID)).thenReturn(true);

    triggerService.delete(APP_ID, TRIGGER_ID);
    verify(wingsPersistence).delete(Trigger.class, TRIGGER_ID);
  }

  @Test
  public void shouldDeleteArtifactTrigger() {
    when(wingsPersistence.get(Trigger.class, APP_ID, TRIGGER_ID)).thenReturn(artifactConditionTrigger);
    when(wingsPersistence.delete(Trigger.class, TRIGGER_ID)).thenReturn(true);

    triggerService.delete(APP_ID, TRIGGER_ID);
    verify(wingsPersistence).delete(Trigger.class, TRIGGER_ID);
  }

  @Test
  public void shouldDeleteTriggersForPipeline() {
    when(wingsPersistence.saveAndGet(any(), any(Trigger.class))).thenReturn(pipelineCondTrigger);

    Trigger trigger = triggerService.save(pipelineCondTrigger);
    assertThat(trigger).isNotNull();
    assertThat(trigger.getUuid()).isEqualTo(TRIGGER_ID);
    assertThat(trigger.getAppId()).isEqualTo(APP_ID);

    when(query.asList()).thenReturn(singletonList(pipelineCondTrigger));
    when(wingsPersistence.get(Trigger.class, APP_ID, TRIGGER_ID)).thenReturn(pipelineCondTrigger);
    when(wingsPersistence.delete(Trigger.class, TRIGGER_ID)).thenReturn(true);
    when(wingsPersistence.query(any(), any(PageRequest.class)))
        .thenReturn(aPageResponse().withResponse(asList(pipelineCondTrigger)).build());

    triggerService.pruneByPipeline(APP_ID, PIPELINE_ID);
    verify(wingsPersistence, times(2)).delete(Trigger.class, TRIGGER_ID);
  }

  @Test
  public void shouldDeleteTriggersForWorkflow() {
    when(wingsPersistence.saveAndGet(any(), any(Trigger.class))).thenReturn(workflowArtifactConditionTrigger);

    Trigger trigger = triggerService.save(workflowArtifactConditionTrigger);
    assertThat(trigger).isNotNull();
    assertThat(trigger.getUuid()).isEqualTo(TRIGGER_ID);
    assertThat(trigger.getAppId()).isEqualTo(APP_ID);
    assertThat(trigger.getWorkflowId()).isEqualTo(WORKFLOW_ID);

    when(query.asList()).thenReturn(singletonList(workflowArtifactConditionTrigger));
    when(wingsPersistence.get(Trigger.class, APP_ID, TRIGGER_ID)).thenReturn(workflowArtifactConditionTrigger);
    when(wingsPersistence.delete(Trigger.class, TRIGGER_ID)).thenReturn(true);
    when(wingsPersistence.query(any(), any(PageRequest.class)))
        .thenReturn(aPageResponse().withResponse(asList(workflowArtifactConditionTrigger)).build());

    triggerService.pruneByWorkflow(APP_ID, WORKFLOW_ID);
    verify(wingsPersistence, times(1)).delete(Trigger.class, TRIGGER_ID);
  }

  @Test
  public void shouldDeleteTriggersForArtifactStream() {
    when(wingsPersistence.saveAndGet(any(), any(Trigger.class))).thenReturn(artifactConditionTrigger);

    Trigger trigger = triggerService.save(artifactConditionTrigger);
    assertThat(trigger).isNotNull();
    assertThat(trigger.getUuid()).isEqualTo(TRIGGER_ID);
    assertThat(trigger.getAppId()).isEqualTo(APP_ID);

    when(query.asList()).thenReturn(singletonList(artifactConditionTrigger));
    when(wingsPersistence.get(Trigger.class, APP_ID, TRIGGER_ID)).thenReturn(artifactConditionTrigger);
    when(wingsPersistence.delete(Trigger.class, TRIGGER_ID)).thenReturn(true);
    when(wingsPersistence.query(any(), any(PageRequest.class)))
        .thenReturn(aPageResponse().withResponse(asList(artifactConditionTrigger)).build());

    triggerService.pruneByArtifactStream(APP_ID, ARTIFACT_STREAM_ID);
    verify(wingsPersistence).delete(Trigger.class, TRIGGER_ID);
  }

  @Test
  public void shouldDeleteTriggersByApp() {
    when(wingsPersistence.saveAndGet(any(), any(Trigger.class))).thenReturn(scheduledConditionTrigger);

    Trigger trigger = triggerService.save(scheduledConditionTrigger);
    assertThat(trigger).isNotNull();
    assertThat(trigger.getUuid()).isEqualTo(TRIGGER_ID);
    assertThat(trigger.getAppId()).isEqualTo(APP_ID);

    when(query.asList()).thenReturn(singletonList(scheduledConditionTrigger));
    when(wingsPersistence.get(Trigger.class, APP_ID, TRIGGER_ID)).thenReturn(scheduledConditionTrigger);
    when(wingsPersistence.delete(Trigger.class, TRIGGER_ID)).thenReturn(true);

    triggerService.pruneByApplication(APP_ID);
    verify(wingsPersistence).delete(Trigger.class, TRIGGER_ID);
  }

  @Test
  public void shouldGenerateWebHookToken() {
    when(wingsPersistence.get(Trigger.class, APP_ID, TRIGGER_ID)).thenReturn(webhookConditionTrigger);
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
    when(wingsPersistence.query(any(), any(PageRequest.class)))
        .thenReturn(aPageResponse().withResponse(asList(artifactConditionTrigger)).build());
    when(workflowExecutionService.triggerPipelineExecution(anyString(), anyString(), any(ExecutionArgs.class)))
        .thenReturn(aWorkflowExecution().withAppId(APP_ID).withStatus(SUCCESS).build());

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
    when(wingsPersistence.query(any(), any(PageRequest.class)))
        .thenReturn(aPageResponse().withResponse(asList(artifactConditionTrigger)).build());
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

    when(wingsPersistence.query(any(), any(PageRequest.class)))
        .thenReturn(aPageResponse().withResponse(asList(artifactConditionTrigger)).build());
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

    when(wingsPersistence.query(any(), any(PageRequest.class)))
        .thenReturn(aPageResponse().withResponse(asList(artifactConditionTrigger)).build());
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

    when(wingsPersistence.query(any(), any(PageRequest.class)))
        .thenReturn(aPageResponse().withResponse(asList(artifactConditionTrigger)).build());
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

    when(wingsPersistence.query(any(), any(PageRequest.class)))
        .thenReturn(aPageResponse().withResponse(asList(artifactConditionTrigger)).build());
    when(workflowExecutionService.triggerPipelineExecution(anyString(), anyString(), any(ExecutionArgs.class)))
        .thenReturn(aWorkflowExecution().withAppId(APP_ID).withStatus(SUCCESS).build());

    triggerService.triggerExecutionPostArtifactCollectionAsync(artifact);
    verify(workflowExecutionService, times(0))
        .triggerPipelineExecution(anyString(), anyString(), any(ExecutionArgs.class));
  }

  @Test
  public void shouldTriggerExecutionPostArtifactCollectionWithArtifactMatchesArtifactFilter() {
    Artifact artifact = anArtifact()
                            .withAppId(APP_ID)
                            .withUuid(ARTIFACT_ID)
                            .withArtifactStreamId(ARTIFACT_STREAM_ID)
                            .withMetadata(ImmutableMap.of("buildNo", ARTIFACT_FILTER))
                            .build();
    when(wingsPersistence.query(any(), any(PageRequest.class)))
        .thenReturn(aPageResponse().withResponse(asList(artifactConditionTrigger)).build());
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

    when(wingsPersistence.query(any(), any(PageRequest.class)))
        .thenReturn(aPageResponse().withResponse(asList(artifactConditionTrigger)).build());
    when(artifactStreamService.get(APP_ID, ARTIFACT_STREAM_ID)).thenReturn(jenkinsArtifactStream);
    when(artifactService.fetchLastCollectedArtifactForArtifactStream(
             APP_ID, ARTIFACT_STREAM_ID, jenkinsArtifactStream.getSourceName()))
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
    verify(artifactStreamService).get(APP_ID, ARTIFACT_STREAM_ID);
    verify(artifactService)
        .fetchLastCollectedArtifactForArtifactStream(APP_ID, ARTIFACT_STREAM_ID, jenkinsArtifactStream.getSourceName());
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

    when(wingsPersistence.query(any(), any(PageRequest.class)))
        .thenReturn(aPageResponse().withResponse(asList(workflowArtifactConditionTrigger)).build());
    when(artifactStreamService.get(APP_ID, ARTIFACT_STREAM_ID)).thenReturn(jenkinsArtifactStream);
    when(artifactService.fetchLastCollectedArtifactForArtifactStream(
             APP_ID, ARTIFACT_STREAM_ID, jenkinsArtifactStream.getSourceName()))
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
    when(wingsPersistence.get(Workflow.class, APP_ID, WORKFLOW_ID)).thenReturn(workflow);

    triggerService.triggerExecutionPostArtifactCollectionAsync(artifact);
    verify(workflowExecutionService).triggerEnvExecution(anyString(), anyString(), any(ExecutionArgs.class));
    verify(artifactStreamService).get(APP_ID, ARTIFACT_STREAM_ID);
    verify(artifactService)
        .fetchLastCollectedArtifactForArtifactStream(APP_ID, ARTIFACT_STREAM_ID, jenkinsArtifactStream.getSourceName());
    verify(workflowExecutionService).obtainLastGoodDeployedArtifacts(APP_ID, WORKFLOW_ID);
  }
  @Test
  public void shouldTriggerTemplateWorkflowExecution() {
    workflow.getOrchestrationWorkflow().getUserVariables().add(
        aVariable().withName("Environment").withValue(ENV_ID).withEntityType(ENVIRONMENT).build());
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

    workflowArtifactConditionTrigger.setWorkflowVariables(
        ImmutableMap.of("Environment", ENV_ID, "Service", SERVICE_ID, "ServiceInfraStructure", INFRA_MAPPING_ID));

    when(wingsPersistence.query(any(), any(PageRequest.class)))
        .thenReturn(aPageResponse().withResponse(asList(workflowArtifactConditionTrigger)).build());
    when(artifactStreamService.get(APP_ID, ARTIFACT_STREAM_ID)).thenReturn(jenkinsArtifactStream);
    when(artifactService.fetchLastCollectedArtifactForArtifactStream(
             APP_ID, ARTIFACT_STREAM_ID, jenkinsArtifactStream.getSourceName()))
        .thenReturn(artifact);

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
    verify(artifactStreamService).get(APP_ID, ARTIFACT_STREAM_ID);
    verify(artifactService)
        .fetchLastCollectedArtifactForArtifactStream(APP_ID, ARTIFACT_STREAM_ID, jenkinsArtifactStream.getSourceName());
    verify(workflowExecutionService).obtainLastGoodDeployedArtifacts(APP_ID, WORKFLOW_ID);
    verify(workflowService).readWorkflow(APP_ID, WORKFLOW_ID);
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
    ExecutionArgs executionArgs = new ExecutionArgs();
    executionArgs.setArtifacts(singletonList(artifact));

    when(wingsPersistence.query(any(), any(PageRequest.class)))
        .thenReturn(aPageResponse().withResponse(asList(pipelineCondTrigger)).build());
    when(workflowExecutionService.triggerPipelineExecution(anyString(), anyString(), any(ExecutionArgs.class)))
        .thenReturn(aWorkflowExecution().withAppId(APP_ID).withStatus(SUCCESS).build());
    when(workflowExecutionService.listExecutions(any(PageRequest.class), anyBoolean()))
        .thenReturn(aPageResponse()
                        .withResponse(singletonList(
                            aWorkflowExecution().withAppId(APP_ID).withExecutionArgs(executionArgs).build()))
                        .build());

    triggerService.triggerExecutionPostPipelineCompletionAsync(APP_ID, PIPELINE_ID);
    verify(workflowExecutionService).triggerPipelineExecution(anyString(), anyString(), any(ExecutionArgs.class));
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

    when(wingsPersistence.query(any(), any(PageRequest.class)))
        .thenReturn(aPageResponse().withResponse(asList(pipelineCondTrigger)).build());
    when(artifactStreamService.get(APP_ID, ARTIFACT_STREAM_ID)).thenReturn(jenkinsArtifactStream);
    when(artifactService.fetchLastCollectedArtifactForArtifactStream(
             APP_ID, ARTIFACT_STREAM_ID, jenkinsArtifactStream.getSourceName()))
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
    verify(artifactStreamService).get(APP_ID, ARTIFACT_STREAM_ID);
    verify(artifactService)
        .fetchLastCollectedArtifactForArtifactStream(APP_ID, ARTIFACT_STREAM_ID, jenkinsArtifactStream.getSourceName());
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
    ExecutionArgs executionArgs = new ExecutionArgs();
    executionArgs.setArtifacts(singletonList(artifact));

    when(wingsPersistence.get(Trigger.class, APP_ID, TRIGGER_ID)).thenReturn(scheduledConditionTrigger);
    when(wingsPersistence.query(any(), any(PageRequest.class)))
        .thenReturn(aPageResponse().withResponse(asList(scheduledConditionTrigger)).build());
    when(workflowExecutionService.triggerPipelineExecution(anyString(), anyString(), any(ExecutionArgs.class)))
        .thenReturn(aWorkflowExecution().withAppId(APP_ID).withStatus(SUCCESS).build());
    when(workflowExecutionService.listExecutions(any(PageRequest.class), anyBoolean()))
        .thenReturn(aPageResponse()
                        .withResponse(singletonList(
                            aWorkflowExecution().withAppId(APP_ID).withExecutionArgs(executionArgs).build()))
                        .build());

    triggerService.triggerScheduledExecutionAsync(scheduledConditionTrigger, new Date());
    verify(idempotentRegistry).create(any(), any(), any(), any());
    verify(workflowExecutionService, times(1))
        .triggerPipelineExecution(anyString(), anyString(), any(ExecutionArgs.class));
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

    when(wingsPersistence.get(Trigger.class, APP_ID, TRIGGER_ID)).thenReturn(scheduledConditionTrigger);
    when(wingsPersistence.query(any(), any(PageRequest.class)))
        .thenReturn(aPageResponse().withResponse(asList(scheduledConditionTrigger)).build());
    when(artifactStreamService.get(APP_ID, ARTIFACT_STREAM_ID)).thenReturn(jenkinsArtifactStream);
    when(artifactService.fetchLastCollectedArtifactForArtifactStream(
             APP_ID, ARTIFACT_STREAM_ID, jenkinsArtifactStream.getSourceName()))
        .thenReturn(artifact);
    when(workflowExecutionService.triggerPipelineExecution(anyString(), anyString(), any(ExecutionArgs.class)))
        .thenReturn(aWorkflowExecution().withAppId(APP_ID).withStatus(SUCCESS).build());

    triggerService.triggerScheduledExecutionAsync(scheduledConditionTrigger, new Date());
    verify(workflowExecutionService).triggerPipelineExecution(anyString(), anyString(), any(ExecutionArgs.class));
    verify(artifactStreamService).get(APP_ID, ARTIFACT_STREAM_ID);
    verify(artifactService)
        .fetchLastCollectedArtifactForArtifactStream(APP_ID, ARTIFACT_STREAM_ID, jenkinsArtifactStream.getSourceName());
    verify(idempotentRegistry).create(any(), any(), any(), any());
  }

  @Test
  public void shouldTriggerExecutionByWebhook() {
    Artifact artifact = anArtifact()
                            .withAppId(APP_ID)
                            .withUuid(ARTIFACT_ID)
                            .withArtifactStreamId(ARTIFACT_STREAM_ID)
                            .withServiceIds(singletonList(SERVICE_ID))
                            .withMetadata(ImmutableMap.of("buildNo", ARTIFACT_FILTER))
                            .build();
    ExecutionArgs executionArgs = new ExecutionArgs();
    executionArgs.setArtifacts(singletonList(artifact));

    when(wingsPersistence.get(Trigger.class, APP_ID, TRIGGER_ID)).thenReturn(scheduledConditionTrigger);
    when(wingsPersistence.query(any(), any(PageRequest.class)))
        .thenReturn(aPageResponse().withResponse(asList(webhookConditionTrigger)).build());
    when(workflowExecutionService.triggerPipelineExecution(anyString(), anyString(), any(ExecutionArgs.class)))
        .thenReturn(aWorkflowExecution().withAppId(APP_ID).withStatus(SUCCESS).build());

    when(wingsPersistence.saveAndGet(any(), any(Trigger.class))).thenReturn(webhookConditionTrigger);

    webhookConditionTrigger = triggerService.save(webhookConditionTrigger);

    triggerService.triggerExecutionByWebHook(
        APP_ID, webhookConditionTrigger.getWebHookToken(), artifact, new HashMap<>());
    verify(workflowExecutionService).triggerPipelineExecution(anyString(), anyString(), any(ExecutionArgs.class));
    verify(artifactStreamService, times(0)).get(APP_ID, ARTIFACT_STREAM_ID);
    verify(artifactService, times(0))
        .fetchLastCollectedArtifactForArtifactStream(APP_ID, ARTIFACT_STREAM_ID, jenkinsArtifactStream.getSourceName());
    verify(workflowExecutionService, times(0)).listExecutions(any(PageRequest.class), anyBoolean());
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

    webhookConditionTrigger.setArtifactSelections(asList(ArtifactSelection.builder()
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
        ArtifactSelection.builder().type(LAST_DEPLOYED).serviceId(SERVICE_ID).workflowId(PIPELINE_ID).build()));

    when(wingsPersistence.saveAndGet(any(), any(Trigger.class))).thenReturn(webhookConditionTrigger);

    webhookConditionTrigger = triggerService.save(webhookConditionTrigger);

    when(wingsPersistence.get(Trigger.class, APP_ID, TRIGGER_ID)).thenReturn(webhookConditionTrigger);
    when(wingsPersistence.query(any(), any(PageRequest.class)))
        .thenReturn(aPageResponse().withResponse(asList(webhookConditionTrigger)).build());
    when(artifactStreamService.get(APP_ID, ARTIFACT_STREAM_ID)).thenReturn(jenkinsArtifactStream);
    when(artifactService.fetchLastCollectedArtifactForArtifactStream(
             APP_ID, ARTIFACT_STREAM_ID, jenkinsArtifactStream.getSourceName()))
        .thenReturn(artifact);
    when(workflowExecutionService.triggerPipelineExecution(anyString(), anyString(), any(ExecutionArgs.class)))
        .thenReturn(aWorkflowExecution().withAppId(APP_ID).withStatus(SUCCESS).build());
    when(workflowExecutionService.listExecutions(any(PageRequest.class), anyBoolean()))
        .thenReturn(aPageResponse()
                        .withResponse(singletonList(
                            aWorkflowExecution().withAppId(APP_ID).withExecutionArgs(executionArgs).build()))
                        .build());
    when(artifactStreamService.get(APP_ID, ARTIFACT_STREAM_ID)).thenReturn(jenkinsArtifactStream);
    when(pipelineService.readPipeline(APP_ID, PIPELINE_ID, true)).thenReturn(pipeline);
    when(artifactService.getArtifactByBuildNumber(any(), any(), anyString())).thenReturn(artifact);

    triggerService.triggerExecutionByWebHook(
        APP_ID, webhookConditionTrigger.getWebHookToken(), ImmutableMap.of("Catalog", "123"), new HashMap<>());

    verify(workflowExecutionService).triggerPipelineExecution(anyString(), anyString(), any(ExecutionArgs.class));
    verify(artifactStreamService, times(3)).get(APP_ID, ARTIFACT_STREAM_ID);
    verify(artifactService)
        .fetchLastCollectedArtifactForArtifactStream(APP_ID, ARTIFACT_STREAM_ID, jenkinsArtifactStream.getSourceName());

    verify(artifactService).getArtifactByBuildNumber(any(), any(), anyString());
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

    webhookConditionTrigger.setArtifactSelections(asList(ArtifactSelection.builder()
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
        ArtifactSelection.builder().type(LAST_DEPLOYED).serviceId(SERVICE_ID).workflowId(PIPELINE_ID).build()));

    when(wingsPersistence.saveAndGet(any(), any(Trigger.class))).thenReturn(webhookConditionTrigger);

    webhookConditionTrigger = triggerService.save(webhookConditionTrigger);

    when(wingsPersistence.get(Trigger.class, APP_ID, TRIGGER_ID)).thenReturn(webhookConditionTrigger);
    when(wingsPersistence.query(any(), any(PageRequest.class)))
        .thenReturn(aPageResponse().withResponse(asList(webhookConditionTrigger)).build());
    when(artifactStreamService.get(APP_ID, ARTIFACT_STREAM_ID)).thenReturn(jenkinsArtifactStream);
    when(artifactService.fetchLastCollectedArtifactForArtifactStream(
             APP_ID, ARTIFACT_STREAM_ID, jenkinsArtifactStream.getSourceName()))
        .thenReturn(artifact);
    when(workflowExecutionService.triggerPipelineExecution(anyString(), anyString(), any(ExecutionArgs.class)))
        .thenReturn(aWorkflowExecution().withAppId(APP_ID).withStatus(SUCCESS).build());
    when(workflowExecutionService.listExecutions(any(PageRequest.class), anyBoolean()))
        .thenReturn(aPageResponse()
                        .withResponse(singletonList(
                            aWorkflowExecution().withAppId(APP_ID).withExecutionArgs(executionArgs).build()))
                        .build());
    when(artifactStreamService.get(APP_ID, ARTIFACT_STREAM_ID)).thenReturn(jenkinsArtifactStream);
    when(pipelineService.readPipeline(APP_ID, PIPELINE_ID, true)).thenReturn(pipeline);
    when(artifactService.getArtifactByBuildNumber(APP_ID, ARTIFACT_STREAM_ID, "123")).thenReturn(artifact);

    triggerService.triggerExecutionByWebHook(
        APP_ID, webhookConditionTrigger.getWebHookToken(), ImmutableMap.of("Catalog", "123"), new HashMap<>());

    verify(workflowExecutionService).triggerPipelineExecution(anyString(), anyString(), any(ExecutionArgs.class));
    verify(artifactStreamService, times(3)).get(APP_ID, ARTIFACT_STREAM_ID);
    verify(artifactService)
        .fetchLastCollectedArtifactForArtifactStream(APP_ID, ARTIFACT_STREAM_ID, jenkinsArtifactStream.getSourceName());

    verify(workflowExecutionService).obtainLastGoodDeployedArtifacts(APP_ID, PIPELINE_ID);
    verify(artifactService).getArtifactByBuildNumber(APP_ID, ARTIFACT_STREAM_ID, "123");
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

    when(wingsPersistence.saveAndGet(any(), any(Trigger.class))).thenReturn(workflowWebhookConditionTrigger);

    when(wingsPersistence.get(Workflow.class, APP_ID, WORKFLOW_ID)).thenReturn(workflow);

    workflowWebhookConditionTrigger = triggerService.save(workflowWebhookConditionTrigger);

    when(wingsPersistence.get(Trigger.class, APP_ID, TRIGGER_ID)).thenReturn(workflowWebhookConditionTrigger);
    when(wingsPersistence.query(any(), any(PageRequest.class)))
        .thenReturn(aPageResponse().withResponse(asList(workflowWebhookConditionTrigger)).build());
    when(artifactStreamService.get(APP_ID, ARTIFACT_STREAM_ID)).thenReturn(jenkinsArtifactStream);
    when(artifactService.fetchLastCollectedArtifactForArtifactStream(
             APP_ID, ARTIFACT_STREAM_ID, jenkinsArtifactStream.getSourceName()))
        .thenReturn(artifact);
    when(workflowExecutionService.triggerEnvExecution(anyString(), anyString(), any(ExecutionArgs.class)))
        .thenReturn(aWorkflowExecution().withAppId(APP_ID).withStatus(SUCCESS).build());
    when(workflowExecutionService.listExecutions(any(PageRequest.class), anyBoolean()))
        .thenReturn(aPageResponse()
                        .withResponse(singletonList(
                            aWorkflowExecution().withAppId(APP_ID).withExecutionArgs(executionArgs).build()))
                        .build());
    when(artifactStreamService.get(APP_ID, ARTIFACT_STREAM_ID)).thenReturn(jenkinsArtifactStream);
    when(pipelineService.readPipeline(APP_ID, PIPELINE_ID, true)).thenReturn(pipeline);
    when(artifactService.getArtifactByBuildNumber(APP_ID, ARTIFACT_STREAM_ID, "123")).thenReturn(artifact);

    triggerService.triggerExecutionByWebHook(
        APP_ID, workflowWebhookConditionTrigger.getWebHookToken(), ImmutableMap.of("Catalog", "123"), new HashMap<>());

    verify(workflowExecutionService).triggerEnvExecution(anyString(), anyString(), any(ExecutionArgs.class));
    verify(artifactStreamService, times(3)).get(APP_ID, ARTIFACT_STREAM_ID);
    verify(artifactService)
        .fetchLastCollectedArtifactForArtifactStream(APP_ID, ARTIFACT_STREAM_ID, jenkinsArtifactStream.getSourceName());

    verify(workflowExecutionService).obtainLastGoodDeployedArtifacts(APP_ID, WORKFLOW_ID);
    verify(artifactService).getArtifactByBuildNumber(APP_ID, ARTIFACT_STREAM_ID, "123");
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
    when(wingsPersistence.query(any(), any(PageRequest.class)))
        .thenReturn(aPageResponse().withResponse(asList(pipelineCondTrigger)).build());

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
    when(wingsPersistence.query(any(), any(PageRequest.class)))
        .thenReturn(aPageResponse().withResponse(asList(artifactConditionTrigger)).build());

    List<Trigger> triggersHasArtifactStreamAction =
        triggerService.getTriggersHasArtifactStreamAction(APP_ID, ARTIFACT_STREAM_ID);

    assertThat(triggersHasArtifactStreamAction).isNotEmpty();
  }

  @Test(expected = WingsException.class)
  public void shouldValidateArtifactStreamSelections() {
    when(wingsPersistence.saveAndGet(any(), any(Trigger.class))).thenReturn(webhookConditionTrigger);
    webhookConditionTrigger.setArtifactSelections(
        asList(ArtifactSelection.builder().type(LAST_COLLECTED).artifactFilter(ARTIFACT_FILTER).build(),
            ArtifactSelection.builder().type(LAST_DEPLOYED).build()));

    when(wingsPersistence.get(Trigger.class, defaultTrigger.getAppId(), defaultTrigger.getUuid()))
        .thenReturn(webhookConditionTrigger);

    triggerService.save(webhookConditionTrigger);
  }

  @Test(expected = WingsException.class)
  public void shouldValidateUpdateArtifactStreamSelections() {
    when(wingsPersistence.saveAndGet(any(), any(Trigger.class))).thenReturn(webhookConditionTrigger);
    webhookConditionTrigger.setArtifactSelections(
        asList(ArtifactSelection.builder().type(LAST_COLLECTED).artifactFilter(ARTIFACT_FILTER).build(),
            ArtifactSelection.builder().type(LAST_DEPLOYED).workflowId(PIPELINE_ID).build()));

    when(wingsPersistence.get(Trigger.class, defaultTrigger.getAppId(), defaultTrigger.getUuid()))
        .thenReturn(webhookConditionTrigger);

    triggerService.update(webhookConditionTrigger);
  }

  @Test
  public void shouldListPipelineWebhookParameters() {
    setPipelineStages();
    WebhookParameters webhookParameters = triggerService.listWebhookParameters(APP_ID, PIPELINE_ID, PIPELINE);
    assertThat(webhookParameters.getParams()).isNotNull().contains("MyVar");
    assertThat(webhookParameters.getExpressions()).isNotNull().contains(WebhookParameters.PULL_REQUEST_ID);
    verify(pipelineService).readPipeline(anyString(), anyString(), anyBoolean());
    verify(workflowService).readWorkflow(APP_ID, WORKFLOW_ID);
  }

  @Test
  public void shouldListWorkflowWebhookParameters() {
    WebhookParameters webhookParameters = triggerService.listWebhookParameters(APP_ID, WORKFLOW_ID, ORCHESTRATION);
    assertThat(webhookParameters.getParams()).isNotNull().contains("MyVar");
    assertThat(webhookParameters.getExpressions()).isNotNull().contains(WebhookParameters.PULL_REQUEST_ID);
    verify(workflowService).readWorkflow(APP_ID, WORKFLOW_ID);
  }

  @Test
  public void shouldSaveNewInstanceTrigger() {
    when(wingsPersistence.saveAndGet(any(), any(Trigger.class))).thenReturn(newInstanceTrigger);
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
    verify(wingsPersistence).saveAndGet(any(), any(Trigger.class));
    verify(infrastructureMappingService).get(APP_ID, INFRA_MAPPING_ID);
    verify(workflowService, times(2)).readWorkflow(APP_ID, WORKFLOW_ID);
  }

  @Test
  public void shouldTriggerExecutionByServiceInfra() {
    when(infrastructureMappingService.get(APP_ID, INFRA_MAPPING_ID))
        .thenReturn(anAwsInfrastructureMapping()
                        .withUuid(INFRA_MAPPING_ID)
                        .withServiceId(SERVICE_ID)
                        .withDeploymentType(SSH.name())
                        .withComputeProviderType("AWS")
                        .build());
    when(wingsPersistence.query(any(), any(PageRequest.class)))
        .thenReturn(aPageResponse().withResponse(asList(newInstanceTrigger)).build());

    assertThat(triggerService.triggerExecutionByServiceInfra(APP_ID, INFRA_MAPPING_ID)).isTrue();
    verify(infrastructureMappingService).get(APP_ID, INFRA_MAPPING_ID);
    verify(workflowExecutionService).triggerEnvExecution(anyString(), anyString(), any(ExecutionArgs.class));
  }

  private Trigger buildArtifactTrigger() {
    return Trigger.builder()
        .workflowId(PIPELINE_ID)
        .uuid(TRIGGER_ID)
        .appId(APP_ID)
        .name(TRIGGER_NAME)
        .condition(ArtifactTriggerCondition.builder()
                       .artifactFilter(ARTIFACT_FILTER)
                       .artifactStreamId(ARTIFACT_STREAM_ID)
                       .build())
        .build();
  }

  private Trigger buildWorkflowArtifactTrigger() {
    return Trigger.builder()
        .workflowId(WORKFLOW_ID)
        .uuid(TRIGGER_ID)
        .workflowType(ORCHESTRATION)
        .appId(APP_ID)
        .name(TRIGGER_NAME)
        .condition(ArtifactTriggerCondition.builder()
                       .artifactFilter(ARTIFACT_FILTER)
                       .artifactStreamId(ARTIFACT_STREAM_ID)
                       .build())
        .build();
  }

  private Trigger buildPipelineCondTrigger() {
    return Trigger.builder()
        .workflowId(PIPELINE_ID)
        .uuid(TRIGGER_ID)
        .appId(APP_ID)
        .name(TRIGGER_NAME)
        .condition(PipelineTriggerCondition.builder().pipelineId(PIPELINE_ID).build())
        .build();
  }

  private Trigger buildScheduledCondTrigger() {
    return Trigger.builder()
        .workflowId(PIPELINE_ID)
        .uuid(TRIGGER_ID)
        .appId(APP_ID)
        .name(TRIGGER_NAME)
        .condition(ScheduledTriggerCondition.builder().cronExpression("* * * * ?").build())
        .build();
  }

  private Trigger buildWebhookCondTrigger() {
    return Trigger.builder()
        .workflowId(PIPELINE_ID)
        .uuid(TRIGGER_ID)
        .appId(APP_ID)
        .name(TRIGGER_NAME)
        .condition(WebHookTriggerCondition.builder().webHookToken(WebHookToken.builder().build()).build())
        .build();
  }

  private Trigger buildNewInstanceTrigger() {
    return Trigger.builder()
        .uuid(TRIGGER_ID)
        .appId(APP_ID)
        .workflowType(ORCHESTRATION)
        .workflowId(WORKFLOW_ID)
        .name("New Instance Trigger")
        .serviceInfraWorkflows(
            asList(ServiceInfraWorkflow.builder().infraMappingId(INFRA_MAPPING_ID).workflowId(WORKFLOW_ID).build()))
        .condition(NewInstanceTriggerCondition.builder().build())
        .build();
  }

  private Pipeline buildPipeline() {
    return Pipeline.builder()
        .appId(APP_ID)
        .uuid(PIPELINE_ID)
        .services(asList(Service.builder().uuid(SERVICE_ID).name("Catalog").build(),
            Service.builder().uuid(SERVICE_ID_CHANGED).name("Order").build()))
        .build();
  }

  private Trigger buildWorkflowWebhookTrigger() {
    return Trigger.builder()
        .workflowId(WORKFLOW_ID)
        .workflowType(ORCHESTRATION)
        .uuid(TRIGGER_ID)
        .appId(APP_ID)
        .name(TRIGGER_NAME)
        .condition(WebHookTriggerCondition.builder().webHookToken(WebHookToken.builder().build()).build())
        .build();
  }

  private Workflow buildWorkflow() {
    List<Variable> userVariables = new ArrayList<>();
    userVariables.add(aVariable().withName("MyVar").withValue("MyVal").build());
    return aWorkflow()
        .withEnvId(ENV_ID)
        .withName(WORKFLOW_NAME)
        .withAppId(APP_ID)
        .withServiceId(SERVICE_ID)
        .withInfraMappingId(INFRA_MAPPING_ID)
        .withOrchestrationWorkflow(
            aBasicOrchestrationWorkflow()
                .withUserVariables(userVariables)
                .withPreDeploymentSteps(aPhaseStep(PRE_DEPLOYMENT, Constants.PRE_DEPLOYMENT).build())
                .withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT, Constants.POST_DEPLOYMENT).build())
                .build())
        .withServices(asList(Service.builder().uuid(SERVICE_ID).name("Catalog").build(),
            Service.builder().uuid(SERVICE_ID_CHANGED).name("Order").build()))
        .build();
  }

  private JenkinsArtifactStream buildJenkinsArtifactStream() {
    return JenkinsArtifactStream.builder()
        .appId(APP_ID)
        .uuid(ARTIFACT_STREAM_ID)
        .sourceName(ARTIFACT_SOURCE_NAME)
        .settingId(SETTING_ID)
        .jobname("JOB")
        .serviceId(SERVICE_ID)
        .artifactPaths(asList("*WAR"))
        .build();
  }

  private void setPipelineStages() {
    Map<String, Object> properties = new HashMap<>();
    properties.put("envId", ENV_ID);
    properties.put("workflowId", WORKFLOW_ID);

    List<PipelineStage> pipelineStages = new ArrayList<>();
    PipelineStage pipelineStage =
        PipelineStage.builder()
            .pipelineStageElements(asList(PipelineStageElement.builder()
                                              .name("STAGE 1")
                                              .type(ENV_STATE.name())
                                              .properties(properties)
                                              .workflowVariables(ImmutableMap.of("Environment", ENV_ID, "Service",
                                                  SERVICE_ID, "ServiceInfraStructure", INFRA_MAPPING_ID))
                                              .build()))
            .build();
    pipelineStages.add(pipelineStage);

    pipeline.setPipelineStages(pipelineStages);
  }
}
