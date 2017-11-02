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
import static software.wings.beans.Pipeline.Builder.aPipeline;
import static software.wings.beans.Service.Builder.aService;
import static software.wings.beans.WorkflowExecution.WorkflowExecutionBuilder.aWorkflowExecution;
import static software.wings.beans.artifact.ArtifactFile.Builder.anArtifactFile;
import static software.wings.beans.artifact.JenkinsArtifactStream.Builder.aJenkinsArtifactStream;
import static software.wings.beans.trigger.ArtifactSelection.Type.ARTIFACT_SOURCE;
import static software.wings.beans.trigger.ArtifactSelection.Type.LAST_COLLECTED;
import static software.wings.beans.trigger.ArtifactSelection.Type.LAST_DEPLOYED;
import static software.wings.beans.trigger.ArtifactSelection.Type.PIPELINE_SOURCE;
import static software.wings.beans.trigger.Trigger.Builder.aDeploymentTrigger;
import static software.wings.dl.PageResponse.Builder.aPageResponse;
import static software.wings.service.impl.TriggerServiceImpl.SCHEDULED_TRIGGER_CRON_GROUP;
import static software.wings.sm.ExecutionStatus.SUCCESS;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_FILTER;
import static software.wings.utils.WingsTestConstants.ARTIFACT_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_SOURCE_NAME;
import static software.wings.utils.WingsTestConstants.ARTIFACT_STREAM_ID;
import static software.wings.utils.WingsTestConstants.FILE_ID;
import static software.wings.utils.WingsTestConstants.FILE_NAME;
import static software.wings.utils.WingsTestConstants.PIPELINE_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.SETTING_ID;
import static software.wings.utils.WingsTestConstants.TRIGGER_ID;
import static software.wings.utils.WingsTestConstants.TRIGGER_NAME;

import com.google.common.collect.ImmutableMap;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mongodb.morphia.query.FieldEnd;
import org.mongodb.morphia.query.Query;
import org.quartz.JobDetail;
import org.quartz.TriggerKey;
import software.wings.WingsBaseTest;
import software.wings.beans.ExecutionArgs;
import software.wings.beans.Pipeline;
import software.wings.beans.WebHookToken;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.JenkinsArtifactStream;
import software.wings.beans.trigger.ArtifactSelection;
import software.wings.beans.trigger.ArtifactTriggerCondition;
import software.wings.beans.trigger.PipelineTriggerCondition;
import software.wings.beans.trigger.ScheduledTriggerCondition;
import software.wings.beans.trigger.Trigger;
import software.wings.beans.trigger.WebHookTriggerCondition;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.scheduler.JobScheduler;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.TriggerService;
import software.wings.service.intfc.WorkflowExecutionService;

import java.util.Arrays;
import java.util.HashMap;
import javax.inject.Inject;

/**
 * Created by sgurubelli on 10/26/17.
 */
public class TriggerServiceTest extends WingsBaseTest {
  @Mock private WingsPersistence wingsPersistence;
  @Mock private Query query;
  @Mock private FieldEnd end;
  @Mock private JobScheduler jobScheduler;
  @Mock private PipelineService pipelineService;
  @Mock private WorkflowExecutionService workflowExecutionService;
  @Mock private ArtifactStreamService artifactStreamService;
  @Mock private ArtifactService artifactService;

  @Inject @InjectMocks private TriggerService triggerService;

