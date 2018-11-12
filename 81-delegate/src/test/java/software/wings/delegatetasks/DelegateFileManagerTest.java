package software.wings.delegatetasks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.beans.artifact.ArtifactStreamAttributes.Builder.anArtifactStreamAttributes;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

import io.harness.CategoryTest;
import io.harness.rule.OwnerRule.Owner;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import software.wings.beans.AwsConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.beans.artifact.ArtifactStreamType;
import software.wings.beans.config.ArtifactoryConfig;
import software.wings.common.Constants;
import software.wings.delegate.app.DelegateConfiguration;
import software.wings.delegate.service.DelegateFileManagerImpl;
import software.wings.delegatetasks.collect.artifacts.ArtifactCollectionTaskHelper;
import software.wings.managerclient.ManagerClient;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class DelegateFileManagerTest extends CategoryTest {
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Mock ArtifactCollectionTaskHelper artifactCollectionTaskHelper;
  @Mock ManagerClient managerClient;
  @Mock DelegateConfiguration delegateConfiguration;
  @InjectMocks
  private DelegateFileManagerImpl delegateFileManager =
      new DelegateFileManagerImpl(managerClient, delegateConfiguration);

  private static final String ACCESS_KEY = "ACCESS_KEY";
  private static final String ACCOUNT_ID = "ACCOUNT_ID";
  private static final String ACTIVITY_ID = "ACTIVITY_ID";
  private static final String APP_ID = "APP_ID";
  private static final String ARTIFACT_FILE_NAME = "ARTIFACT_FILE_NAME";
  private static final String ARTIFACT_STREAM_ID_S3 = "ARTIFACT_STREAM_ID_S3";
  private static final String ARTIFACT_STREAM_ID_ARTIFACTORY = "ARTIFACT_STREAM_ID_ARTIFACTORY";
  private static final String ARTIFACT_PATH = "ARTIFACT_PATH";
  private static final String BUCKET_NAME = "BUCKET_NAME";
  private static final String BUILD_NO = "BUILD_NO";
  private static final String COMMAND_UNIT_NAME = "COMMAND_UNIT_NAME";
  private static final String HOST_NAME = "HOST_NAME";
  private static final String S3_URL = "S3_URL";
  private static final char[] SECRET_KEY = "SECRET_KEY".toCharArray();
  private static final String SETTING_ID = "SETTING_ID";
  private static final Long MY_SIZE = 3433L;
  private static final String ARTIFACT_REPO_BASE_DIR = "./repository/artifacts/";
  private static final String ARTIFACTORY_URL = "ARTIFACTORY_URL";

  private SettingAttribute awsSetting =
      aSettingAttribute()
          .withUuid(SETTING_ID)
          .withValue(AwsConfig.builder().secretKey(SECRET_KEY).accessKey(ACCESS_KEY).build())
          .build();
  private ArtifactStreamAttributes artifactStreamAttributesForS3 =
      anArtifactStreamAttributes()
          .withArtifactStreamType(ArtifactStreamType.AMAZON_S3.name())
          .withMetadataOnly(true)
          .withMetadata(mockMetadata(ArtifactStreamType.AMAZON_S3))
          .withServerSetting(awsSetting)
          .withArtifactStreamId(ARTIFACT_STREAM_ID_S3)
          .withArtifactServerEncryptedDataDetails(Collections.emptyList())
          .build();

  private SettingAttribute artifactorySetting = aSettingAttribute()
                                                    .withUuid(SETTING_ID)
                                                    .withValue(ArtifactoryConfig.builder()
                                                                   .artifactoryUrl(ARTIFACTORY_URL)
                                                                   .username("admin")
                                                                   .password("dummy123!".toCharArray())
                                                                   .build())
                                                    .build();
  private ArtifactStreamAttributes artifactStreamAttributesForArtifactory =
      anArtifactStreamAttributes()
          .withArtifactStreamType(ArtifactStreamType.ARTIFACTORY.name())
          .withMetadataOnly(true)
          .withMetadata(mockMetadata(ArtifactStreamType.ARTIFACTORY))
          .withServerSetting(artifactorySetting)
          .withArtifactStreamId(ARTIFACT_STREAM_ID_ARTIFACTORY)
          .withArtifactServerEncryptedDataDetails(Collections.emptyList())
          .build();

  @Test
  @Owner(emails = "aaditi.joag@harness.io", intermittent = true)
  public void testDownloadArtifactAtRuntimeForS3() throws IOException, ExecutionException {
    String fileContent = "test";
    InputStream is = new ByteArrayInputStream(fileContent.getBytes(Charset.defaultCharset()));
    Pair<String, InputStream> pair = new ImmutablePair<>(fileContent, is);
    when(artifactCollectionTaskHelper.downloadArtifactAtRuntime(
             artifactStreamAttributesForS3, ACCOUNT_ID, APP_ID, ACTIVITY_ID, COMMAND_UNIT_NAME, HOST_NAME))
        .thenReturn(pair);
    delegateFileManager.downloadArtifactAtRuntime(
        artifactStreamAttributesForS3, ACCOUNT_ID, APP_ID, ACTIVITY_ID, COMMAND_UNIT_NAME, HOST_NAME);
    String text =
        Files.toString(new File(ARTIFACT_REPO_BASE_DIR + "_" + ARTIFACT_STREAM_ID_S3 + "-" + BUILD_NO), Charsets.UTF_8);
    assertThat(text).isEqualTo(fileContent);
    FileUtils.deleteQuietly(new File(ARTIFACT_REPO_BASE_DIR + "_" + ARTIFACT_STREAM_ID_S3 + "-" + BUILD_NO));
  }

  @Test
  public void testDownloadArtifactAtRuntimeForArtifactory() throws IOException, ExecutionException {
    String fileContent = "test";
    InputStream is = new ByteArrayInputStream(fileContent.getBytes(Charset.defaultCharset()));
    Pair<String, InputStream> pair = new ImmutablePair<>(fileContent, is);
    when(artifactCollectionTaskHelper.downloadArtifactAtRuntime(
             artifactStreamAttributesForArtifactory, ACCOUNT_ID, APP_ID, ACTIVITY_ID, COMMAND_UNIT_NAME, HOST_NAME))
        .thenReturn(pair);
    delegateFileManager.downloadArtifactAtRuntime(
        artifactStreamAttributesForArtifactory, ACCOUNT_ID, APP_ID, ACTIVITY_ID, COMMAND_UNIT_NAME, HOST_NAME);
    String text = Files.toString(
        new File(ARTIFACT_REPO_BASE_DIR + "_" + ARTIFACT_STREAM_ID_ARTIFACTORY + "-" + BUILD_NO), Charsets.UTF_8);
    assertThat(text).isEqualTo(fileContent);
    FileUtils.deleteQuietly(new File(ARTIFACT_REPO_BASE_DIR + "_" + ARTIFACT_STREAM_ID_ARTIFACTORY + "-" + BUILD_NO));
  }

  @Test
  public void testGetArtifactFileSize() {
    when(artifactCollectionTaskHelper.getArtifactFileSize(any(ArtifactStreamAttributes.class))).thenReturn(1234L);
    Long size = delegateFileManager.getArtifactFileSize(artifactStreamAttributesForS3);
    assertThat(size.longValue()).isEqualTo(1234L);
  }

  private Map<String, String> mockMetadata(ArtifactStreamType artifactStreamType) {
    Map<String, String> map = new HashMap<>();
    switch (artifactStreamType) {
      case AMAZON_S3:
        map.put(Constants.BUCKET_NAME, BUCKET_NAME);
        map.put(Constants.ARTIFACT_FILE_NAME, ARTIFACT_FILE_NAME);
        map.put(Constants.ARTIFACT_PATH, ARTIFACT_PATH);
        map.put(Constants.BUILD_NO, BUILD_NO);
        map.put(Constants.ARTIFACT_FILE_SIZE, String.valueOf(MY_SIZE));
        map.put(Constants.KEY, ACCESS_KEY);
        map.put(Constants.URL, S3_URL);
        break;
      case ARTIFACTORY:
        map.put(Constants.ARTIFACT_FILE_NAME, ARTIFACT_FILE_NAME);
        map.put(Constants.ARTIFACT_PATH, ARTIFACT_PATH);
        map.put(Constants.BUILD_NO, BUILD_NO);
        map.put(Constants.ARTIFACT_FILE_SIZE, String.valueOf(MY_SIZE));
        break;
      default:
        break;
    }
    return map;
  }
}
