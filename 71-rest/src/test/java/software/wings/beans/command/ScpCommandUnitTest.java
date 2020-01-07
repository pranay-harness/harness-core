package software.wings.beans.command;

import static io.harness.delegate.beans.artifact.ArtifactFileMetadata.builder;
import static io.harness.rule.OwnerRule.AADITI;
import static io.harness.rule.OwnerRule.GARVIT;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.beans.command.CommandExecutionContext.Builder.aCommandExecutionContext;
import static software.wings.utils.WingsTestConstants.ACCESS_KEY;
import static software.wings.utils.WingsTestConstants.ACTIVITY_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_FILE_NAME;
import static software.wings.utils.WingsTestConstants.ARTIFACT_PATH;
import static software.wings.utils.WingsTestConstants.ARTIFACT_STREAM_ID;
import static software.wings.utils.WingsTestConstants.BUCKET_NAME;
import static software.wings.utils.WingsTestConstants.BUILD_NO;
import static software.wings.utils.WingsTestConstants.HOST_NAME;
import static software.wings.utils.WingsTestConstants.S3_URL;
import static software.wings.utils.WingsTestConstants.SECRET_KEY;
import static software.wings.utils.WingsTestConstants.SETTING_ID;

import io.harness.category.element.UnitTests;
import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.AwsConfig;
import software.wings.beans.JenkinsConfig;
import software.wings.beans.Log;
import software.wings.beans.SettingAttribute;
import software.wings.beans.artifact.Artifact.ArtifactMetadataKeys;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.beans.artifact.ArtifactStreamType;
import software.wings.beans.artifact.AzureArtifactsArtifactStream.ProtocolType;
import software.wings.beans.command.ScpCommandUnit.ScpFileCategory;
import software.wings.beans.config.ArtifactoryConfig;
import software.wings.beans.infrastructure.Host;
import software.wings.beans.settings.azureartifacts.AzureArtifactsConfig;
import software.wings.beans.settings.azureartifacts.AzureArtifactsPATConfig;
import software.wings.core.BaseScriptExecutor;
import software.wings.delegatetasks.DelegateLogService;
import software.wings.helpers.ext.azure.devops.AzureArtifactsPackageFileInfo;
import software.wings.helpers.ext.azure.devops.AzureArtifactsService;
import software.wings.helpers.ext.jenkins.Jenkins;
import software.wings.service.impl.jenkins.JenkinsUtils;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.utils.ArtifactType;
import software.wings.utils.WingsTestConstants;

