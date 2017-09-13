package software.wings.service;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.Workflow.WorkflowBuilder.aWorkflow;
import static software.wings.beans.artifact.ArtifactStreamAction.Builder.anArtifactStreamAction;
import static software.wings.beans.artifact.DockerArtifactStream.Builder.aDockerArtifactStream;
import static software.wings.beans.artifact.JenkinsArtifactStream.Builder.aJenkinsArtifactStream;
import static software.wings.dl.PageResponse.Builder.aPageResponse;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_STREAM_ID;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.ENV_NAME;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.SETTING_ID;
import static software.wings.utils.WingsTestConstants.WORKFLOW_ID;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mongodb.morphia.mapping.Mapper;
import org.mongodb.morphia.query.FieldEnd;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.quartz.JobDetail;
import org.quartz.Trigger;
import software.wings.WingsBaseTest;
import software.wings.beans.Environment;
import software.wings.beans.Service;
import software.wings.beans.WorkflowType;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.ArtifactStreamAction;
import software.wings.beans.artifact.DockerArtifactStream;
import software.wings.beans.artifact.JenkinsArtifactStream;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.scheduler.JobScheduler;
import software.wings.service.impl.ArtifactStreamServiceImpl;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.BuildSourceService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.WorkflowService;

import java.util.Arrays;
import javax.inject.Inject;

/**
 * Created by anubhaw on 11/4/16.
 */
public class ArtifactStreamServiceTest extends WingsBaseTest {
  @Mock private WingsPersistence wingsPersistence;
  @Mock private UpdateOperations<ArtifactStream> updateOperations;
  @Mock private JobScheduler jobScheduler;
  @Mock private WorkflowService workflowService;
  @Mock private EnvironmentService environmentService;
  @Mock private Query<ArtifactStream> query;
  @Mock private FieldEnd end;
  @Mock private BuildSourceService buildSourceService;
  @Mock private ArtifactService artifactService;

  @Inject @InjectMocks private ArtifactStreamService artifactStreamService;

  @Spy @InjectMocks private ArtifactStreamService spyArtifactStreamService = new ArtifactStreamServiceImpl();

  private JenkinsArtifactStream jenkinsArtifactStream = aJenkinsArtifactStream()
                                                            .withAppId(APP_ID)
                                                            .withUuid(ARTIFACT_STREAM_ID)
                                                            .withSourceName("SOURCE_NAME")
                                                            .withSettingId(SETTING_ID)
                                                            .withJobname("JOB")
                                                            .withServiceId(SERVICE_ID)
                                                            .withArtifactPaths(Arrays.asList("*WAR"))
                                                            .build();
  private DockerArtifactStream dockerArtifactStream = aDockerArtifactStream()
                                                          .withAppId(APP_ID)
                                                          .withUuid(ARTIFACT_STREAM_ID)
                                                          .withSourceName("SOURCE_NAME_DOCKER")
                                                          .withSettingId(SETTING_ID)
                                                          .withImageName("wingsplugins/todolist")
                                                          .build();

  @Before
  public void setUp() throws Exception {
    when(wingsPersistence.createQuery(ArtifactStream.class)).thenReturn(query);
    when(query.field(any())).thenReturn(end);
    when(end.equal(any())).thenReturn(query);
    when(end.notEqual(any())).thenReturn(query);
    when(end.equal(any())).thenReturn(query);
    when(wingsPersistence.createUpdateOperations(ArtifactStream.class)).thenReturn(updateOperations);
    when(updateOperations.add(any(), any())).thenReturn(updateOperations);
    when(updateOperations.removeAll(any(String.class), any(ArtifactStreamAction.class))).thenReturn(updateOperations);
  }

  @Test
  public void shouldList() {
    PageRequest pageRequest = PageRequest.Builder.aPageRequest().build();
    when(wingsPersistence.query(ArtifactStream.class, pageRequest))
        .thenReturn(aPageResponse().withResponse(asList(jenkinsArtifactStream)).build());

    PageResponse<ArtifactStream> artifactStreams = artifactStreamService.list(pageRequest);

    assertThat(artifactStreams.size()).isEqualTo(1);
  }

  @Test
  public void shouldGet() {
    when(wingsPersistence.get(ArtifactStream.class, APP_ID, ARTIFACT_STREAM_ID)).thenReturn(jenkinsArtifactStream);
    ArtifactStream artifactStream = artifactStreamService.get(APP_ID, ARTIFACT_STREAM_ID);
    assertThat(artifactStream.getUuid()).isEqualTo(ARTIFACT_STREAM_ID);
    verify(wingsPersistence).get(ArtifactStream.class, APP_ID, ARTIFACT_STREAM_ID);
  }

