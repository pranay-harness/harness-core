package software.wings.beans.command;

import static io.harness.delegate.beans.artifact.ArtifactFileMetadata.builder;
import static io.harness.rule.OwnerRule.AADITI;
import static io.harness.rule.OwnerRule.UNKNOWN;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.beans.command.CommandExecutionContext.Builder.aCommandExecutionContext;
import static software.wings.utils.WingsTestConstants.ACCESS_KEY;
import static software.wings.utils.WingsTestConstants.ACTIVITY_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_FILE_NAME;
import static software.wings.utils.WingsTestConstants.ARTIFACT_PATH;
import static software.wings.utils.WingsTestConstants.ARTIFACT_STREAM_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_STREAM_ID_ARTIFACTORY;
import static software.wings.utils.WingsTestConstants.BUCKET_NAME;
import static software.wings.utils.WingsTestConstants.BUILD_NO;
import static software.wings.utils.WingsTestConstants.S3_URL;
import static software.wings.utils.WingsTestConstants.SECRET_KEY;
import static software.wings.utils.WingsTestConstants.SETTING_ID;

import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.artifact.ArtifactFileMetadata;
import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.delegate.task.shell.ScriptType;
import io.harness.exception.WingsException;
import io.harness.rule.Owner;
import io.harness.security.encryption.EncryptedDataDetail;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.annotation.EncryptableSetting;
import software.wings.beans.AwsConfig;
import software.wings.beans.BambooConfig;
import software.wings.beans.HostConnectionAttributes.AccessType;
import software.wings.beans.HostConnectionAttributes.Builder;
import software.wings.beans.JenkinsConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.artifact.Artifact.ArtifactMetadataKeys;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.beans.artifact.ArtifactStreamType;
import software.wings.beans.artifact.AzureArtifactsArtifactStream.ProtocolType;
import software.wings.beans.config.ArtifactoryConfig;
import software.wings.beans.config.NexusConfig;
import software.wings.beans.infrastructure.Host;
import software.wings.beans.settings.azureartifacts.AzureArtifactsConfig;
import software.wings.beans.settings.azureartifacts.AzureArtifactsPATConfig;
import software.wings.core.BaseScriptExecutor;
import software.wings.delegatetasks.DelegateLogService;
import software.wings.helpers.ext.azure.devops.AzureArtifactsPackageFileInfo;
import software.wings.helpers.ext.azure.devops.AzureArtifactsService;
import software.wings.service.impl.AwsHelperService;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.utils.WingsTestConstants;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@RunWith(JUnitParamsRunner.class)
public class DownloadArtifactCommandUnitTest extends WingsBaseTest {
  private static final String JENKINS_ARTIFACT_URL_1 =
      "http://localhost:8089/job/scheduler-svn/75/artifact/build/libs/docker-scheduler-1.0-SNAPSHOT-all.jar";
  private static final String JENKINS_ARTIFACT_URL_2 =
      "http://localhost:8089/job/scheduler-svn/75/artifact/build/libs/docker-scheduler-1.0-SNAPSHOT-sources.jar";
  private static final String JENKINS_ARTIFACT_FILENAME_1 = "docker-scheduler-1.0-SNAPSHOT-all.jar";
  private static final String JENKINS_ARTIFACT_FILENAME_2 = "docker-scheduler-1.0-SNAPSHOT-sources.jar";
  @InjectMocks private DownloadArtifactCommandUnit downloadArtifactCommandUnit = new DownloadArtifactCommandUnit();
  @Mock private BaseScriptExecutor executor;
  @Mock private EncryptionService encryptionService;
  @Mock private AwsHelperService awsHelperService;
  @Mock private AzureArtifactsService azureArtifactsService;
  @Mock DelegateLogService logService;
  private SettingAttribute awsSetting =
      aSettingAttribute()
          .withUuid(SETTING_ID)
          .withValue(AwsConfig.builder().secretKey(SECRET_KEY).accessKey(ACCESS_KEY).build())
          .build();
  private SettingAttribute hostConnectionAttributes = aSettingAttribute()
                                                          .withValue(Builder.aHostConnectionAttributes()
                                                                         .withAccessType(AccessType.USER_PASSWORD)
                                                                         .withAccountId(WingsTestConstants.ACCOUNT_ID)
                                                                         .build())
                                                          .build();
  private ArtifactStreamAttributes artifactStreamAttributesForAmazonS3 =
      ArtifactStreamAttributes.builder()
          .artifactStreamType(ArtifactStreamType.AMAZON_S3.name())
          .metadataOnly(true)
          .metadata(mockMetadata(ArtifactStreamType.AMAZON_S3))
          .serverSetting(awsSetting)
          .artifactServerEncryptedDataDetails(Collections.emptyList())
          .build();
  private Host host = Host.Builder.aHost().withPublicDns(WingsTestConstants.PUBLIC_DNS).build();

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