import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ScpCommandUnitTest extends WingsBaseTest {
  private static final String JENKINS_ARTIFACT_URL_1 =
      "http://localhost:8089/job/scheduler-svn/75/artifact/build/libs/docker-scheduler-1.0-SNAPSHOT-all.jar";
  private static final String JENKINS_ARTIFACT_URL_2 =
      "http://localhost:8089/job/scheduler-svn/75/artifact/build/libs/docker-scheduler-1.0-SNAPSHOT-sources.jar";
  private static final String JENKINS_ARTIFACT_FILENAME_1 = "docker-scheduler-1.0-SNAPSHOT-all.jar";
  private static final String JENKINS_ARTIFACT_FILENAME_2 = "docker-scheduler-1.0-SNAPSHOT-sources.jar";
  @InjectMocks private ScpCommandUnit scpCommandUnit = new ScpCommandUnit();
  @Mock BaseScriptExecutor baseExecutor;
  @Mock AzureArtifactsService azureArtifactsService;
  @Mock EncryptionService encryptionService;
  @Mock JenkinsUtils jenkinsUtils;
  @Mock Jenkins jenkins;
  @Mock DelegateLogService delegateLogService;

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
  private SettingAttribute azureArtifactsConfig =
      aSettingAttribute()
          .withUuid(SETTING_ID)
          .withValue(AzureArtifactsPATConfig.builder()
                         .azureDevopsUrl(WingsTestConstants.AZURE_DEVOPS_URL)
                         .pat("dummy123!".toCharArray())
                         .build())
          .build();

  private SettingAttribute jenkinsConfig = aSettingAttribute()
                                               .withUuid(SETTING_ID)
                                               .withValue(JenkinsConfig.builder()
                                                              .jenkinsUrl(WingsTestConstants.HARNESS_JENKINS)
                                                              .username("admin")
                                                              .password("dummy123!".toCharArray())
                                                              .build())
                                               .build();

  private ArtifactStreamAttributes artifactStreamAttributesForS3 =
      ArtifactStreamAttributes.builder()
          .artifactStreamType(ArtifactStreamType.AMAZON_S3.name())
          .metadataOnly(true)
          .metadata(mockMetadata(ArtifactStreamType.AMAZON_S3))
          .serverSetting(awsSetting)
          .artifactServerEncryptedDataDetails(Collections.emptyList())
          .build();
  private ArtifactStreamAttributes artifactStreamAttributesForArtifactory =
      ArtifactStreamAttributes.builder()
          .artifactStreamType(ArtifactStreamType.ARTIFACTORY.name())
          .metadataOnly(true)
          .metadata(mockMetadata(ArtifactStreamType.ARTIFACTORY))
          .serverSetting(artifactorySetting)
          .artifactServerEncryptedDataDetails(Collections.emptyList())
          .copyArtifactEnabled(true)
          .artifactType(ArtifactType.WAR)
          .build();

  private ArtifactStreamAttributes artifactStreamAttributesForArtifactoryRpmType =
      ArtifactStreamAttributes.builder()
          .artifactStreamType(ArtifactStreamType.ARTIFACTORY.name())
          .metadataOnly(true)
          .metadata(mockMetadata(ArtifactStreamType.ARTIFACTORY))
          .serverSetting(artifactorySetting)
          .artifactServerEncryptedDataDetails(Collections.emptyList())
          .copyArtifactEnabled(true)
          .artifactType(ArtifactType.RPM)
          .build();

  private ArtifactStreamAttributes artifactStreamAttributesForArtifactoryFeatureFlagDisabled =
      ArtifactStreamAttributes.builder()
          .artifactStreamType(ArtifactStreamType.ARTIFACTORY.name())
          .metadataOnly(true)
          .metadata(mockMetadata(ArtifactStreamType.ARTIFACTORY))
          .serverSetting(artifactorySetting)
          .artifactServerEncryptedDataDetails(Collections.emptyList())
          .copyArtifactEnabled(false)
          .artifactType(ArtifactType.TAR)
          .build();

  private ArtifactStreamAttributes artifactStreamAttributesForAzureArtifacts =
      ArtifactStreamAttributes.builder()
          .artifactStreamType(ArtifactStreamType.AZURE_ARTIFACTS.name())
          .metadataOnly(true)
          .metadata(mockMetadata(ArtifactStreamType.AZURE_ARTIFACTS))
          .serverSetting(azureArtifactsConfig)
          .artifactServerEncryptedDataDetails(Collections.emptyList())
          .protocolType(ProtocolType.maven.name())
          .project("PROJECT")
          .feed("FEED")
          .packageId("PACKAGE_ID")
          .packageName("GROUP_ID:ARTIFACT_ID")
          .build();

  private ArtifactStreamAttributes artifactStreamAttributesForJenkins =
      ArtifactStreamAttributes.builder()
          .artifactStreamType(ArtifactStreamType.JENKINS.name())
          .metadataOnly(true)
          .metadata(mockMetadata(ArtifactStreamType.JENKINS))
          .artifactFileMetadata(
              asList(builder().fileName(JENKINS_ARTIFACT_FILENAME_1).url(JENKINS_ARTIFACT_URL_1).build(),
                  builder().fileName(JENKINS_ARTIFACT_FILENAME_2).url(JENKINS_ARTIFACT_URL_2).build()))
          .serverSetting(jenkinsConfig)
          .artifactStreamId(ARTIFACT_STREAM_ID)
          .jobName("scheduler")
          .artifactPaths(asList("build/libs/docker-scheduler-1.0-SNAPSHOT-all.jar",
              "build/libs/docker-scheduler-1.0-SNAPSHOT-sources.jar"))
          .artifactServerEncryptedDataDetails(Collections.emptyList())
          .copyArtifactEnabled(true)
          .build();

  private ArtifactStreamAttributes artifactStreamAttributesForJenkinsOld =
      ArtifactStreamAttributes.builder()
          .artifactStreamType(ArtifactStreamType.JENKINS.name())
          .metadataOnly(true)
          .metadata(mockMetadata(ArtifactStreamType.JENKINS))
          .serverSetting(jenkinsConfig)
          .artifactStreamId(ARTIFACT_STREAM_ID)
          .jobName("scheduler")
          .artifactPaths(asList("build/libs/docker-scheduler-1.0-SNAPSHOT-all.jar",
              "build/libs/docker-scheduler-1.0-SNAPSHOT-sources.jar"))
          .artifactServerEncryptedDataDetails(Collections.emptyList())
          .copyArtifactEnabled(true)
          .build();

  private ArtifactStreamAttributes artifactStreamAttributesForJenkinsFFOff =
      ArtifactStreamAttributes.builder()
          .artifactStreamType(ArtifactStreamType.JENKINS.name())
          .metadataOnly(true)
          .copyArtifactEnabled(false)
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

  @InjectMocks
  ShellCommandExecutionContext contextForAzureArtifacts =
      new ShellCommandExecutionContext(aCommandExecutionContext()
                                           .withArtifactStreamAttributes(artifactStreamAttributesForAzureArtifacts)
                                           .withMetadata(mockMetadata(ArtifactStreamType.AZURE_ARTIFACTS))
                                           .build());

  @InjectMocks
  ShellCommandExecutionContext contextForJenkins =
      new ShellCommandExecutionContext(aCommandExecutionContext()
                                           .withArtifactStreamAttributes(artifactStreamAttributesForJenkins)
                                           .withMetadata(mockMetadata(ArtifactStreamType.JENKINS))
                                           .build());

  @InjectMocks
  ShellCommandExecutionContext contextForJenkinsOld =
      new ShellCommandExecutionContext(aCommandExecutionContext()
                                           .withArtifactStreamAttributes(artifactStreamAttributesForJenkinsOld)
                                           .withMetadata(mockMetadata(ArtifactStreamType.JENKINS))
                                           .withAppId(APP_ID)
                                           .withActivityId(ACTIVITY_ID)
                                           .withHost(Host.Builder.aHost().withHostName(HOST_NAME).build())
                                           .build());

  @InjectMocks
  ShellCommandExecutionContext contextForJenkinsFFOff =
      new ShellCommandExecutionContext(aCommandExecutionContext()
                                           .withArtifactStreamAttributes(artifactStreamAttributesForJenkinsFFOff)
                                           .withMetadata(mockMetadata(ArtifactStreamType.JENKINS))
                                           .build());

  public ScpCommandUnitTest() throws URISyntaxException {}

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
  @Owner(developers = AADITI)
  @Category(UnitTests.class)
  public void shouldDownloadArtifactFromAmazonS3IfMetadataOnly() {
    when(baseExecutor.copyFiles(anyString(), any(ArtifactStreamAttributes.class), anyString(), anyString(), anyString(),
             anyString(), anyString()))
        .thenReturn(CommandExecutionStatus.SUCCESS);
    CommandExecutionStatus status = scpCommandUnit.executeInternal(contextForS3);
    assertThat(status).isEqualTo(CommandExecutionStatus.SUCCESS);
  }

  @Test
  @Owner(developers = AADITI)
  @Category(UnitTests.class)
  public void shouldDownloadArtifactFromArtifactoryIfMetadataOnly() {
    when(baseExecutor.copyFiles(anyString(), any(ArtifactStreamAttributes.class), anyString(), anyString(), anyString(),
             anyString(), anyString()))
        .thenReturn(CommandExecutionStatus.SUCCESS);
    CommandExecutionStatus status = scpCommandUnit.executeInternal(contextForArtifactory);
    assertThat(status).isEqualTo(CommandExecutionStatus.SUCCESS);
  }

  @Test
  @Owner(developers = AADITI)
  @Category(UnitTests.class)
  public void shouldNotDownloadArtifactFromArtifactoryForRpmType() {
    when(baseExecutor.copyFiles(anyString(), any(ArtifactStreamAttributes.class), anyString(), anyString(), anyString(),
             anyString(), anyString()))
        .thenReturn(CommandExecutionStatus.SUCCESS);
    CommandExecutionStatus status = scpCommandUnit.executeInternal(contextForArtifactoryRpm);
    assertThat(status).isEqualTo(CommandExecutionStatus.SUCCESS);
  }

  @Test
  @Owner(developers = AADITI)
  @Category(UnitTests.class)
  public void shouldNotDownloadArtifactFromArtifactoryIfFeatureFlagDisabled() {
    when(baseExecutor.copyFiles(anyString(), any(ArtifactStreamAttributes.class), anyString(), anyString(), anyString(),
             anyString(), anyString()))
        .thenReturn(CommandExecutionStatus.SUCCESS);
    CommandExecutionStatus status = scpCommandUnit.executeInternal(contextForArtifactoryFeatureFlagDisabled);
    assertThat(status).isEqualTo(CommandExecutionStatus.SUCCESS);
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void shouldDownloadArtifactFromAzureArtifactsIfMetadataOnly() {
    when(azureArtifactsService.listFiles(any(AzureArtifactsConfig.class), any(), any(), anyMap(), eq(false)))
        .thenReturn(Collections.singletonList(new AzureArtifactsPackageFileInfo(ARTIFACT_FILE_NAME, 10)));
    when(baseExecutor.copyFiles(anyString(), any(ArtifactStreamAttributes.class), anyString(), anyString(), anyString(),
             anyString(), anyString()))
        .thenReturn(CommandExecutionStatus.SUCCESS);
    CommandExecutionStatus status = scpCommandUnit.executeInternal(contextForAzureArtifacts);
    assertThat(status).isEqualTo(CommandExecutionStatus.SUCCESS);
  }

  @Test
  @Owner(developers = AADITI)
  @Category(UnitTests.class)
  public void shouldDownloadArtifactFromJenkinsIfMetadataOnly() {
    when(encryptionService.decrypt(any(), any())).thenReturn(null);
    when(jenkinsUtils.getJenkins(any())).thenReturn(jenkins);
    when(jenkins.getFileSize(anyString(), anyString(), eq(JENKINS_ARTIFACT_FILENAME_1))).thenReturn(123L);
    when(jenkins.getFileSize(anyString(), anyString(), eq(JENKINS_ARTIFACT_FILENAME_2))).thenReturn(2323L);
    when(baseExecutor.copyFiles(anyString(), any(ArtifactStreamAttributes.class), anyString(), anyString(), anyString(),
             anyString(), anyString()))
        .thenReturn(CommandExecutionStatus.SUCCESS);
    when(baseExecutor.copyFiles(anyString(), any(ArtifactStreamAttributes.class), anyString(), anyString(), anyString(),
             anyString(), anyString()))
        .thenReturn(CommandExecutionStatus.SUCCESS);
    CommandExecutionStatus status = scpCommandUnit.executeInternal(contextForJenkins);
    assertThat(status).isEqualTo(CommandExecutionStatus.SUCCESS);
  }

  @Test
  @Owner(developers = AADITI)
  @Category(UnitTests.class)
  public void shouldNotDownloadArtifactFromJenkinsIfNoArtifactFileMetadata() {
    CommandExecutionStatus status = scpCommandUnit.executeInternal(contextForJenkinsOld);
    assertThat(status).isEqualTo(CommandExecutionStatus.SUCCESS);
    verify(delegateLogService, times(1)).save(anyString(), any(Log.class));
  }

  @Test
  @Owner(developers = AADITI)
  @Category(UnitTests.class)
  public void shouldlNotDownloadArtifactFromJenkinsIfFeatureFlagDisabled() {
    when(baseExecutor.copyFiles(anyString(), any(ArtifactStreamAttributes.class), anyString(), anyString(), anyString(),
             anyString(), anyString()))
        .thenReturn(CommandExecutionStatus.SUCCESS);
    CommandExecutionStatus status = scpCommandUnit.executeInternal(contextForJenkinsFFOff);
    assertThat(status).isEqualTo(CommandExecutionStatus.SUCCESS);
  }

  private Map<String, String> mockMetadata(ArtifactStreamType artifactStreamType) {
    Map<String, String> map = new HashMap<>();
    switch (artifactStreamType) {
      case AMAZON_S3:
        map.put(ArtifactMetadataKeys.bucketName, BUCKET_NAME);
        map.put(ArtifactMetadataKeys.artifactFileName, ARTIFACT_FILE_NAME);
        map.put(ArtifactMetadataKeys.artifactPath, ARTIFACT_PATH);
        map.put(ArtifactMetadataKeys.buildNo, BUILD_NO);
        map.put(ArtifactMetadataKeys.artifactFileSize, String.valueOf(WingsTestConstants.ARTIFACT_FILE_SIZE));
        map.put(ArtifactMetadataKeys.key, ACCESS_KEY);
        map.put(ArtifactMetadataKeys.url, S3_URL);
        break;
      case ARTIFACTORY:
        map.put(ArtifactMetadataKeys.artifactFileName, ARTIFACT_FILE_NAME);
        map.put(ArtifactMetadataKeys.artifactPath, ARTIFACT_PATH);
        map.put(ArtifactMetadataKeys.buildNo, BUILD_NO);
        map.put(ArtifactMetadataKeys.artifactFileSize, String.valueOf(WingsTestConstants.ARTIFACT_FILE_SIZE));
        break;
      case AZURE_ARTIFACTS:
        map.put(ArtifactMetadataKeys.version, BUILD_NO);
        map.put(ArtifactMetadataKeys.buildNo, BUILD_NO);
        break;
      case JENKINS:
        map.put(ArtifactMetadataKeys.buildNo, BUILD_NO);
        break;
      default:
        break;
    }
    return map;
  }
}