  private Trigger trigger = aDeploymentTrigger().withUuid(TRIGGER_ID).withAppId(APP_ID).withName(TRIGGER_NAME).build();
  private Trigger artifactConditionTrigger = aDeploymentTrigger()
                                                 .withPipelineId(PIPELINE_ID)
                                                 .withUuid(TRIGGER_ID)
                                                 .withAppId(APP_ID)
                                                 .withName(TRIGGER_NAME)
                                                 .withCondition(ArtifactTriggerCondition.builder()
                                                                    .artifactFilter(ARTIFACT_FILTER)
                                                                    .artifactStreamId(ARTIFACT_STREAM_ID)
                                                                    .build())
                                                 .build();
  private Trigger pipelineConditionTrigger =
      aDeploymentTrigger()
          .withPipelineId(PIPELINE_ID)
          .withUuid(TRIGGER_ID)
          .withAppId(APP_ID)
          .withName(TRIGGER_NAME)
          .withCondition(PipelineTriggerCondition.builder().pipelineId(PIPELINE_ID).build())
          .build();
  private Trigger scheduledConditionTrigger =
      aDeploymentTrigger()
          .withPipelineId(PIPELINE_ID)
          .withUuid(TRIGGER_ID)
          .withAppId(APP_ID)
          .withName(TRIGGER_NAME)
          .withCondition(ScheduledTriggerCondition.builder().cronExpression("* * * * ?").build())
          .build();
  private Trigger webhookConditionTrigger =
      aDeploymentTrigger()
          .withPipelineId(PIPELINE_ID)
          .withUuid(TRIGGER_ID)
          .withAppId(APP_ID)
          .withName(TRIGGER_NAME)
          .withCondition(
              WebHookTriggerCondition.builder().webHookToken("XYZToken").requestBody("Sample request body").build())
          .build();

  private Pipeline pipeline = aPipeline()
                                  .withAppId(APP_ID)
                                  .withUuid(PIPELINE_ID)
                                  .withServices(asList(aService().withUuid(SERVICE_ID).withName("Catalog").build(),
                                      aService().withUuid(SERVICE_ID).withName("Order").build()))
                                  .build();

  private JenkinsArtifactStream jenkinsArtifactStream = aJenkinsArtifactStream()
                                                            .withAppId(APP_ID)
                                                            .withUuid(ARTIFACT_STREAM_ID)
                                                            .withSourceName(ARTIFACT_SOURCE_NAME)
                                                            .withSettingId(SETTING_ID)
                                                            .withJobname("JOB")
                                                            .withServiceId(SERVICE_ID)
                                                            .withArtifactPaths(Arrays.asList("*WAR"))
                                                            .build();

  @Before
  public void setUp() {
    when(wingsPersistence.createQuery(Trigger.class)).thenReturn(query);
    when(query.field(any())).thenReturn(end);
    when(end.equal(any())).thenReturn(query);
    when(end.lessThan(any())).thenReturn(query);
    when(end.in(any())).thenReturn(query);
  }

  @Test
  public void shouldListTriggers() {
    PageRequest<Trigger> pageRequest = new PageRequest<>();
    when(wingsPersistence.query(Trigger.class, pageRequest))
        .thenReturn(aPageResponse().withResponse(asList(trigger)).build());
    PageResponse<Trigger> triggers = triggerService.list(pageRequest);
    assertThat(triggers.size()).isEqualTo(1);
  }