  private ArtifactStreamAttributes artifactStreamAttributesForArtifactory =
      ArtifactStreamAttributes.builder()
          .artifactStreamType(ArtifactStreamType.ARTIFACTORY.name())
          .metadataOnly(true)
          .metadata(mockMetadata(ArtifactStreamType.ARTIFACTORY))
          .serverSetting(artifactorySetting)
          .artifactStreamId(ARTIFACT_STREAM_ID_ARTIFACTORY)
          .artifactServerEncryptedDataDetails(Collections.emptyList())
          .build();
  private SettingAttribute artifactoryAnonSetting =
      aSettingAttribute()
          .withUuid(SETTING_ID)
          .withValue(ArtifactoryConfig.builder().artifactoryUrl(WingsTestConstants.ARTIFACTORY_URL).build())
          .build();
  private ArtifactStreamAttributes streamAttributesAnon =
      ArtifactStreamAttributes.builder()
          .artifactStreamType(ArtifactStreamType.ARTIFACTORY.name())
          .metadataOnly(true)
          .metadata(mockMetadata(ArtifactStreamType.ARTIFACTORY))
          .serverSetting(artifactoryAnonSetting)
          .artifactStreamId(ARTIFACT_STREAM_ID_ARTIFACTORY)
          .artifactServerEncryptedDataDetails(Collections.emptyList())
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

  private SettingAttribute nexusSetting = aSettingAttribute()
                                              .withUuid(SETTING_ID)
                                              .withValue(NexusConfig.builder()
                                                             .nexusUrl(WingsTestConstants.HARNESS_NEXUS)
                                                             .username("admin")
                                                             .password("dummy123!".toCharArray())
                                                             .build())
                                              .build();

  private SettingAttribute nexusSettingAnon =
      aSettingAttribute()
          .withUuid(SETTING_ID)
          .withValue(NexusConfig.builder().nexusUrl(WingsTestConstants.HARNESS_NEXUS_THREE).build())
          .build();

  private SettingAttribute jenkinsSetting = aSettingAttribute()
                                                .withUuid(SETTING_ID)
                                                .withValue(JenkinsConfig.builder()
                                                               .jenkinsUrl(WingsTestConstants.HARNESS_JENKINS)
                                                               .username("admin")
                                                               .password("dummy123!".toCharArray())
                                                               .build())
                                                .build();

  private SettingAttribute bambooSetting = aSettingAttribute()
                                               .withUuid(SETTING_ID)
                                               .withValue(BambooConfig.builder()
                                                              .bambooUrl("http://localhost:9095/")
                                                              .username("admin")
                                                              .password("admin".toCharArray())
                                                              .build())
                                               .build();

  private ArtifactStreamAttributes nexus2MavenStreamAttributes =
      ArtifactStreamAttributes.builder()
          .artifactStreamType(ArtifactStreamType.NEXUS.name())
          .metadataOnly(true)
          .artifactFileMetadata(asList(
              builder()
                  .fileName("todolist-7.0.war")
                  .url(
                      "https://nexus2-cdteam.harness.io/service/local/artifact/maven/content?r=releases&g=io.harness.test&a=todolist&v=7.0&p=war&e=war")
                  .build()))
          .serverSetting(nexusSetting)
          .artifactStreamId(ARTIFACT_STREAM_ID)
          .jobName("releases")
          .groupId("io.harness.test")
          .artifactName("todolist")
          .artifactServerEncryptedDataDetails(Collections.emptyList())
          .build();

