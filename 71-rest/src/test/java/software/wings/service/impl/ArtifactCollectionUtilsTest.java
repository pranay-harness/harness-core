package software.wings.service.impl;

import static io.harness.rule.OwnerRule.GARVIT;
import static io.harness.rule.OwnerRule.ROHITKARELIA;
import static io.harness.rule.OwnerRule.SRINIVAS;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static software.wings.utils.ArtifactType.JAR;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_STREAM_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.SETTING_ID;

import com.google.inject.Inject;

import io.harness.beans.DelegateTask;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.TaskData;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.AwsConfig;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TaskType;
import software.wings.beans.artifact.AmazonS3ArtifactStream;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.CustomArtifactStream;
import software.wings.beans.artifact.DockerArtifactStream;
import software.wings.delegatetasks.buildsource.BuildSourceParameters;
import software.wings.service.impl.artifact.ArtifactCollectionUtils;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.ArtifactStreamServiceBindingService;
import software.wings.service.intfc.SettingsService;

import java.util.concurrent.TimeUnit;

public class ArtifactCollectionUtilsTest extends WingsBaseTest {
  @Mock private SettingsService settingsService;
  @Mock private ArtifactStreamService artifactStreamService;
  @Mock private ArtifactStreamServiceBindingService artifactStreamServiceBindingService;

  @Inject @InjectMocks private ArtifactCollectionUtils artifactCollectionUtils;
  private static final String SCRIPT_STRING = "echo Hello World!! and echo ${secrets.getValue(My Secret)}";

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void shouldSkipArtifactStreamIterationExcessiveFailedAttempts() {
    ArtifactStream artifactStream = DockerArtifactStream.builder().build();
    artifactStream.setFailedCronAttempts(PermitServiceImpl.MAX_FAILED_ATTEMPTS + 1);

    assertThat(artifactCollectionUtils.skipArtifactStreamIteration(artifactStream, true)).isTrue();
  }

