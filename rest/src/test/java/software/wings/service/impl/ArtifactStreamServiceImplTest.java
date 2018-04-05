package software.wings.service.impl;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static software.wings.beans.artifact.JenkinsArtifactStream.Builder.aJenkinsArtifactStream;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_SOURCE_NAME;
import static software.wings.utils.WingsTestConstants.ARTIFACT_STREAM_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.SETTING_ID;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.beans.artifact.JenkinsArtifactStream;
import software.wings.dl.WingsPersistence;
import software.wings.scheduler.QuartzScheduler;
import software.wings.service.impl.yaml.YamlChangeSetHelper;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.BuildSourceService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.TriggerService;
import software.wings.service.intfc.yaml.EntityUpdateService;
import software.wings.service.intfc.yaml.YamlChangeSetService;
import software.wings.service.intfc.yaml.YamlDirectoryService;
import software.wings.stencils.StencilPostProcessor;

import java.util.concurrent.ExecutorService;

public class ArtifactStreamServiceImplTest {
  @Mock private WingsPersistence wingsPersistence;
  @Mock private ExecutorService executorService;
  @Mock private QuartzScheduler jobScheduler;
  @Mock private StencilPostProcessor stencilPostProcessor;
  @Mock private ServiceResourceService serviceResourceService;
  @Mock private BuildSourceService buildSourceService;
  @Mock private EntityUpdateService entityUpdateService;
  @Mock private YamlDirectoryService yamlDirectoryService;
  @Mock private AppService appService;
  @Mock private TriggerService triggerService;
  @Mock private YamlChangeSetService yamlChangeSetService;
  @Mock private YamlChangeSetHelper yamlChangeSetHelper;
  @Mock private transient FeatureFlagService featureFlagService;

  @InjectMocks private ArtifactStreamServiceImpl artifactStreamService = spy(ArtifactStreamServiceImpl.class);

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void testUpdateWithNewName() {
    final JenkinsArtifactStream savedJenkinsArtifactStream = aJenkinsArtifactStream()
                                                                 .withAppId(APP_ID)
                                                                 .withUuid(ARTIFACT_STREAM_ID)
                                                                 .withSourceName(ARTIFACT_SOURCE_NAME)
                                                                 .withSettingId(SETTING_ID)
                                                                 .withJobname("JOB")
                                                                 .withServiceId(SERVICE_ID)
                                                                 .withArtifactPaths(asList("*WAR"))
                                                                 .build();
    final String originalName = "Saved";
    savedJenkinsArtifactStream.setName(originalName);
    doReturn(savedJenkinsArtifactStream)
        .when(wingsPersistence)
        .get(eq(ArtifactStream.class), eq(APP_ID), eq(ARTIFACT_STREAM_ID));
    doReturn(true)
        .when(buildSourceService)
        .validateArtifactSource(anyString(), anyString(), any(ArtifactStreamAttributes.class));
    final JenkinsArtifactStream toUpdateJenkinsArtifactStream = aJenkinsArtifactStream()
                                                                    .withAppId(APP_ID)
                                                                    .withUuid(ARTIFACT_STREAM_ID)
                                                                    .withSourceName(ARTIFACT_SOURCE_NAME)
                                                                    .withSettingId(SETTING_ID)
                                                                    .withJobname("JOB")
                                                                    .withServiceId(SERVICE_ID)
                                                                    .withArtifactPaths(asList("*WAR"))
                                                                    .build();
    final String updatedName = "Updated";
    toUpdateJenkinsArtifactStream.setName(updatedName); // Change name
    doReturn(toUpdateJenkinsArtifactStream)
        .when(wingsPersistence)
        .saveAndGet(eq(ArtifactStream.class), eq(toUpdateJenkinsArtifactStream));
    doReturn(null).when(executorService).submit(any(Runnable.class));
    doReturn(ACCOUNT_ID).when(appService).getAccountIdByAppId(anyString());
    doNothing()
        .when(yamlChangeSetHelper)
        .updateYamlChangeAsync(any(ArtifactStream.class), any(ArtifactStream.class), eq(ACCOUNT_ID));
    final ArtifactStream updatedArtifactStream = artifactStreamService.update(toUpdateJenkinsArtifactStream);
    assertThat(updatedArtifactStream.getName()).isEqualTo(updatedName);
  }
}