  @Test
  public void shouldGet() {
    when(wingsPersistence.get(Trigger.class, APP_ID, TRIGGER_ID)).thenReturn(this.trigger);
    Trigger trigger = triggerService.get(APP_ID, TRIGGER_ID);
    assertThat(trigger.getName()).isEqualTo(TRIGGER_NAME);
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
  public void shouldUpdateArtifactConditionTrigger() {
    when(wingsPersistence.saveAndGet(any(), any(Trigger.class))).thenReturn(artifactConditionTrigger);

    artifactConditionTrigger.setArtifactSelections(
        asList(ArtifactSelection.builder().serviceId(SERVICE_ID).type(ARTIFACT_SOURCE).build(),
            ArtifactSelection.builder()
                .type(LAST_COLLECTED)
                .artifactStreamId(ARTIFACT_STREAM_ID)
                .artifactFilter(ARTIFACT_FILTER)
                .build()));

    when(wingsPersistence.get(Trigger.class, trigger.getAppId(), trigger.getUuid()))
        .thenReturn(artifactConditionTrigger);

    Trigger updatedTrigger = triggerService.update(artifactConditionTrigger);

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
        .contains(ARTIFACT_SOURCE);
    verify(wingsPersistence).saveAndGet(any(), any(Trigger.class));
  }

  @Test
  public void shouldSavePipelineConditionTrigger() {
    when(wingsPersistence.saveAndGet(any(), any(Trigger.class))).thenReturn(pipelineConditionTrigger);
    Trigger trigger = triggerService.save(pipelineConditionTrigger);
    assertThat(trigger.getUuid()).isEqualTo(TRIGGER_ID);
    assertThat(trigger.getCondition()).isInstanceOf(PipelineTriggerCondition.class);
    assertThat(((PipelineTriggerCondition) trigger.getCondition()).getPipelineId()).isNotNull().isEqualTo(PIPELINE_ID);
    verify(wingsPersistence).saveAndGet(any(), any(Trigger.class));
  }

  @Test
  public void shouldUpdatePipelineConditionTrigger() {
    when(wingsPersistence.saveAndGet(any(), any(Trigger.class))).thenReturn(pipelineConditionTrigger);
    pipelineConditionTrigger.setArtifactSelections(asList(ArtifactSelection.builder().type(PIPELINE_SOURCE).build(),
        ArtifactSelection.builder().type(LAST_DEPLOYED).pipelineId(PIPELINE_ID).build()));
    when(wingsPersistence.get(Trigger.class, trigger.getAppId(), trigger.getUuid()))
        .thenReturn(pipelineConditionTrigger);

    Trigger updatedTrigger = triggerService.update(pipelineConditionTrigger);

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
                                                               .artifactStreamId(ARTIFACT_STREAM_ID)
                                                               .artifactFilter(ARTIFACT_FILTER)
                                                               .build(),
        ArtifactSelection.builder().type(LAST_DEPLOYED).pipelineId(PIPELINE_ID).build()));