  @Test(expected = InvalidRequestException.class)
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void shouldSkipArtifactStreamIterationForInvalidSetting() {
    when(settingsService.getOnlyConnectivityError(SETTING_ID)).thenReturn(null);
    ArtifactStream artifactStream = DockerArtifactStream.builder().settingId(SETTING_ID).build();

    artifactCollectionUtils.skipArtifactStreamIteration(artifactStream, true);
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void shouldSkipArtifactStreamIterationForConnectivityError() {
    when(settingsService.getOnlyConnectivityError(SETTING_ID))
        .thenReturn(SettingAttribute.Builder.aSettingAttribute().withConnectivityError("err").build());
    ArtifactStream artifactStream = DockerArtifactStream.builder().settingId(SETTING_ID).build();

    assertThat(artifactCollectionUtils.skipArtifactStreamIteration(artifactStream, true)).isTrue();
    assertThat(artifactCollectionUtils.skipArtifactStreamIteration(artifactStream, false)).isTrue();
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void shouldNotSkipArtifactStreamIterationForConnectivityError() {
    when(settingsService.getOnlyConnectivityError(SETTING_ID))
        .thenReturn(SettingAttribute.Builder.aSettingAttribute().build());
    ArtifactStream artifactStream = DockerArtifactStream.builder().settingId(SETTING_ID).build();

    assertThat(artifactCollectionUtils.skipArtifactStreamIteration(artifactStream, true)).isFalse();
    assertThat(artifactCollectionUtils.skipArtifactStreamIteration(artifactStream, false)).isFalse();
  }

  @Test
  @Owner(developers = ROHITKARELIA)
  @Category(UnitTests.class)
  public void shouldNotSkipArtifactStreamIterationForConnectivityErrorForCustomArtifactSource() {
    ArtifactStream artifactStream = CustomArtifactStream.builder().build();
    assertThat(artifactCollectionUtils.skipArtifactStreamIteration(artifactStream, true)).isFalse();
  }

  @Test
  @Owner(developers = SRINIVAS)
  @Category({UnitTests.class})
  public void shouldPrepareValidateTaskForCustomArtifactStream() {
    ArtifactStream customArtifactStream = CustomArtifactStream.builder()
                                              .accountId(ACCOUNT_ID)
                                              .appId(APP_ID)
                                              .uuid(ARTIFACT_STREAM_ID)
                                              .serviceId(SERVICE_ID)
                                              .name("Custom Artifact Stream" + System.currentTimeMillis())
                                              .scripts(asList(CustomArtifactStream.Script.builder()
                                                                  .action(CustomArtifactStream.Action.FETCH_VERSIONS)
                                                                  .scriptString(SCRIPT_STRING)
                                                                  .build()))
                                              .tags(asList("Delegate Tag"))
                                              .build();

    when(artifactStreamService.get(ARTIFACT_STREAM_ID)).thenReturn(customArtifactStream);
    final DelegateTask delegateTask = artifactCollectionUtils.prepareValidateTask(ARTIFACT_STREAM_ID);
    assertThat(delegateTask.getAccountId()).isEqualTo(ACCOUNT_ID);
    assertThat(delegateTask.getTags()).contains("Delegate Tag");
    TaskData data = delegateTask.getData();
    assertThat(data.getTaskType()).isEqualTo(TaskType.BUILD_SOURCE_TASK.name());
    assertThat(data.getTimeout()).isEqualTo(TimeUnit.MINUTES.toMillis(1));
    BuildSourceParameters parameters = (BuildSourceParameters) data.getParameters()[0];
    assertThat(parameters.getBuildSourceRequestType())
        .isEqualTo(BuildSourceParameters.BuildSourceRequestType.GET_BUILDS);
  }

  @Test
  @Owner(developers = SRINIVAS)
  @Category({UnitTests.class})
  public void shouldPrepareValidateTaskForS3ArtifactStream() {
    AmazonS3ArtifactStream amazonS3ArtifactStream = AmazonS3ArtifactStream.builder()
                                                        .accountId(ACCOUNT_ID)
                                                        .uuid(ARTIFACT_STREAM_ID)
                                                        .appId(APP_ID)
                                                        .settingId(SETTING_ID)
                                                        .jobname("harnessapps")
                                                        .name("Amazon S3")
                                                        .serviceId(SERVICE_ID)
                                                        .artifactPaths(asList("dev/todolist.war"))
                                                        .build();

    when(artifactStreamService.get(ARTIFACT_STREAM_ID)).thenReturn(amazonS3ArtifactStream);
    when(settingsService.get(SETTING_ID))
        .thenReturn(SettingAttribute.Builder.aSettingAttribute()
                        .withAccountId(ACCOUNT_ID)
                        .withValue(AwsConfig.builder().tag("AWS Tag").build())
                        .build());
    when(artifactStreamServiceBindingService.getService(APP_ID, ARTIFACT_STREAM_ID, true))
        .thenReturn(Service.builder()
                        .uuid(SERVICE_ID)
                        .appId(APP_ID)
                        .name("SERVICE_NAME")
                        .description("SERVICE_DESC")
                        .artifactType(JAR)
                        .build());

    final DelegateTask delegateTask = artifactCollectionUtils.prepareValidateTask(ARTIFACT_STREAM_ID);

    assertThat(delegateTask.getAccountId()).isEqualTo(ACCOUNT_ID);
    assertThat(delegateTask.getTags()).contains("AWS Tag");
    TaskData data = delegateTask.getData();
    assertThat(data.getTaskType()).isEqualTo(TaskType.BUILD_SOURCE_TASK.name());
    assertThat(data.getTimeout()).isEqualTo(TimeUnit.MINUTES.toMillis(1));
    BuildSourceParameters parameters = (BuildSourceParameters) data.getParameters()[0];
    assertThat(parameters.getBuildSourceRequestType())
        .isEqualTo(BuildSourceParameters.BuildSourceRequestType.GET_BUILDS);
    assertThat(parameters.getSettingValue()).isNotNull();
    assertThat(parameters.getEncryptedDataDetails()).isNotNull();
  }

  @Test(expected = InvalidRequestException.class)
  @Owner(developers = SRINIVAS)
  @Category({UnitTests.class})
  public void shouldPrepareValidateTaskArtifactStreamNotExists() {
    artifactCollectionUtils.prepareValidateTask(ARTIFACT_STREAM_ID);
  }

  @Test(expected = InvalidRequestException.class)
  @Owner(developers = SRINIVAS)
  @Category({UnitTests.class})
  public void shouldPrepareValidateTaskSettingAttributeNotExists() {
    AmazonS3ArtifactStream amazonS3ArtifactStream = AmazonS3ArtifactStream.builder()
                                                        .accountId(ACCOUNT_ID)
                                                        .uuid(ARTIFACT_STREAM_ID)
                                                        .appId(APP_ID)
                                                        .settingId(SETTING_ID)
                                                        .jobname("harnessapps")
                                                        .name("Amazon S3")
                                                        .serviceId(SERVICE_ID)
                                                        .artifactPaths(asList("dev/todolist.war"))
                                                        .build();

    when(artifactStreamService.get(ARTIFACT_STREAM_ID)).thenReturn(amazonS3ArtifactStream);
    artifactCollectionUtils.prepareValidateTask(ARTIFACT_STREAM_ID);
  }
}