  private ArtifactStreamAttributes nexus2MavenStreamAttributesAnon =
      ArtifactStreamAttributes.builder()
          .artifactStreamType(ArtifactStreamType.NEXUS.name())
          .metadataOnly(true)
          .artifactFileMetadata(asList(
              builder()
                  .fileName("todolist-7.0.war")
                  .url(
                      "https://nexus2-cdteam.harness.io/service/local/artifact/maven/content?r=releases&g=io.harness.test&a=todolist&v=7.0&p=war&e=war")
                  .build(),
              builder()
                  .fileName("todolist-7.0.tar")
                  .url(
                      "https://nexus2-cdteam.harness.io/service/local/artifact/maven/content?r=releases&g=io.harness.test&a=todolist&v=7.0&p=war&e=tar")
                  .build()))
          .serverSetting(nexusSettingAnon)
          .artifactStreamId(ARTIFACT_STREAM_ID)
          .jobName("releases")
          .groupId("io.harness.test")
          .artifactName("todolist")
          .artifactServerEncryptedDataDetails(Collections.emptyList())
          .build();

  private ArtifactStreamAttributes jenkinsArtifactStreamAttributes =
      ArtifactStreamAttributes.builder()
          .artifactStreamType(ArtifactStreamType.JENKINS.name())
          .metadataOnly(true)
          .artifactFileMetadata(
              asList(builder().fileName(JENKINS_ARTIFACT_FILENAME_1).url(JENKINS_ARTIFACT_URL_1).build(),
                  builder().fileName(JENKINS_ARTIFACT_FILENAME_2).url(JENKINS_ARTIFACT_URL_2).build()))
          .serverSetting(jenkinsSetting)
          .artifactStreamId(ARTIFACT_STREAM_ID)
          .jobName("scheduler")
          .artifactPaths(asList("build/libs/docker-scheduler-1.0-SNAPSHOT-all.jar",
              "build/libs/docker-scheduler-1.0-SNAPSHOT-sources.jar"))
          .artifactServerEncryptedDataDetails(Collections.emptyList())
          .build();

  private ArtifactStreamAttributes bambooStreamAttributes =
      ArtifactStreamAttributes.builder()
          .artifactStreamType(ArtifactStreamType.BAMBOO.name())
          .jobName("TOD-TOD")
          .artifactPaths(Arrays.asList("artifacts/todolist.tar"))
          .artifactFileMetadata(
              Arrays.asList(ArtifactFileMetadata.builder()
                                .fileName("todolist.tar")
                                .url("http://localhost:9095/artifact/TOD-TOD/JOB1/build-11/artifacts/todolist.tar")
                                .build(),
                  ArtifactFileMetadata.builder()
                      .fileName("todolist.war")
                      .url("http://localhost:9095/artifact/TOD-TOD/JOB1/build-11/artifacts/todolist.war")
                      .build()))
          .serverSetting(bambooSetting)
          .artifactServerEncryptedDataDetails(Collections.emptyList())
          .artifactStreamId(ARTIFACT_STREAM_ID)
          .build();

  @InjectMocks
  private ShellCommandExecutionContext amazonS3Context =
      new ShellCommandExecutionContext(aCommandExecutionContext()
                                           .withArtifactStreamAttributes(artifactStreamAttributesForAmazonS3)
                                           .withMetadata(mockMetadata(ArtifactStreamType.AMAZON_S3))
                                           .withHostConnectionAttributes(hostConnectionAttributes)
                                           .withAppId(WingsTestConstants.APP_ID)
                                           .withActivityId(ACTIVITY_ID)
                                           .withHost(host)
                                           .build());

  @InjectMocks
  private ShellCommandExecutionContext amazonS3ContextFolder =
      new ShellCommandExecutionContext(aCommandExecutionContext()
                                           .withArtifactStreamAttributes(artifactStreamAttributesForAmazonS3)
                                           .withMetadata(mockMetadataForS3Folder())
                                           .withHostConnectionAttributes(hostConnectionAttributes)
                                           .withAppId(WingsTestConstants.APP_ID)
                                           .withActivityId(ACTIVITY_ID)
                                           .withHost(host)
                                           .build());

