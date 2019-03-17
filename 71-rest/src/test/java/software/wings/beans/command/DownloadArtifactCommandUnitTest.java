package software.wings.beans.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.beans.command.CommandExecutionContext.Builder.aCommandExecutionContext;
import static software.wings.utils.WingsTestConstants.ACCESS_KEY;
import static software.wings.utils.WingsTestConstants.ACTIVITY_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_FILE_NAME;
import static software.wings.utils.WingsTestConstants.ARTIFACT_PATH;
import static software.wings.utils.WingsTestConstants.ARTIFACT_STREAM_ID_ARTIFACTORY;
import static software.wings.utils.WingsTestConstants.BUCKET_NAME;
import static software.wings.utils.WingsTestConstants.BUILD_NO;
import static software.wings.utils.WingsTestConstants.S3_URL;
import static software.wings.utils.WingsTestConstants.SECRET_KEY;
import static software.wings.utils.WingsTestConstants.SETTING_ID;

import io.harness.category.element.UnitTests;
import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.delegate.task.shell.ScriptType;
import io.harness.exception.WingsException;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.annotation.EncryptableSetting;
import software.wings.beans.AwsConfig;
import software.wings.beans.HostConnectionAttributes.AccessType;
import software.wings.beans.HostConnectionAttributes.Builder;
import software.wings.beans.SettingAttribute;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.beans.artifact.ArtifactStreamType;
import software.wings.beans.config.ArtifactoryConfig;
import software.wings.beans.infrastructure.Host;
import software.wings.common.Constants;
import software.wings.core.BaseExecutor;
import software.wings.delegatetasks.DelegateLogService;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.AwsHelperService;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.utils.WingsTestConstants;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@RunWith(JUnitParamsRunner.class)
public class DownloadArtifactCommandUnitTest extends WingsBaseTest {
  @InjectMocks private DownloadArtifactCommandUnit downloadArtifactCommandUnit = new DownloadArtifactCommandUnit();
  @Mock private BaseExecutor executor;
  @Mock private EncryptionService encryptionService;
  @Mock private AwsHelperService awsHelperService;
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
  private ArtifactStreamAttributes artifactStreamAttributesForArtifactory =
      ArtifactStreamAttributes.builder()
          .artifactStreamType(ArtifactStreamType.ARTIFACTORY.name())
          .metadataOnly(true)
          .metadata(mockMetadata(ArtifactStreamType.ARTIFACTORY))
          .serverSetting(artifactorySetting)
          .artifactStreamId(ARTIFACT_STREAM_ID_ARTIFACTORY)
          .artifactServerEncryptedDataDetails(Collections.emptyList())
          .build();
  SettingAttribute artifactoryAnonSetting =
      aSettingAttribute()
          .withUuid(SETTING_ID)
          .withValue(ArtifactoryConfig.builder().artifactoryUrl(WingsTestConstants.ARTIFACTORY_URL).build())
          .build();
  ArtifactStreamAttributes streamAttributesAnon = ArtifactStreamAttributes.builder()
                                                      .artifactStreamType(ArtifactStreamType.ARTIFACTORY.name())
                                                      .metadataOnly(true)
                                                      .metadata(mockMetadata(ArtifactStreamType.ARTIFACTORY))
                                                      .serverSetting(artifactoryAnonSetting)
                                                      .artifactStreamId(ARTIFACT_STREAM_ID_ARTIFACTORY)
                                                      .artifactServerEncryptedDataDetails(Collections.emptyList())
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

  @Test
  @Category(UnitTests.class)
  @Parameters(method = "getData")
  @TestCaseName("{method}-{0}")
  public void testShouldDownloadArtifactThroughPowerShell(ArtifactStreamType artifactStreamType) {
    ShellCommandExecutionContext context = null;
    switch (artifactStreamType) {
      case AMAZON_S3:
        context = amazonS3Context;
        break;
      case ARTIFACTORY:
        context = artifactoryContext;
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
    when(executor.executeCommandString(anyString(), anyBoolean())).thenReturn(CommandExecutionStatus.SUCCESS);
    CommandExecutionStatus status = downloadArtifactCommandUnit.executeInternal(context);
    assertThat(status).isEqualTo(CommandExecutionStatus.SUCCESS);
  }

  @Test
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
      default:
        break;
    }
    downloadArtifactCommandUnit.setScriptType(ScriptType.BASH);
    executeDownloadCommandUnit(context);
  }

  @Test
  @Category(UnitTests.class)
  @Parameters(method = "getScriptType")
  @TestCaseName("{method}-{0}")
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
  @Category(UnitTests.class)
  public void shouldFailWithInvalidArtifactDownloadDir() {
    downloadArtifactCommandUnit.setScriptType(ScriptType.BASH);
    CommandExecutionStatus status = downloadArtifactCommandUnit.executeInternal(artifactoryContextAnon);
    assertThat(status).isEqualTo(CommandExecutionStatus.FAILURE);
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
        break;
      default:
        break;
    }
    return map;
  }

  private Object[][] getData() {
    amazonS3Context.setExecutor(executor);
    artifactoryContext.setExecutor(executor);
    return new Object[][] {{ArtifactStreamType.AMAZON_S3}, {ArtifactStreamType.ARTIFACTORY}};
  }

  private Object[][] getScriptType() {
    amazonS3Context.setExecutor(executor);
    artifactoryContext.setExecutor(executor);
    return new Object[][] {{ScriptType.BASH}, {ScriptType.POWERSHELL}};
  }
}
