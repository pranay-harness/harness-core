package software.wings.collect;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.beans.TaskType.BAMBOO_COLLECTION;
import static software.wings.beans.TaskType.JENKINS_COLLECTION;
import static software.wings.beans.artifact.Artifact.Builder.anArtifact;
import static software.wings.beans.artifact.Artifact.ContentStatus.DOWNLOADING;
import static software.wings.beans.artifact.Artifact.Status.FAILED;
import static software.wings.beans.artifact.Artifact.Status.RUNNING;
import static software.wings.collect.CollectEvent.Builder.aCollectEvent;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_PATH;
import static software.wings.utils.WingsTestConstants.ARTIFACT_STREAM_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_STREAM_NAME;
import static software.wings.utils.WingsTestConstants.JENKINS_URL;
import static software.wings.utils.WingsTestConstants.JOB_NAME;
import static software.wings.utils.WingsTestConstants.PASSWORD;
import static software.wings.utils.WingsTestConstants.SETTING_ID;
import static software.wings.utils.WingsTestConstants.USER_NAME;

import com.google.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.Application;
import software.wings.beans.BambooConfig;
import software.wings.beans.DelegateTask;
import software.wings.beans.JenkinsConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.BambooArtifactStream;
import software.wings.beans.artifact.JenkinsArtifactStream;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.SettingsService;

public class ArtifactCollectEventListenerTest extends WingsBaseTest {
  private final Application APP = anApplication().withUuid(APP_ID).withAccountId(ACCOUNT_ID).build();

  @InjectMocks @Inject private ArtifactCollectEventListener artifactCollectEventListener;

  @Mock private AppService appService;

  @Mock private ArtifactService artifactService;

  @Mock private DelegateService delegateService;

  @Mock private SettingsService settingsService;

  @Mock private ArtifactStreamService artifactStreamService;

  @Before
  public void setupMocks() {
    when(appService.get(APP_ID)).thenReturn(APP);
  }

  @Test
  public void shouldSendJenkinsTask() {
    SettingAttribute SETTING_ATTRIBUTE = aSettingAttribute()
                                             .withValue(JenkinsConfig.builder()
                                                            .jenkinsUrl(JENKINS_URL)
                                                            .username(USER_NAME)
                                                            .password(PASSWORD)
                                                            .accountId(ACCOUNT_ID)
                                                            .build())
                                             .build();
    when(settingsService.get(SETTING_ID)).thenReturn(SETTING_ATTRIBUTE);

    ArtifactStream ARTIFACT_SOURCE = JenkinsArtifactStream.builder()
                                         .sourceName(ARTIFACT_STREAM_NAME)
                                         .appId(APP_ID)
                                         .settingId(SETTING_ID)
                                         .jobname(JOB_NAME)
                                         .artifactPaths(asList(ARTIFACT_PATH))
                                         .build();
    when(artifactStreamService.get(APP_ID, ARTIFACT_STREAM_ID)).thenReturn(ARTIFACT_SOURCE);

    artifactCollectEventListener.onMessage(
        aCollectEvent()
            .withArtifact(
                anArtifact().withUuid(ARTIFACT_ID).withAppId(APP_ID).withArtifactStreamId(ARTIFACT_STREAM_ID).build())
            .build());

    verify(artifactService).updateStatus(ARTIFACT_ID, APP_ID, RUNNING, DOWNLOADING);

    ArgumentCaptor<DelegateTask> delegateTaskArgumentCaptor = ArgumentCaptor.forClass(DelegateTask.class);
    verify(delegateService).queueTask(delegateTaskArgumentCaptor.capture());
    assertThat(delegateTaskArgumentCaptor.getValue())
        .isNotNull()
        .hasFieldOrPropertyWithValue("taskType", JENKINS_COLLECTION.name())
        .hasFieldOrProperty("parameters");
  }

  @Test
  public void shouldSendBambooTask() {
    SettingAttribute SETTING_ATTRIBUTE =
        aSettingAttribute()
            .withValue(BambooConfig.builder().bambooUrl(JENKINS_URL).username(USER_NAME).password(PASSWORD).build())
            .build();
    when(settingsService.get(SETTING_ID)).thenReturn(SETTING_ATTRIBUTE);

    ArtifactStream ARTIFACT_SOURCE = BambooArtifactStream.builder()
                                         .sourceName(ARTIFACT_STREAM_NAME)
                                         .appId(APP_ID)
                                         .settingId(SETTING_ID)
                                         .jobname(JOB_NAME)
                                         .artifactPaths(asList(ARTIFACT_PATH))
                                         .build();

    when(artifactStreamService.get(APP_ID, ARTIFACT_STREAM_ID)).thenReturn(ARTIFACT_SOURCE);

    artifactCollectEventListener.onMessage(
        aCollectEvent()
            .withArtifact(
                anArtifact().withUuid(ARTIFACT_ID).withAppId(APP_ID).withArtifactStreamId(ARTIFACT_STREAM_ID).build())
            .build());

    verify(artifactService).updateStatus(ARTIFACT_ID, APP_ID, RUNNING, DOWNLOADING);

    ArgumentCaptor<DelegateTask> delegateTaskArgumentCaptor = ArgumentCaptor.forClass(DelegateTask.class);
    verify(delegateService).queueTask(delegateTaskArgumentCaptor.capture());
    assertThat(delegateTaskArgumentCaptor.getValue())
        .isNotNull()
        .hasFieldOrPropertyWithValue("taskType", BAMBOO_COLLECTION.name())
        .hasFieldOrProperty("parameters");
  }

  @Test
  public void shouldFailToCollectArtifactWhenSourceIsMissing() throws Exception {
    artifactCollectEventListener.onMessage(
        aCollectEvent().withArtifact(anArtifact().withUuid(ARTIFACT_ID).withAppId(APP_ID).build()).build());

    verify(artifactService).updateStatus(ARTIFACT_ID, APP_ID, RUNNING, DOWNLOADING);
    verify(artifactService).updateStatus(ARTIFACT_ID, APP_ID, FAILED, Artifact.ContentStatus.FAILED);
  }
}