  @InjectMocks
  private ShellCommandExecutionContext artifactoryContext =
      new ShellCommandExecutionContext(aCommandExecutionContext()
                                           .withArtifactStreamAttributes(artifactStreamAttributesForArtifactory)
                                           .withMetadata(mockMetadata(ArtifactStreamType.ARTIFACTORY))
                                           .withHostConnectionAttributes(hostConnectionAttributes)
                                           .withAppId(WingsTestConstants.APP_ID)
                                           .withActivityId(ACTIVITY_ID)
                                           .withHost(host)
                                           .build());

  @InjectMocks
  ShellCommandExecutionContext artifactoryContextAnon =
      new ShellCommandExecutionContext(aCommandExecutionContext()
                                           .withArtifactStreamAttributes(streamAttributesAnon)
                                           .withMetadata(mockMetadata(ArtifactStreamType.ARTIFACTORY))
                                           .withHostConnectionAttributes(hostConnectionAttributes)
                                           .withAppId(WingsTestConstants.APP_ID)
                                           .withActivityId(ACTIVITY_ID)
                                           .withHost(host)
                                           .build());

  @InjectMocks
  ShellCommandExecutionContext azureArtifactsContext =
      new ShellCommandExecutionContext(aCommandExecutionContext()
                                           .withArtifactStreamAttributes(artifactStreamAttributesForAzureArtifacts)
                                           .withMetadata(mockMetadata(ArtifactStreamType.AZURE_ARTIFACTS))
                                           .withHostConnectionAttributes(hostConnectionAttributes)
                                           .withAppId(WingsTestConstants.APP_ID)
                                           .withActivityId(ACTIVITY_ID)
                                           .withHost(host)
                                           .build());

  @InjectMocks
  ShellCommandExecutionContext nexusContextMaven =
      new ShellCommandExecutionContext(aCommandExecutionContext()
                                           .withArtifactStreamAttributes(nexus2MavenStreamAttributes)
                                           .withMetadata(mockMetadata(ArtifactStreamType.NEXUS))
                                           .withHostConnectionAttributes(hostConnectionAttributes)
                                           .withAppId(WingsTestConstants.APP_ID)
                                           .withActivityId(ACTIVITY_ID)
                                           .withHost(host)
                                           .build());

  @InjectMocks
  ShellCommandExecutionContext nexusContextMavenAnon =
      new ShellCommandExecutionContext(aCommandExecutionContext()
                                           .withArtifactStreamAttributes(nexus2MavenStreamAttributesAnon)
                                           .withMetadata(mockMetadata(ArtifactStreamType.NEXUS))
                                           .withHostConnectionAttributes(hostConnectionAttributes)
                                           .withAppId(WingsTestConstants.APP_ID)
                                           .withActivityId(ACTIVITY_ID)
                                           .withHost(host)
                                           .build());

  @InjectMocks
  ShellCommandExecutionContext bambooContext =
      new ShellCommandExecutionContext(aCommandExecutionContext()
                                           .withArtifactStreamAttributes(bambooStreamAttributes)
                                           .withMetadata(mockMetadata(ArtifactStreamType.BAMBOO))
                                           .withHostConnectionAttributes(hostConnectionAttributes)
                                           .withAppId(WingsTestConstants.APP_ID)
                                           .withActivityId(ACTIVITY_ID)
                                           .withHost(host)
                                           .build());

  @InjectMocks
  ShellCommandExecutionContext jenkinsContext =
      new ShellCommandExecutionContext(aCommandExecutionContext()
                                           .withArtifactStreamAttributes(jenkinsArtifactStreamAttributes)
                                           .withMetadata(mockMetadata(ArtifactStreamType.JENKINS))
                                           .withHostConnectionAttributes(hostConnectionAttributes)
                                           .withAppId(WingsTestConstants.APP_ID)
                                           .withActivityId(ACTIVITY_ID)
                                           .withHost(host)
                                           .build());

