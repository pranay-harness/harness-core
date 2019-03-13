package software.wings.beans.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.beans.artifact.ArtifactStreamAttributes.Builder.anArtifactStreamAttributes;
import static software.wings.beans.command.CommandExecutionContext.Builder.aCommandExecutionContext;
import static software.wings.utils.WingsTestConstants.ACCESS_KEY;
import static software.wings.utils.WingsTestConstants.ARTIFACT_FILE_NAME;
import static software.wings.utils.WingsTestConstants.ARTIFACT_PATH;
import static software.wings.utils.WingsTestConstants.BUCKET_NAME;
import static software.wings.utils.WingsTestConstants.BUILD_NO;
import static software.wings.utils.WingsTestConstants.S3_URL;
import static software.wings.utils.WingsTestConstants.SECRET_KEY;
import static software.wings.utils.WingsTestConstants.SETTING_ID;

import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.AwsConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.beans.artifact.ArtifactStreamType;
import software.wings.beans.command.ScpCommandUnit.ScpFileCategory;
import software.wings.beans.config.ArtifactoryConfig;
import software.wings.common.Constants;
import software.wings.core.BaseExecutor;
import software.wings.utils.ArtifactType;
import software.wings.utils.WingsTestConstants;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ScpCommandUnitTest extends WingsBaseTest {
  @InjectMocks private ScpCommandUnit scpCommandUnit = new ScpCommandUnit();
  @Mock BaseExecutor baseExecutor;
  private SettingAttribute awsSetting =
      aSettingAttribute()
          .withUuid(SETTING_ID)
          .withValue(AwsConfig.builder().secretKey(SECRET_KEY).accessKey(ACCESS_KEY).build())
          .build();
  private SettingAttribute artifactorySetting = aSettingAttribute()
                                                    .withUuid(SETTING_ID)
                                                    .withValue(ArtifactoryConfig.builder()
                                                                   .artifactoryUrl(WingsTestConstants.ARTIFACTORY_URL)
                                                                   .username("admin")
                                                                   .password("dummy123!".toCharArray())
                                                                   .build())
                                                    .build();
  private ArtifactStreamAttributes artifactStreamAttributesForS3 =
      anArtifactStreamAttributes()
          .artifactStreamType(ArtifactStreamType.AMAZON_S3.name())
          .metadataOnly(true)
          .metadata(mockMetadata(ArtifactStreamType.AMAZON_S3))
          .serverSetting(awsSetting)
          .artifactServerEncryptedDataDetails(Collections.emptyList())
          .build();
  private ArtifactStreamAttributes artifactStreamAttributesForArtifactory =
      anArtifactStreamAttributes()
          .artifactStreamType(ArtifactStreamType.ARTIFACTORY.name())
          .metadataOnly(true)
          .metadata(mockMetadata(ArtifactStreamType.ARTIFACTORY))
          .serverSetting(artifactorySetting)
          .artifactServerEncryptedDataDetails(Collections.emptyList())
          .copyArtifactEnabledForArtifactory(true)
          .artifactType(ArtifactType.WAR)
          .build();

  private ArtifactStreamAttributes artifactStreamAttributesForArtifactoryRpmType =
      anArtifactStreamAttributes()
          .artifactStreamType(ArtifactStreamType.ARTIFACTORY.name())
          .metadataOnly(true)
          .metadata(mockMetadata(ArtifactStreamType.ARTIFACTORY))
          .serverSetting(artifactorySetting)
          .artifactServerEncryptedDataDetails(Collections.emptyList())
          .copyArtifactEnabledForArtifactory(true)
          .artifactType(ArtifactType.RPM)
          .build();

  private ArtifactStreamAttributes artifactStreamAttributesForArtifactoryFeatureFlagDisabled =
      anArtifactStreamAttributes()
          .artifactStreamType(ArtifactStreamType.ARTIFACTORY.name())
          .metadataOnly(true)
          .metadata(mockMetadata(ArtifactStreamType.ARTIFACTORY))
          .serverSetting(artifactorySetting)
          .artifactServerEncryptedDataDetails(Collections.emptyList())
          .copyArtifactEnabledForArtifactory(false)
          .artifactType(ArtifactType.TAR)
          .build();

  @InjectMocks
  private ShellCommandExecutionContext contextForS3 = new ShellCommandExecutionContext(
      aCommandExecutionContext().withArtifactStreamAttributes(artifactStreamAttributesForS3).build());

  @InjectMocks
  private ShellCommandExecutionContext contextForArtifactory = new ShellCommandExecutionContext(
      aCommandExecutionContext().withArtifactStreamAttributes(artifactStreamAttributesForArtifactory).build());

  @InjectMocks
  private ShellCommandExecutionContext contextForArtifactoryRpm = new ShellCommandExecutionContext(
      aCommandExecutionContext().withArtifactStreamAttributes(artifactStreamAttributesForArtifactoryRpmType).build());

  @InjectMocks
  private ShellCommandExecutionContext contextForArtifactoryFeatureFlagDisabled = new ShellCommandExecutionContext(
      aCommandExecutionContext()
          .withArtifactStreamAttributes(artifactStreamAttributesForArtifactoryFeatureFlagDisabled)
          .build());

  /**
   * Sets up mocks.
   *
   * @throws Exception the exception
   */
  @Before
  public void setup() {
    scpCommandUnit.setFileCategory(ScpFileCategory.ARTIFACTS);
    scpCommandUnit.setDestinationDirectoryPath(WingsTestConstants.DESTINATION_DIR_PATH);
  }

  @Test
  public void shouldDownloadArtifactFromAmazonS3IfMetadataOnly() {
    when(baseExecutor.copyFiles(anyString(), any(ArtifactStreamAttributes.class), anyString(), anyString(), anyString(),
             anyString(), anyString()))
        .thenReturn(CommandExecutionStatus.SUCCESS);
    CommandExecutionStatus status = scpCommandUnit.executeInternal(contextForS3);
    assertThat(status).isEqualTo(CommandExecutionStatus.SUCCESS);
  }

  @Test
  public void shouldDownloadArtifactFromArtifactoryIfMetadataOnly() {
    when(baseExecutor.copyFiles(anyString(), any(ArtifactStreamAttributes.class), anyString(), anyString(), anyString(),
             anyString(), anyString()))
        .thenReturn(CommandExecutionStatus.SUCCESS);
    CommandExecutionStatus status = scpCommandUnit.executeInternal(contextForArtifactory);
    assertThat(status).isEqualTo(CommandExecutionStatus.SUCCESS);
  }

  @Test
  public void shouldNotDownloadArtifactFromArtifactoryForRpmType() {
    when(baseExecutor.copyFiles(anyString(), any(ArtifactStreamAttributes.class), anyString(), anyString(), anyString(),
             anyString(), anyString()))
        .thenReturn(CommandExecutionStatus.SUCCESS);
    CommandExecutionStatus status = scpCommandUnit.executeInternal(contextForArtifactoryRpm);
    assertThat(status).isEqualTo(CommandExecutionStatus.SUCCESS);
  }

  @Test
  public void shoudlNotDownloadArtifactFromArtifactoryIfFeatureFlagDisabled() {
    when(baseExecutor.copyFiles(anyString(), any(ArtifactStreamAttributes.class), anyString(), anyString(), anyString(),
             anyString(), anyString()))
        .thenReturn(CommandExecutionStatus.SUCCESS);
    CommandExecutionStatus status = scpCommandUnit.executeInternal(contextForArtifactoryFeatureFlagDisabled);
    assertThat(status).isEqualTo(CommandExecutionStatus.SUCCESS);
  }

  private Map<String, String> mockMetadata(ArtifactStreamType artifactStreamType) {
    Map<String, String> map = new HashMap<>();
    switch (artifactStreamType) {
      case AMAZON_S3:
        map.put(Constants.BUCKET_NAME, BUCKET_NAME);
        map.put(Constants.ARTIFACT_FILE_NAME, ARTIFACT_FILE_NAME);
        map.put(Constants.ARTIFACT_PATH, ARTIFACT_PATH);
        map.put(Constants.BUILD_NO, BUILD_NO);
        map.put(Constants.ARTIFACT_FILE_SIZE, String.valueOf(WingsTestConstants.ARTIFACT_FILE_SIZE));
        map.put(Constants.KEY, ACCESS_KEY);
        map.put(Constants.URL, S3_URL);
        break;
      case ARTIFACTORY:
        map.put(Constants.ARTIFACT_FILE_NAME, ARTIFACT_FILE_NAME);
        map.put(Constants.ARTIFACT_PATH, ARTIFACT_PATH);
        map.put(Constants.BUILD_NO, BUILD_NO);
        map.put(Constants.ARTIFACT_FILE_SIZE, String.valueOf(WingsTestConstants.ARTIFACT_FILE_SIZE));
        break;
      default:
        break;
    }
    return map;
  }
}
