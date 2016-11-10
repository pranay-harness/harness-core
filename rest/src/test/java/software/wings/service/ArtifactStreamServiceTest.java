package software.wings.service;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mongodb.morphia.mapping.Mapper;
import org.mongodb.morphia.query.FieldEnd;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.WingsBaseTest;
import software.wings.beans.WorkflowType;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.ArtifactStreamAction;
import software.wings.beans.artifact.JenkinsArtifactStream;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.scheduler.JobScheduler;
import software.wings.service.impl.ArtifactStreamServiceImpl;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.ServiceResourceService;

import javax.inject.Inject;
import java.util.concurrent.ExecutorService;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static software.wings.beans.artifact.ArtifactPathServiceEntry.Builder.anArtifactPathServiceEntry;
import static software.wings.beans.artifact.ArtifactStreamAction.Builder.anArtifactStreamAction;
import static software.wings.beans.artifact.JenkinsArtifactStream.Builder.aJenkinsArtifactStream;
import static software.wings.dl.PageResponse.Builder.aPageResponse;
import static software.wings.utils.WingsTestConstants.*;

/**
 * Created by anubhaw on 11/4/16.
 */
public class ArtifactStreamServiceTest extends WingsBaseTest {
  @Mock private WingsPersistence wingsPersistence;
  @Mock private ServiceResourceService serviceResourceService;
  @Mock private ExecutorService executorService;
  @Mock private Query<ArtifactStream> query;
  @Mock private UpdateOperations<ArtifactStream> updateOperations;
  @Mock private JobScheduler jobScheduler;
  @Mock private FieldEnd end;

  @Inject @InjectMocks private ArtifactStreamService artifactStreamService;

  @Spy @InjectMocks private ArtifactStreamService spyArtifactStreamService = new ArtifactStreamServiceImpl();

  private JenkinsArtifactStream jenkinsArtifactStream =
      aJenkinsArtifactStream()
          .withAppId(APP_ID)
          .withUuid(ARTIFACT_STREAM_ID)
          .withSourceName("SOURCE_NAME")
          .withJenkinsSettingId(SETTING_ID)
          .withJobname("JOB")
          .withArtifactPathServices(asList(
              anArtifactPathServiceEntry().withArtifactPathRegex("*WAR").withServiceIds(asList(SERVICE_ID)).build()))
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
    verify(serviceResourceService).get(APP_ID, SERVICE_ID);
  }

  @Test
  public void shouldGet() {
    when(wingsPersistence.get(ArtifactStream.class, APP_ID, ARTIFACT_STREAM_ID)).thenReturn(jenkinsArtifactStream);
    ArtifactStream artifactStream = artifactStreamService.get(APP_ID, ARTIFACT_STREAM_ID);
    assertThat(artifactStream.getUuid()).isEqualTo(ARTIFACT_STREAM_ID);
    verify(serviceResourceService).get(APP_ID, SERVICE_ID);
    verify(wingsPersistence).get(ArtifactStream.class, APP_ID, ARTIFACT_STREAM_ID);
  }

  @Test
  public void shouldCreate() {
    when(wingsPersistence.save(any(ArtifactStream.class))).thenReturn(ARTIFACT_STREAM_ID);
    when(wingsPersistence.get(ArtifactStream.class, APP_ID, ARTIFACT_STREAM_ID)).thenReturn(jenkinsArtifactStream);
    ArtifactStream artifactStream = artifactStreamService.create(jenkinsArtifactStream);
    assertThat(artifactStream.getUuid()).isEqualTo(ARTIFACT_STREAM_ID);
    verify(wingsPersistence).save(any(ArtifactStream.class));
    verify(wingsPersistence).get(ArtifactStream.class, APP_ID, ARTIFACT_STREAM_ID);
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
    artifactStreamService.delete(APP_ID, ARTIFACT_STREAM_ID);
    verify(wingsPersistence).delete(any(Query.class));
    verify(wingsPersistence).createQuery(any());
    verify(query).field("appId");
    verify(end).equal(APP_ID);
    verify(query).field(Mapper.ID_KEY);
    verify(end).equal(ARTIFACT_STREAM_ID);
  }

  @Test
  public void shouldAddStreamAction() {
    ArtifactStreamAction artifactStreamAction = anArtifactStreamAction()
                                                    .withCustomAction(true)
                                                    .withCronExpression("*.war")
                                                    .withWorkflowType(WorkflowType.ORCHESTRATION)
                                                    .withWorkflowId(WORKFLOW_ID)
                                                    .build();
    artifactStreamService.addStreamAction(APP_ID, ARTIFACT_STREAM_ID, artifactStreamAction);
    verify(wingsPersistence).createQuery(any());
    verify(query).field("appId");
    verify(end).equal(APP_ID);
    verify(query).field(Mapper.ID_KEY);
    verify(end).equal(ARTIFACT_STREAM_ID);
    verify(query).field("streamActions.workflowId");
    verify(end).notEqual(WORKFLOW_ID);
    verify(updateOperations).add("streamActions", artifactStreamAction);
    verify(wingsPersistence).update(query, updateOperations);
    verify(wingsPersistence).get(ArtifactStream.class, APP_ID, ARTIFACT_STREAM_ID);
  }

  @Test
  public void shouldDeleteStreamAction() {
    ArtifactStreamAction artifactStreamAction =
        anArtifactStreamAction().withWorkflowType(WorkflowType.ORCHESTRATION).withWorkflowId(WORKFLOW_ID).build();
    jenkinsArtifactStream.setStreamActions(asList(artifactStreamAction));
    when(query.get()).thenReturn(jenkinsArtifactStream);
    artifactStreamService.deleteStreamAction(APP_ID, ARTIFACT_STREAM_ID, WORKFLOW_ID);
    verify(wingsPersistence).createQuery(any());
    verify(query).field("appId");
    verify(end).equal(APP_ID);
    verify(query).field(Mapper.ID_KEY);
    verify(end).equal(ARTIFACT_STREAM_ID);
    verify(query).field("streamActions.workflowId");
    verify(end).equal(WORKFLOW_ID);
    verify(updateOperations).removeAll("streamActions", artifactStreamAction);
    verify(wingsPersistence).update(query, updateOperations);
    verify(wingsPersistence).get(ArtifactStream.class, APP_ID, ARTIFACT_STREAM_ID);
  }

  @Test
  public void shouldUpdateStreamAction() {
    ArtifactStreamAction artifactStreamAction =
        anArtifactStreamAction().withWorkflowType(WorkflowType.ORCHESTRATION).withWorkflowId(WORKFLOW_ID).build();
    jenkinsArtifactStream.setStreamActions(asList(artifactStreamAction));

    doReturn(jenkinsArtifactStream)
        .when(spyArtifactStreamService)
        .deleteStreamAction(APP_ID, ARTIFACT_STREAM_ID, WORKFLOW_ID);
    doReturn(jenkinsArtifactStream)
        .when(spyArtifactStreamService)
        .addStreamAction(APP_ID, ARTIFACT_STREAM_ID, artifactStreamAction);

    spyArtifactStreamService.updateStreamAction(APP_ID, ARTIFACT_STREAM_ID, artifactStreamAction);

    verify(spyArtifactStreamService).deleteStreamAction(APP_ID, ARTIFACT_STREAM_ID, WORKFLOW_ID);
    verify(spyArtifactStreamService).addStreamAction(APP_ID, ARTIFACT_STREAM_ID, artifactStreamAction);
  }

  @Test
  @Ignore
  public void shouldTriggerStreamActionAsync() {}
}