  @Test
  @Owner(developers = UNKNOWN)
  @Category(UnitTests.class)
  @Parameters(method = "getData")
  public void testShouldDownloadArtifactThroughPowerShell(ArtifactStreamType artifactStreamType) {
    ShellCommandExecutionContext context = null;
    switch (artifactStreamType) {
      case AMAZON_S3:
        context = amazonS3Context;
        break;
      case ARTIFACTORY:
        context = artifactoryContext;
        break;
      case AZURE_ARTIFACTS:
        context = azureArtifactsContext;
        break;
      default:
        break;
    }
    downloadArtifactCommandUnit.setScriptType(ScriptType.POWERSHELL);
    executeDownloadCommandUnit(context);
  }

  private void executeDownloadCommandUnit(ShellCommandExecutionContext context) {
    downloadArtifactCommandUnit.setCommandPath(WingsTestConstants.DESTINATION_DIR_PATH);
    when(encryptionService.decrypt(any(EncryptableSetting.class), anyListOf(EncryptedDataDetail.class)))
        .thenReturn((EncryptableSetting) hostConnectionAttributes.getValue());
    when(awsHelperService.getBucketRegion(any(AwsConfig.class), anyListOf(EncryptedDataDetail.class), anyString()))
        .thenReturn("us-west-1");
    when(azureArtifactsService.listFiles(
             any(AzureArtifactsConfig.class), anyListOf(EncryptedDataDetail.class), any(), anyMap(), eq(true)))
        .thenReturn(Collections.singletonList(new AzureArtifactsPackageFileInfo(ARTIFACT_FILE_NAME, -1)));
    when(executor.executeCommandString(anyString(), anyBoolean())).thenReturn(CommandExecutionStatus.SUCCESS);
    CommandExecutionStatus status = downloadArtifactCommandUnit.executeInternal(context);
    assertThat(status).isEqualTo(CommandExecutionStatus.SUCCESS);
  }

  @Test
  @Owner(developers = UNKNOWN)
  @Category(UnitTests.class)
  @Parameters(method = "getData")
  @TestCaseName("{method}-{0}")
  public void testShouldDownloadThroughBash(ArtifactStreamType artifactStreamType) {
    ShellCommandExecutionContext context =
        new ShellCommandExecutionContext(CommandExecutionContext.Builder.aCommandExecutionContext().build());
    switch (artifactStreamType) {
      case AMAZON_S3:
        context = amazonS3Context;
        break;
      case ARTIFACTORY:
        context = artifactoryContext;
        break;
      case AZURE_ARTIFACTS:
        context = azureArtifactsContext;
        break;
      default:
        break;
    }
    downloadArtifactCommandUnit.setScriptType(ScriptType.BASH);
    executeDownloadCommandUnit(context);
  }

  @Test
  @Owner(developers = UNKNOWN)
  @Category(UnitTests.class)
  @Parameters(method = "getScriptType")
  public void shouldDownloadFromArtifactoryAsAnonymous(ScriptType scriptType) {
    downloadArtifactCommandUnit.setScriptType(scriptType);
    downloadArtifactCommandUnit.setCommandPath(WingsTestConstants.DESTINATION_DIR_PATH);
    when(encryptionService.decrypt(any(EncryptableSetting.class), anyListOf(EncryptedDataDetail.class)))
        .thenReturn((EncryptableSetting) hostConnectionAttributes.getValue());
    when(executor.executeCommandString(anyString(), anyBoolean())).thenReturn(CommandExecutionStatus.SUCCESS);
    CommandExecutionStatus status = downloadArtifactCommandUnit.executeInternal(artifactoryContextAnon);
    assertThat(status).isEqualTo(CommandExecutionStatus.SUCCESS);
  }

  @Test(expected = WingsException.class)
  @Owner(developers = AADITI)
  @Category(UnitTests.class)
  public void shouldFailWithInvalidArtifactDownloadDir() {
    downloadArtifactCommandUnit.setScriptType(ScriptType.BASH);
    CommandExecutionStatus status = downloadArtifactCommandUnit.executeInternal(artifactoryContextAnon);
    assertThat(status).isEqualTo(CommandExecutionStatus.FAILURE);
  }