    when(wingsPersistence.get(Trigger.class, trigger.getAppId(), trigger.getUuid()))
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
    Trigger trigger = triggerService.save(webhookConditionTrigger);
    assertThat(trigger.getUuid()).isEqualTo(TRIGGER_ID);
    assertThat(trigger.getCondition()).isInstanceOf(WebHookTriggerCondition.class);
    assertThat(((WebHookTriggerCondition) trigger.getCondition()).getWebHookToken()).isNotNull();
    assertThat(((WebHookTriggerCondition) trigger.getCondition()).getRequestBody()).isNotNull();
    verify(wingsPersistence).saveAndGet(any(), any(Trigger.class));
  }

  @Test
  public void shouldUpdateWebhookConditionTrigger() {
    when(wingsPersistence.saveAndGet(any(), any(Trigger.class))).thenReturn(webhookConditionTrigger);
    webhookConditionTrigger.setArtifactSelections(asList(ArtifactSelection.builder()
                                                             .type(LAST_COLLECTED)
                                                             .artifactStreamId(ARTIFACT_STREAM_ID)
                                                             .artifactFilter(ARTIFACT_FILTER)
                                                             .build(),
        ArtifactSelection.builder().type(LAST_DEPLOYED).pipelineId(PIPELINE_ID).build()));

    when(wingsPersistence.get(Trigger.class, trigger.getAppId(), trigger.getUuid()))
        .thenReturn(webhookConditionTrigger);

    Trigger updatedTrigger = triggerService.update(webhookConditionTrigger);
    assertThat(updatedTrigger.getUuid()).isEqualTo(TRIGGER_ID);
    assertThat(updatedTrigger.getCondition()).isInstanceOf(WebHookTriggerCondition.class);
    assertThat(((WebHookTriggerCondition) updatedTrigger.getCondition()).getRequestBody()).isNotNull();
    assertThat(((WebHookTriggerCondition) updatedTrigger.getCondition()).getWebHookToken()).isNotNull();
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
                                                             .artifactFilter(ARTIFACT_FILTER)
                                                             .build(),
        ArtifactSelection.builder().type(LAST_DEPLOYED).pipelineId(PIPELINE_ID).build()));

    when(wingsPersistence.saveAndGet(any(), any(Trigger.class))).thenReturn(webhookConditionTrigger);
    Trigger updatedTrigger = triggerService.update(webhookConditionTrigger);

    assertThat(updatedTrigger.getCondition()).isInstanceOf(WebHookTriggerCondition.class);
    assertThat(((WebHookTriggerCondition) updatedTrigger.getCondition()).getRequestBody()).isNotNull();
    assertThat(((WebHookTriggerCondition) updatedTrigger.getCondition()).getWebHookToken()).isNotNull();
    assertThat(updatedTrigger.getArtifactSelections())
        .isNotNull()
        .extracting(artifactSelection -> artifactSelection.getType())
        .contains(LAST_COLLECTED, LAST_DEPLOYED);
    verify(jobScheduler).deleteJob(TRIGGER_ID, "SCHEDULED_TRIGGER_CRON_GROUP");
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
    verify(jobScheduler).deleteJob(TRIGGER_ID, "SCHEDULED_TRIGGER_CRON_GROUP");
  }

  @Test
  public void shouldDeleteArtifactTrigger() {
    when(wingsPersistence.get(Trigger.class, APP_ID, TRIGGER_ID)).thenReturn(artifactConditionTrigger);
    when(wingsPersistence.delete(Trigger.class, TRIGGER_ID)).thenReturn(true);
    triggerService.delete(APP_ID, TRIGGER_ID);
    verify(wingsPersistence).delete(Trigger.class, TRIGGER_ID);
    verify(jobScheduler, times(0)).deleteJob(TRIGGER_ID, "SCHEDULED_TRIGGER_CRON_GROUP");
  }

  @Test
  public void shouldDeleteTriggersForPipeline() {
    when(wingsPersistence.saveAndGet(any(), any(Trigger.class))).thenReturn(pipelineConditionTrigger);

    Trigger trigger = triggerService.save(pipelineConditionTrigger);
    assertThat(trigger).isNotNull();
    assertThat(trigger.getUuid()).isEqualTo(TRIGGER_ID);
    assertThat(trigger.getAppId()).isEqualTo(APP_ID);

    when(query.asList()).thenReturn(singletonList(pipelineConditionTrigger));
    when(wingsPersistence.get(Trigger.class, APP_ID, TRIGGER_ID)).thenReturn(pipelineConditionTrigger);
    when(wingsPersistence.delete(Trigger.class, TRIGGER_ID)).thenReturn(true);
    triggerService.deleteTriggersForPipeline(APP_ID, PIPELINE_ID);
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
    triggerService.deleteByApp(APP_ID);
    verify(wingsPersistence).delete(Trigger.class, TRIGGER_ID);
    verify(jobScheduler).deleteJob(TRIGGER_ID, "SCHEDULED_TRIGGER_CRON_GROUP");
  }

  @Test
  public void shouldGenerateWebHookToken() {
    when(pipelineService.readPipeline(APP_ID, PIPELINE_ID, true)).thenReturn(pipeline);
    WebHookToken webHookToken = triggerService.generateWebHookToken(APP_ID, PIPELINE_ID);
    assertThat(webHookToken).isNotNull();
    assertThat(webHookToken.getPayload()).contains("application");
  }

  @Test
  public void shouldTriggerExecutionPostArtifactCollectionAsync() {
    Artifact artifact = Artifact.Builder.anArtifact()
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
  public void shouldTriggerExecutionPostArtifactCollectionAsyncWithFiles() {
    Artifact artifact = Artifact.Builder.anArtifact()
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
    verify(workflowExecutionService).triggerPipelineExecution(anyString(), anyString(), any(ExecutionArgs.class));
  }

  @Test
  public void shouldTriggerExecutionPostArtifactCollectionWithArtifactSelections() {
    Artifact artifact = Artifact.Builder.anArtifact()
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
            ArtifactSelection.builder().type(LAST_DEPLOYED).serviceId(SERVICE_ID).pipelineId(PIPELINE_ID).build()));

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
  }

  @Test
  public void shouldTriggerExecutionPostPipelineCompletion() {
    Artifact artifact = Artifact.Builder.anArtifact()
                            .withAppId(APP_ID)
                            .withUuid(ARTIFACT_ID)
                            .withArtifactStreamId(ARTIFACT_STREAM_ID)
                            .withServiceIds(singletonList(SERVICE_ID))
                            .withMetadata(ImmutableMap.of("buildNo", ARTIFACT_FILTER))
                            .build();
    ExecutionArgs executionArgs = new ExecutionArgs();
    executionArgs.setArtifacts(singletonList(artifact));

    when(wingsPersistence.query(any(), any(PageRequest.class)))
        .thenReturn(aPageResponse().withResponse(asList(pipelineConditionTrigger)).build());
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
    Artifact artifact = Artifact.Builder.anArtifact()
                            .withAppId(APP_ID)
                            .withUuid(ARTIFACT_ID)
                            .withArtifactStreamId(ARTIFACT_STREAM_ID)
                            .withServiceIds(singletonList(SERVICE_ID))
                            .withMetadata(ImmutableMap.of("buildNo", ARTIFACT_FILTER))
                            .build();
    pipelineConditionTrigger.setArtifactSelections(
        asList(ArtifactSelection.builder().serviceId(SERVICE_ID).type(PIPELINE_SOURCE).build(),
            ArtifactSelection.builder()
                .type(LAST_COLLECTED)
                .serviceId(SERVICE_ID)
                .artifactStreamId(ARTIFACT_STREAM_ID)
                .artifactFilter(ARTIFACT_FILTER)
                .build(),
            ArtifactSelection.builder().type(LAST_DEPLOYED).serviceId(SERVICE_ID).pipelineId(PIPELINE_ID).build()));

    when(wingsPersistence.query(any(), any(PageRequest.class)))
        .thenReturn(aPageResponse().withResponse(asList(pipelineConditionTrigger)).build());
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
  }

  @Test
  public void shouldTriggerScheduledExecution() {
    Artifact artifact = Artifact.Builder.anArtifact()
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

    triggerService.triggerScheduledExecutionAsync(APP_ID, TRIGGER_ID);
    verify(workflowExecutionService).triggerPipelineExecution(anyString(), anyString(), any(ExecutionArgs.class));
  }

  @Test
  public void shouldTriggerScheduledExecutionWithArtifactSelections() {
    Artifact artifact = Artifact.Builder.anArtifact()
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
        ArtifactSelection.builder().type(LAST_DEPLOYED).serviceId(SERVICE_ID).pipelineId(PIPELINE_ID).build()));

    when(wingsPersistence.get(Trigger.class, APP_ID, TRIGGER_ID)).thenReturn(scheduledConditionTrigger);
    when(wingsPersistence.query(any(), any(PageRequest.class)))
        .thenReturn(aPageResponse().withResponse(asList(scheduledConditionTrigger)).build());
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

    triggerService.triggerScheduledExecutionAsync(APP_ID, TRIGGER_ID);
    verify(workflowExecutionService).triggerPipelineExecution(anyString(), anyString(), any(ExecutionArgs.class));
  }

  @Test
  public void shouldTriggerExecutionByWebhook() {
    Artifact artifact = Artifact.Builder.anArtifact()
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
    when(workflowExecutionService.listExecutions(any(PageRequest.class), anyBoolean()))
        .thenReturn(aPageResponse()
                        .withResponse(singletonList(
                            aWorkflowExecution().withAppId(APP_ID).withExecutionArgs(executionArgs).build()))
                        .build());

    triggerService.triggerExecutionByWebhook(APP_ID, artifact, webhookConditionTrigger, new HashMap<>());
    verify(workflowExecutionService).triggerPipelineExecution(anyString(), anyString(), any(ExecutionArgs.class));
  }

  @Test
  public void shouldTriggerExecutionByWebhookWithArtifactSelections() {
    Artifact artifact = Artifact.Builder.anArtifact()
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

    triggerService.triggerScheduledExecutionAsync(APP_ID, TRIGGER_ID);
    verify(workflowExecutionService).triggerPipelineExecution(anyString(), anyString(), any(ExecutionArgs.class));
  }

  @Test
  public void shouldDeleteScheduledJobIfTriggerDeleted() {
    triggerService.triggerScheduledExecutionAsync(APP_ID, TRIGGER_ID);
    verify(jobScheduler).deleteJob(TRIGGER_ID, SCHEDULED_TRIGGER_CRON_GROUP);
  }
}
