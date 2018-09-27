package software.wings.beans.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.beans.artifact.ArtifactStreamAttributes.Builder.anArtifactStreamAttributes;
import static software.wings.beans.command.CommandExecutionContext.Builder.aCommandExecutionContext;
import static software.wings.utils.WingsTestConstants.ACCESS_KEY;
import static software.wings.utils.WingsTestConstants.ACTIVITY_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_FILE_NAME;
import static software.wings.utils.WingsTestConstants.ARTIFACT_PATH;
import static software.wings.utils.WingsTestConstants.BUCKET_NAME;
import static software.wings.utils.WingsTestConstants.BUILD_NO;
import static software.wings.utils.WingsTestConstants.S3_URL;
import static software.wings.utils.WingsTestConstants.SECRET_KEY;
import static software.wings.utils.WingsTestConstants.SETTING_ID;

import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.annotation.Encryptable;
import software.wings.api.ScriptType;
import software.wings.beans.AwsConfig;
import software.wings.beans.HostConnectionAttributes.AccessType;
import software.wings.beans.HostConnectionAttributes.Builder;
import software.wings.beans.SettingAttribute;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.beans.artifact.ArtifactStreamType;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
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

public class DownloadArtifactCommandUnitTest extends WingsBaseTest {
  @InjectMocks private DownloadArtifactCommandUnit downloadArtifactCommandUnit = new DownloadArtifactCommandUnit();
  @Mock BaseExecutor baseExecutor;
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
  private Map<String, String> map = mockMetadata();
  private ArtifactStreamAttributes artifactStreamAttributes =
      anArtifactStreamAttributes()
          .withArtifactStreamType(ArtifactStreamType.AMAZON_S3.name())
          .withMetadataOnly(true)
          .withMetadata(map)
          .withServerSetting(awsSetting)
          .withArtifactServerEncryptedDataDetails(Collections.emptyList())
          .build();
  private Host host = Host.Builder.aHost().withPublicDns(WingsTestConstants.PUBLIC_DNS).build();
  @InjectMocks
  private ShellCommandExecutionContext context =
      new ShellCommandExecutionContext(aCommandExecutionContext()
                                           .withArtifactStreamAttributes(artifactStreamAttributes)
                                           .withMetadata(map)
                                           .withHostConnectionAttributes(hostConnectionAttributes)
                                           .withAppId(WingsTestConstants.APP_ID)
                                           .withActivityId(ACTIVITY_ID)
                                           .withHost(host)
                                           .build());

  @Test
  public void testShouldDownloadAmazonS3ArtifactThroughPowerShell() {
    downloadArtifactCommandUnit.setScriptType(ScriptType.POWERSHELL);
    downloadArtifactCommandUnit.setCommandPath(WingsTestConstants.DESTINATION_DIR_PATH);
    when(encryptionService.decrypt(any(Encryptable.class), anyListOf(EncryptedDataDetail.class)))
        .thenReturn((Encryptable) hostConnectionAttributes.getValue());
    when(awsHelperService.getBucketRegion(any(AwsConfig.class), anyListOf(EncryptedDataDetail.class), anyString()))
        .thenReturn("us-west-1");
    when(baseExecutor.executeCommandString(anyString(), anyBoolean())).thenReturn(CommandExecutionStatus.SUCCESS);
    CommandExecutionStatus status = downloadArtifactCommandUnit.executeInternal(context);
    assertThat(status).isEqualTo(CommandExecutionStatus.SUCCESS);
  }

  @Test
  public void testShouldDownloadAmazonS3ArtifactThroughBash() {
    downloadArtifactCommandUnit.setScriptType(ScriptType.BASH);
    downloadArtifactCommandUnit.setCommandPath(WingsTestConstants.DESTINATION_DIR_PATH);
    when(encryptionService.decrypt(any(Encryptable.class), anyListOf(EncryptedDataDetail.class)))
        .thenReturn((Encryptable) hostConnectionAttributes.getValue());
    when(awsHelperService.getBucketRegion(any(AwsConfig.class), anyListOf(EncryptedDataDetail.class), anyString()))
        .thenReturn("us-west-1");
    when(baseExecutor.executeCommandString(anyString(), anyBoolean())).thenReturn(CommandExecutionStatus.SUCCESS);
    CommandExecutionStatus status = downloadArtifactCommandUnit.executeInternal(context);
    assertThat(status).isEqualTo(CommandExecutionStatus.SUCCESS);
  }

  private Map<String, String> mockMetadata() {
    Map<String, String> map = new HashMap<>();
    map.put(Constants.BUCKET_NAME, BUCKET_NAME);
    map.put(Constants.ARTIFACT_FILE_NAME, ARTIFACT_FILE_NAME);
    map.put(Constants.ARTIFACT_PATH, ARTIFACT_PATH);
    map.put(Constants.BUILD_NO, BUILD_NO);
    map.put(Constants.ARTIFACT_FILE_SIZE, String.valueOf(WingsTestConstants.ARTIFACT_FILE_SIZE));
    map.put(Constants.KEY, ACCESS_KEY);
    map.put(Constants.URL, S3_URL);
    return map;
  }
}