  @Test
  @Owner(developers = AADITI)
  @Category(UnitTests.class)
  @Parameters(method = "getNexus2MavenData")
  public void shouldDownloadFromNexus2Maven(ScriptType scriptType, String command) {
    nexusContextMaven.setExecutor(executor);
    downloadArtifactCommandUnit.setScriptType(scriptType);
    downloadArtifactCommandUnit.setCommandPath(WingsTestConstants.DESTINATION_DIR_PATH);
    when(encryptionService.decrypt(any(EncryptableSetting.class), anyListOf(EncryptedDataDetail.class)))
        .thenReturn((EncryptableSetting) hostConnectionAttributes.getValue());
    downloadArtifactCommandUnit.executeInternal(nexusContextMaven);
    ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
    verify(executor).executeCommandString(argument.capture(), anyBoolean());
    assertThat(argument.getValue()).isEqualTo(command);
  }

  @Test
  @Owner(developers = AADITI)
  @Category(UnitTests.class)
  @Parameters(method = "getNexus2MavenDataAnon")
  public void shouldDownloadFromNexus2MavenAnon(ScriptType scriptType, String command) {
    nexusContextMavenAnon.setExecutor(executor);
    downloadArtifactCommandUnit.setScriptType(scriptType);
    downloadArtifactCommandUnit.setCommandPath(WingsTestConstants.DESTINATION_DIR_PATH);
    when(encryptionService.decrypt(any(EncryptableSetting.class), anyListOf(EncryptedDataDetail.class)))
        .thenReturn((EncryptableSetting) hostConnectionAttributes.getValue());
    downloadArtifactCommandUnit.executeInternal(nexusContextMavenAnon);
    ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
    verify(executor).executeCommandString(argument.capture(), anyBoolean());
    assertThat(argument.getValue()).isEqualTo(command);
  }

  @Test
  @Owner(developers = AADITI)
  @Category(UnitTests.class)
  @Parameters(method = "getJenkinsData")
  public void shouldDownloadFromJenkins(ScriptType scriptType, String command) {
    jenkinsContext.setExecutor(executor);
    downloadArtifactCommandUnit.setScriptType(scriptType);
    downloadArtifactCommandUnit.setCommandPath(WingsTestConstants.DESTINATION_DIR_PATH);
    when(encryptionService.decrypt(any(EncryptableSetting.class), anyListOf(EncryptedDataDetail.class)))
        .thenReturn((EncryptableSetting) hostConnectionAttributes.getValue());
    downloadArtifactCommandUnit.executeInternal(jenkinsContext);
    ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
    verify(executor).executeCommandString(argument.capture(), anyBoolean());
    assertThat(argument.getValue()).isEqualTo(command);
  }

  @Test
  @Owner(developers = AADITI)
  @Category(UnitTests.class)
  @Parameters(method = "getBambooData")
  public void shouldDownloadFromBamboo(ScriptType scriptType, String command) {
    bambooContext.setExecutor(executor);
    downloadArtifactCommandUnit.setScriptType(scriptType);
    downloadArtifactCommandUnit.setCommandPath(WingsTestConstants.DESTINATION_DIR_PATH);
    when(encryptionService.decrypt(any(EncryptableSetting.class), anyListOf(EncryptedDataDetail.class)))
        .thenReturn((EncryptableSetting) hostConnectionAttributes.getValue());
    downloadArtifactCommandUnit.executeInternal(bambooContext);
    ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
    verify(executor).executeCommandString(argument.capture(), anyBoolean());
    assertThat(argument.getValue()).isEqualTo(command);
  }

