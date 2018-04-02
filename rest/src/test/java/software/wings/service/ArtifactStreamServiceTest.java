package software.wings.service;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.artifact.DockerArtifactStream.Builder.aDockerArtifactStream;
import static software.wings.beans.artifact.JenkinsArtifactStream.Builder.aJenkinsArtifactStream;
import static software.wings.dl.PageRequest.PageRequestBuilder.aPageRequest;
import static software.wings.dl.PageResponse.PageResponseBuilder.aPageResponse;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_STREAM_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.SETTING_ID;
import static software.wings.utils.WingsTestConstants.TARGET_APP_ID;

import com.google.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mongodb.morphia.query.FieldEnd;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.quartz.JobDetail;
import org.quartz.Trigger;
import software.wings.WingsBaseTest;
import software.wings.beans.Application;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.ArtifactStreamAction;
import software.wings.beans.artifact.DockerArtifactStream;
import software.wings.beans.artifact.JenkinsArtifactStream;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.scheduler.JobScheduler;
import software.wings.service.impl.ArtifactStreamServiceImpl;
import software.wings.service.impl.yaml.YamlChangeSetHelper;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.BuildSourceService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.WorkflowService;
import software.wings.service.intfc.yaml.EntityUpdateService;

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
  @Mock private EntityUpdateService entityUpdateService;
  @Mock private AppService appService;
  @Mock private YamlChangeSetHelper yamlChangeSetHelper;

  @Inject @InjectMocks private ArtifactStreamService artifactStreamService;

  @Spy @InjectMocks private ArtifactStreamService spyArtifactStreamService = new ArtifactStreamServiceImpl();

  private JenkinsArtifactStream jenkinsArtifactStream = aJenkinsArtifactStream()
                                                            .withAppId(APP_ID)
                                                            .withUuid(ARTIFACT_STREAM_ID)
                                                            .withSourceName("SOURCE_NAME")
                                                            .withSettingId(SETTING_ID)
                                                            .withJobname("JOB")
                                                            .withServiceId(SERVICE_ID)
                                                            .withArtifactPaths(asList("*WAR"))
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
    when(wingsPersistence.createUpdateOperations(ArtifactStream.class)).thenReturn(updateOperations);
    when(updateOperations.addToSet(any(), any())).thenReturn(updateOperations);
    when(updateOperations.removeAll(any(String.class), any(ArtifactStreamAction.class))).thenReturn(updateOperations);
    when(appService.get(TARGET_APP_ID))
        .thenReturn(Application.Builder.anApplication().withAccountId(ACCOUNT_ID).build());
    when(appService.get(APP_ID)).thenReturn(Application.Builder.anApplication().withAccountId(ACCOUNT_ID).build());
  }

  @Test
  public void shouldList() {
    PageRequest pageRequest = aPageRequest().build();
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
    when(wingsPersistence.get(ArtifactStream.class, APP_ID, ARTIFACT_STREAM_ID)).thenReturn(dockerArtifactStream);
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
    when(wingsPersistence.get(ArtifactStream.class, APP_ID, ARTIFACT_STREAM_ID)).thenReturn(dockerArtifactStream);
    when(wingsPersistence.saveAndGet(ArtifactStream.class, dockerArtifactStream)).thenReturn(dockerArtifactStream);
    when(buildSourceService.validateArtifactSource(dockerArtifactStream.getAppId(), dockerArtifactStream.getSettingId(),
             dockerArtifactStream.getArtifactStreamAttributes()))
        .thenReturn(true);
    ArtifactStream artifactStream = artifactStreamService.update(dockerArtifactStream);
    assertThat(artifactStream.getUuid()).isEqualTo(ARTIFACT_STREAM_ID);
    verify(wingsPersistence, times(1)).get(ArtifactStream.class, APP_ID, ARTIFACT_STREAM_ID);
    verify(wingsPersistence).saveAndGet(ArtifactStream.class, dockerArtifactStream);
  }

  @Test
  public void shouldUpdate() {
    when(wingsPersistence.get(ArtifactStream.class, APP_ID, ARTIFACT_STREAM_ID)).thenReturn(jenkinsArtifactStream);
    when(wingsPersistence.saveAndGet(ArtifactStream.class, jenkinsArtifactStream)).thenReturn(jenkinsArtifactStream);
    ArtifactStream artifactStream = artifactStreamService.update(jenkinsArtifactStream);
    assertThat(artifactStream.getUuid()).isEqualTo(ARTIFACT_STREAM_ID);
    verify(wingsPersistence, times(1)).get(ArtifactStream.class, APP_ID, ARTIFACT_STREAM_ID);
    verify(wingsPersistence).saveAndGet(ArtifactStream.class, jenkinsArtifactStream);
  }
}