  @Test
  public void shouldCreate() {
    when(wingsPersistence.save(any(ArtifactStream.class))).thenReturn(ARTIFACT_STREAM_ID);
    when(wingsPersistence.get(ArtifactStream.class, APP_ID, ARTIFACT_STREAM_ID)).thenReturn(jenkinsArtifactStream);
    ArtifactStream artifactStream = artifactStreamService.create(jenkinsArtifactStream);
    assertThat(artifactStream.getUuid()).isEqualTo(ARTIFACT_STREAM_ID);
    verify(jobScheduler).scheduleJob(any(JobDetail.class), any(Trigger.class));
    verify(wingsPersistence).save(any(ArtifactStream.class));
    verify(wingsPersistence).get(ArtifactStream.class, APP_ID, ARTIFACT_STREAM_ID);
  }

  @Test
  public void shouldDockerArtifactStreamCreate() {
    when(wingsPersistence.save(any(ArtifactStream.class))).thenReturn(ARTIFACT_STREAM_ID);
    when(wingsPersistence.get(ArtifactStream.class, APP_ID, ARTIFACT_STREAM_ID)).thenReturn(jenkinsArtifactStream);
    when(buildSourceService.validateArtifactSource(dockerArtifactStream.getAppId(), dockerArtifactStream.getSettingId(),
             dockerArtifactStream.getArtifactStreamAttributes()))
        .thenReturn(true);
    ArtifactStream artifactStream = artifactStreamService.create(dockerArtifactStream);
    assertThat(artifactStream.getUuid()).isEqualTo(ARTIFACT_STREAM_ID);
    verify(wingsPersistence).save(any(ArtifactStream.class));
    verify(wingsPersistence).get(ArtifactStream.class, APP_ID, ARTIFACT_STREAM_ID);
  }

  @Test
  public void shouldDockerArtifactStreamUpdate() {
    when(wingsPersistence.get(ArtifactStream.class, APP_ID, ARTIFACT_STREAM_ID)).thenReturn(jenkinsArtifactStream);
    when(wingsPersistence.save(any(ArtifactStream.class))).thenReturn(ARTIFACT_STREAM_ID);
    when(buildSourceService.validateArtifactSource(dockerArtifactStream.getAppId(), dockerArtifactStream.getSettingId(),
             dockerArtifactStream.getArtifactStreamAttributes()))
        .thenReturn(true);
    ArtifactStream artifactStream = artifactStreamService.update(jenkinsArtifactStream);
    assertThat(artifactStream.getUuid()).isEqualTo(ARTIFACT_STREAM_ID);
    verify(wingsPersistence, times(2)).get(ArtifactStream.class, APP_ID, ARTIFACT_STREAM_ID);
    verify(wingsPersistence).save(any(ArtifactStream.class));
  }

  @Test
  public void shouldUpdate() {
    when(wingsPersistence.get(ArtifactStream.class, APP_ID, ARTIFACT_STREAM_ID)).thenReturn(jenkinsArtifactStream);
    when(wingsPersistence.save(any(ArtifactStream.class))).thenReturn(ARTIFACT_STREAM_ID);
    ArtifactStream artifactStream = artifactStreamService.update(jenkinsArtifactStream);
    assertThat(artifactStream.getUuid()).isEqualTo(ARTIFACT_STREAM_ID);
    verify(wingsPersistence, times(2)).get(ArtifactStream.class, APP_ID, ARTIFACT_STREAM_ID);
    verify(wingsPersistence).save(any(ArtifactStream.class));
  }

  @Test
  public void shouldDelete() {
    jenkinsArtifactStream.setStreamActions(
        asList(ArtifactStreamAction.Builder.anArtifactStreamAction().withWorkflowId(WORKFLOW_ID).build()));
    when(wingsPersistence.get(ArtifactStream.class, APP_ID, ARTIFACT_STREAM_ID)).thenReturn(jenkinsArtifactStream);
    when(wingsPersistence.delete(any(Query.class))).thenReturn(true);
    artifactStreamService.delete(APP_ID, ARTIFACT_STREAM_ID);
    verify(wingsPersistence).delete(any(Query.class));
    verify(wingsPersistence).createQuery(any());
    verify(query).field("appId");
    verify(end).equal(APP_ID);
    verify(query).field(Mapper.ID_KEY);
    verify(end).equal(ARTIFACT_STREAM_ID);
    verify(jobScheduler).deleteJob(ARTIFACT_STREAM_ID, "ARTIFACT_STREAM_CRON_GROUP");
    verify(jobScheduler).deleteJob(WORKFLOW_ID, ARTIFACT_STREAM_ID);
  }

