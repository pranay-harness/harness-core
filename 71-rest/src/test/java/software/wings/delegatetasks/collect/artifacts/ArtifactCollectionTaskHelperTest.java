package software.wings.delegatetasks.collect.artifacts;

import static io.harness.rule.OwnerRule.GARVIT;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.ACTIVITY_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.COMMAND_UNIT_NAME;
import static software.wings.utils.WingsTestConstants.HOST_NAME;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidArgumentsException;
import io.harness.rule.Owner;
import io.harness.security.encryption.EncryptedDataDetail;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.artifact.Artifact.ArtifactMetadataKeys;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.beans.artifact.ArtifactStreamType;
import software.wings.beans.artifact.AzureArtifactsArtifactStream.ProtocolType;
import software.wings.beans.settings.azureartifacts.AzureArtifactsConfig;
import software.wings.beans.settings.azureartifacts.AzureArtifactsPATConfig;
import software.wings.helpers.ext.azure.devops.AzureArtifactsPackageFileInfo;
import software.wings.helpers.ext.azure.devops.AzureArtifactsService;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ArtifactCollectionTaskHelperTest extends WingsBaseTest {
  private static final String MAVEN = ProtocolType.maven.name();
  private static final String FEED = "FEED";
  private static final String PACKAGE_ID = "PACKAGE_ID";
  private static final String PACKAGE_NAME_MAVEN = "GROUP_ID:ARTIFACT_ID";

  @Inject @InjectMocks private ArtifactCollectionTaskHelper artifactCollectionTaskHelper;
  @Mock private AzureArtifactsService azureArtifactsService;

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void shouldDownloadAzureArtifactsArtifactAtRuntime() {
    String fileName = "file.war";
    String content = "file content";
    InputStream inputStream = new ByteArrayInputStream(content.getBytes());
    when(azureArtifactsService.downloadArtifact(any(AzureArtifactsConfig.class), anyListOf(EncryptedDataDetail.class),
             any(ArtifactStreamAttributes.class), anyMap()))
        .thenReturn(ImmutablePair.of(fileName, inputStream));

    Pair<String, InputStream> pair = artifactCollectionTaskHelper.downloadArtifactAtRuntime(
        ArtifactStreamAttributes.builder()
            .artifactStreamType(ArtifactStreamType.AZURE_ARTIFACTS.name())
            .serverSetting(aSettingAttribute().withValue(AzureArtifactsPATConfig.builder().build()).build())
            .feed(FEED)
            .packageId(PACKAGE_ID)
            .protocolType(MAVEN)
            .packageName(PACKAGE_NAME_MAVEN)
            .metadata(ImmutableMap.of(ArtifactMetadataKeys.versionId, "id1", ArtifactMetadataKeys.version, "1",
                ArtifactMetadataKeys.artifactFileName, fileName))
            .build(),
        ACCOUNT_ID, APP_ID, ACTIVITY_ID, COMMAND_UNIT_NAME, HOST_NAME);

    assertThat(pair).isNotNull();
    assertThat(pair.getLeft()).isEqualTo(fileName);
    assertThat(pair.getRight()).isNotNull();
    assertThat(convertInputStreamToString(pair.getRight())).isEqualTo(content);
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void shouldGetAzureArtifactsFileSize() {
    String fileName = "file.war";
    String content = "file content";
    InputStream inputStream = new ByteArrayInputStream(content.getBytes());
    when(azureArtifactsService.listFiles(any(AzureArtifactsConfig.class), anyListOf(EncryptedDataDetail.class),
             any(ArtifactStreamAttributes.class), anyMap(), eq(false)))
        .thenReturn(Arrays.asList(new AzureArtifactsPackageFileInfo("random1", 4),
            new AzureArtifactsPackageFileInfo(fileName, 8), new AzureArtifactsPackageFileInfo("random2", 16)));
    assertThat(getArtifactFileSize(fileName)).isEqualTo(8);
  }

  @Test(expected = InvalidArgumentsException.class)
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void shouldNotGetAzureArtifactsFileSizeForInvalidFileName() {
    String fileName = "file.war";
    when(azureArtifactsService.listFiles(any(AzureArtifactsConfig.class), anyListOf(EncryptedDataDetail.class),
             any(ArtifactStreamAttributes.class), anyMap(), eq(false)))
        .thenReturn(Arrays.asList(new AzureArtifactsPackageFileInfo("random1", 4),
            new AzureArtifactsPackageFileInfo(fileName, 8), new AzureArtifactsPackageFileInfo("random2", 16)));
    getArtifactFileSize(null);
  }

  @Test(expected = InvalidArgumentsException.class)
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void shouldNotGetAzureArtifactsFileSizeForNoFiles() {
    String fileName = "file.war";
    when(azureArtifactsService.listFiles(any(AzureArtifactsConfig.class), anyListOf(EncryptedDataDetail.class),
             any(ArtifactStreamAttributes.class), anyMap(), eq(false)))
        .thenReturn(Collections.emptyList());
    getArtifactFileSize(fileName);
  }

  @Test(expected = InvalidArgumentsException.class)
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void shouldNotGetAzureArtifactsFileSizeForFileNotPresent() {
    String fileName = "file.war";
    when(azureArtifactsService.listFiles(any(AzureArtifactsConfig.class), anyListOf(EncryptedDataDetail.class),
             any(ArtifactStreamAttributes.class), anyMap(), eq(false)))
        .thenReturn(Arrays.asList(
            new AzureArtifactsPackageFileInfo("random1", 4), new AzureArtifactsPackageFileInfo("random2", 16)));
    getArtifactFileSize(fileName);
  }

  private Long getArtifactFileSize(String fileName) {
    Map<String, String> metadata = new HashMap<>();
    metadata.put(ArtifactMetadataKeys.version, "id1");
    metadata.put(ArtifactMetadataKeys.versionId, "1");
    if (isNotBlank(fileName)) {
      metadata.put(ArtifactMetadataKeys.artifactFileName, fileName);
    }

    return artifactCollectionTaskHelper.getArtifactFileSize(
        ArtifactStreamAttributes.builder()
            .artifactStreamType(ArtifactStreamType.AZURE_ARTIFACTS.name())
            .serverSetting(aSettingAttribute().withValue(AzureArtifactsPATConfig.builder().build()).build())
            .feed(FEED)
            .packageId(PACKAGE_ID)
            .protocolType(MAVEN)
            .packageName(PACKAGE_NAME_MAVEN)
            .metadata(metadata)
            .build());
  }

  private String convertInputStreamToString(InputStream in) {
    try {
      StringBuilder sb = new StringBuilder();
      try (Reader reader = new BufferedReader(new InputStreamReader(in))) {
        int c;
        while ((c = reader.read()) != -1) {
          sb.append((char) c);
        }
      }
      return sb.toString();
    } catch (IOException e) {
      return "";
    }
  }
}