  @Test
  @Owner(developers = AADITI)
  @Category(UnitTests.class)
  @Parameters(method = "getS3Data")
  public void shouldDownloadFromS3(ShellCommandExecutionContext context, String command) {
    context.setExecutor(executor);
    downloadArtifactCommandUnit.setScriptType(ScriptType.POWERSHELL);
    downloadArtifactCommandUnit.setCommandPath(WingsTestConstants.DESTINATION_DIR_PATH);
    when(encryptionService.decrypt(any(EncryptableSetting.class), anyListOf(EncryptedDataDetail.class)))
        .thenReturn((EncryptableSetting) hostConnectionAttributes.getValue());
    when(awsHelperService.getBucketRegion(any(AwsConfig.class), anyListOf(EncryptedDataDetail.class), anyString()))
        .thenReturn("us-west-1");
    downloadArtifactCommandUnit.executeInternal(context);
    ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
    verify(executor).executeCommandString(argument.capture(), anyBoolean());
    assertThat(argument.getValue()).endsWith(command);
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
        break;
      case AZURE_ARTIFACTS:
        map.put(ArtifactMetadataKeys.version, BUILD_NO);
        map.put(ArtifactMetadataKeys.buildNo, BUILD_NO);
        break;
      default:
        break;
    }
    return map;
  }

  private Map<String, String> mockMetadataForS3Folder() {
    Map<String, String> map = new HashMap<>();
    map.put(ArtifactMetadataKeys.bucketName, BUCKET_NAME);
    map.put(ArtifactMetadataKeys.artifactFileName, "test1/test2/todolist.zip");
    map.put(ArtifactMetadataKeys.artifactPath, "test1/test2/todolist.zip");
    map.put(ArtifactMetadataKeys.buildNo, BUILD_NO);
    map.put(ArtifactMetadataKeys.artifactFileSize, String.valueOf(WingsTestConstants.ARTIFACT_FILE_SIZE));
    map.put(ArtifactMetadataKeys.key, ACCESS_KEY);
    map.put(ArtifactMetadataKeys.url, S3_URL);
    return map;
  }

  private Object[][] getData() {
    amazonS3Context.setExecutor(executor);
    artifactoryContext.setExecutor(executor);
    azureArtifactsContext.setExecutor(executor);
    return new Object[][] {
        {ArtifactStreamType.AMAZON_S3}, {ArtifactStreamType.ARTIFACTORY}, {ArtifactStreamType.AZURE_ARTIFACTS}};
  }

  private Object[][] getScriptType() {
    amazonS3Context.setExecutor(executor);
    artifactoryContext.setExecutor(executor);
    azureArtifactsContext.setExecutor(executor);
    return new Object[][] {{ScriptType.BASH}, {ScriptType.POWERSHELL}};
  }

  private Object[][] getNexus2MavenData() {
    return new Object[][] {
        {ScriptType.BASH,
            "curl --progress-bar -H \"Authorization: Basic YWRtaW46ZHVtbXkxMjMh\" -X GET \"https://nexus2-cdteam.harness.io/service/local/artifact/maven/content?r=releases&g=io.harness.test&a=todolist&v=7.0&p=war&e=war\" -o \"DESTINATION_DIR_PATH/todolist-7.0.war\"\n"},
        {ScriptType.POWERSHELL,
            "$Headers = @{\n"
                + "    Authorization = \"Basic YWRtaW46ZHVtbXkxMjMh\"\n"
                + "}\n"
                + " [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12\n"
                + " Invoke-WebRequest -Uri \"https://nexus2-cdteam.harness.io/service/local/artifact/maven/content?r=releases&g=io.harness.test&a=todolist&v=7.0&p=war&e=war\" -Headers $Headers -OutFile \"DESTINATION_DIR_PATH\\todolist-7.0.war\""}};
  }

  private Object[][] getNexus2MavenDataAnon() {
    return new Object[][] {
        {ScriptType.BASH,
            "curl --progress-bar -X GET \"https://nexus2-cdteam.harness.io/service/local/artifact/maven/content?r=releases&g=io.harness.test&a=todolist&v=7.0&p=war&e=war\" -o \"DESTINATION_DIR_PATH/todolist-7.0.war\"\n"
                + "curl --progress-bar -X GET \"https://nexus2-cdteam.harness.io/service/local/artifact/maven/content?r=releases&g=io.harness.test&a=todolist&v=7.0&p=war&e=tar\" -o \"DESTINATION_DIR_PATH/todolist-7.0.tar\"\n"},
        {ScriptType.POWERSHELL,
            "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12\n"
                + " Invoke-WebRequest -Uri \"https://nexus2-cdteam.harness.io/service/local/artifact/maven/content?r=releases&g=io.harness.test&a=todolist&v=7.0&p=war&e=war\" -OutFile \"DESTINATION_DIR_PATH\\todolist-7.0.war\"\n"
                + " Invoke-WebRequest -Uri \"https://nexus2-cdteam.harness.io/service/local/artifact/maven/content?r=releases&g=io.harness.test&a=todolist&v=7.0&p=war&e=tar\" -OutFile \"DESTINATION_DIR_PATH\\todolist-7.0.tar\""}};
  }

  private Object[][] getJenkinsData() {
    return new Object[][] {
        {ScriptType.BASH,
            "curl --progress-bar -H \"Authorization: Basic YWRtaW46ZHVtbXkxMjMh\" -X GET \"" + JENKINS_ARTIFACT_URL_1
                + "\" -o \"DESTINATION_DIR_PATH/" + JENKINS_ARTIFACT_FILENAME_1 + "\"\n"
                + "curl --progress-bar -H \"Authorization: Basic YWRtaW46ZHVtbXkxMjMh\" -X GET \""
                + JENKINS_ARTIFACT_URL_2 + "\" -o \"DESTINATION_DIR_PATH/" + JENKINS_ARTIFACT_FILENAME_2 + "\"\n"},
        {ScriptType.POWERSHELL,
            "$webClient = New-Object System.Net.WebClient \n"
                + "$webClient.Headers[[System.Net.HttpRequestHeader]::Authorization] = \"Basic YWRtaW46ZHVtbXkxMjMh\";\n"
                + "$url = \"" + JENKINS_ARTIFACT_URL_1 + "\" \n"
                + "$localfilename = \"DESTINATION_DIR_PATH\\" + JENKINS_ARTIFACT_FILENAME_1 + "\" \n"
                + "$webClient.DownloadFile($url, $localfilename) \n"
                + "$url = \"" + JENKINS_ARTIFACT_URL_2 + "\" \n"
                + "$localfilename = \"DESTINATION_DIR_PATH\\" + JENKINS_ARTIFACT_FILENAME_2 + "\" \n"
                + "$webClient.DownloadFile($url, $localfilename) \n"}};
  }

  private Object[][] getBambooData() {
    return new Object[][] {
        {ScriptType.BASH,
            "curl --progress-bar -H \"Authorization: Basic YWRtaW46YWRtaW4=\" -X GET \"http://localhost:9095/artifact/TOD-TOD/JOB1/build-11/artifacts/todolist.tar\" -o \"DESTINATION_DIR_PATH/todolist.tar\"\n"
                + "curl --progress-bar -H \"Authorization: Basic YWRtaW46YWRtaW4=\" -X GET \"http://localhost:9095/artifact/TOD-TOD/JOB1/build-11/artifacts/todolist.war\" -o \"DESTINATION_DIR_PATH/todolist.war\"\n"},
        {ScriptType.POWERSHELL,
            "$Headers = @{\n"
                + "    Authorization = \"Basic YWRtaW46YWRtaW4=\"\n"
                + "}\n"
                + " [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12\n"
                + " Invoke-WebRequest -Uri \"http://localhost:9095/artifact/TOD-TOD/JOB1/build-11/artifacts/todolist.tar\" -Headers $Headers -OutFile \"DESTINATION_DIR_PATH\\todolist.tar\"\n"
                + " Invoke-WebRequest -Uri \"http://localhost:9095/artifact/TOD-TOD/JOB1/build-11/artifacts/todolist.war\" -Headers $Headers -OutFile \"DESTINATION_DIR_PATH\\todolist.war\""}};
  }

  private Object[][] getS3Data() {
    return new Object[][] {
        {amazonS3Context,
            " Invoke-WebRequest -Uri \"https://BUCKET_NAME.s3-us-west-1.amazonaws.com/ARTIFACT_PATH\" -Headers $Headers -OutFile (New-Item -Path \"DESTINATION_DIR_PATH\\ARTIFACT_FILE_NAME\" -Force)"},
        {amazonS3ContextFolder,
            " Invoke-WebRequest -Uri \"https://BUCKET_NAME.s3-us-west-1.amazonaws.com/test1/test2/todolist.zip\" -Headers $Headers -OutFile (New-Item -Path \"DESTINATION_DIR_PATH\\test1/test2/todolist.zip\" -Force)"}};
  }
}