  @Test
  public void shouldAddStreamAction() {
    ArtifactStreamAction artifactStreamAction = anArtifactStreamAction()
                                                    .withCustomAction(true)
                                                    .withCronExpression("* * * * ?")
                                                    .withWorkflowType(WorkflowType.ORCHESTRATION)
                                                    .withWorkflowId(WORKFLOW_ID)
                                                    .withEnvId(ENV_ID)
                                                    .build();
    when(workflowService.readWorkflow(APP_ID, WORKFLOW_ID, null))
        .thenReturn(aWorkflow()
                        .withServices(asList(Service.Builder.aService().withUuid(SERVICE_ID).build()))
                        .withName("NAME")
                        .withEnvId(ENV_ID)
                        .build());
    when(environmentService.get(APP_ID, ENV_ID, false))
        .thenReturn(Environment.Builder.anEnvironment().withUuid(ENV_ID).withName(ENV_NAME).build());
    when(wingsPersistence.get(ArtifactStream.class, APP_ID, ARTIFACT_STREAM_ID)).thenReturn(jenkinsArtifactStream);
    artifactStreamService.addStreamAction(APP_ID, ARTIFACT_STREAM_ID, artifactStreamAction);
    verify(wingsPersistence).createQuery(any());
    verify(query).field("appId");
    verify(end).equal(APP_ID);
    verify(query).field(Mapper.ID_KEY);
    verify(end).equal(ARTIFACT_STREAM_ID);
    verify(query).field("streamActions.uuid");
    verify(end).notEqual(WORKFLOW_ID);
    verify(updateOperations).add("streamActions", artifactStreamAction);
    verify(wingsPersistence).update(query, updateOperations);
    verify(wingsPersistence, times(2)).get(ArtifactStream.class, APP_ID, ARTIFACT_STREAM_ID);
  }

  @Test
  public void shouldDeleteStreamAction() {
    ArtifactStreamAction artifactStreamAction = anArtifactStreamAction()
                                                    .withUuid("ACTION_ID")
                                                    .withWorkflowType(WorkflowType.ORCHESTRATION)
                                                    .withWorkflowId(WORKFLOW_ID)
                                                    .build();
    jenkinsArtifactStream.setStreamActions(asList(artifactStreamAction));
    when(query.get()).thenReturn(jenkinsArtifactStream);
    artifactStreamService.deleteStreamAction(APP_ID, ARTIFACT_STREAM_ID, "ACTION_ID");
    verify(wingsPersistence).createQuery(any());
    verify(query).field("appId");
    verify(end).equal(APP_ID);
    verify(query).field(Mapper.ID_KEY);
    verify(end).equal(ARTIFACT_STREAM_ID);
    verify(query).field("streamActions.uuid");
    verify(end).equal("ACTION_ID");
    verify(updateOperations).removeAll("streamActions", artifactStreamAction);
    verify(wingsPersistence).update(query, updateOperations);
    verify(wingsPersistence).get(ArtifactStream.class, APP_ID, ARTIFACT_STREAM_ID);
  }

  @Test
  public void shouldUpdateStreamAction() {
    ArtifactStreamAction artifactStreamAction = anArtifactStreamAction()
                                                    .withUuid("ACTION_ID")
                                                    .withWorkflowType(WorkflowType.ORCHESTRATION)
                                                    .withWorkflowId(WORKFLOW_ID)
                                                    .build();
    jenkinsArtifactStream.setStreamActions(asList(artifactStreamAction));

    doReturn(jenkinsArtifactStream).when(spyArtifactStreamService).get(APP_ID, ARTIFACT_STREAM_ID);
    doReturn(jenkinsArtifactStream)
        .when(spyArtifactStreamService)
        .deleteStreamAction(APP_ID, ARTIFACT_STREAM_ID, "ACTION_ID");
    doReturn(jenkinsArtifactStream)
        .when(spyArtifactStreamService)
        .addStreamAction(APP_ID, ARTIFACT_STREAM_ID, artifactStreamAction);

    spyArtifactStreamService.updateStreamAction(APP_ID, ARTIFACT_STREAM_ID, artifactStreamAction);

    verify(spyArtifactStreamService).deleteStreamAction(APP_ID, ARTIFACT_STREAM_ID, "ACTION_ID");
    verify(spyArtifactStreamService).addStreamAction(APP_ID, ARTIFACT_STREAM_ID, artifactStreamAction);
  }

  @Test
  public void shouldTriggerScheduledStreamAction() {
    ArtifactStreamAction artifactStreamAction = anArtifactStreamAction()
                                                    .withWorkflowType(WorkflowType.ORCHESTRATION)
                                                    .withWorkflowId(WORKFLOW_ID)
                                                    .withUuid("ACTION_ID")
                                                    .withCustomAction(true)
                                                    .withCronExpression("0 * * * * ?")
                                                    .build();
    jenkinsArtifactStream.setStreamActions(asList(artifactStreamAction));

    when(wingsPersistence.get(ArtifactStream.class, APP_ID, ARTIFACT_STREAM_ID)).thenReturn(jenkinsArtifactStream);
    // when(artifactService.fetchLatestArtifactForArtifactStream(APP_ID,
    // ARTIFACT_STREAM_ID)).thenReturn(Artifact.Builder.anArtifact().withAppId(APP_ID))
    artifactStreamService.triggerScheduledStreamAction(APP_ID, ARTIFACT_STREAM_ID, "ACTION_ID");
  }
}
