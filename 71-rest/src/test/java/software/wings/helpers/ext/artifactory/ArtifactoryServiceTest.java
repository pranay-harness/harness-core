package software.wings.helpers.ext.artifactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.internal.util.reflection.Whitebox.setInternalState;
import static software.wings.common.Constants.ARTIFACT_FILE_NAME;
import static software.wings.common.Constants.ARTIFACT_PATH;
import static software.wings.utils.ArtifactType.RPM;
import static software.wings.utils.ArtifactType.WAR;

import com.google.common.collect.ImmutableMap;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.harness.category.element.UnitTests;
import io.harness.exception.WingsException;
import io.harness.waiter.ListNotifyResponseData;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.beans.artifact.ArtifactStreamType;
import software.wings.beans.config.ArtifactoryConfig;
import software.wings.common.Constants;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.service.impl.security.EncryptionServiceImpl;

import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ArtifactoryServiceTest {
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  @InjectMocks ArtifactoryService artifactoryService = new ArtifactoryServiceImpl();

  /**
   * The Wire mock rule.
   */
  @Rule public WireMockRule wireMockRule = new WireMockRule(9881);

  String url = "http://localhost:9881/artifactory/";

  private ArtifactoryConfig artifactoryConfig =
      ArtifactoryConfig.builder().artifactoryUrl(url).username("admin").password("dummy123!".toCharArray()).build();

  private ArtifactoryConfig artifactoryConfigAnonymous = ArtifactoryConfig.builder().artifactoryUrl(url).build();

  @Before
  public void setUp() {
    setInternalState(artifactoryService, "encryptionService", new EncryptionServiceImpl());
  }

  @Test
  @Category(UnitTests.class)
  public void shouldGetMavenRepositories() {
    Map<String, String> repositories = artifactoryService.getRepositories(artifactoryConfig, null, WAR);
    assertThat(repositories).isNotNull();
    assertThat(repositories).containsKeys("harness-maven");
    assertThat(repositories).doesNotContainKeys("docker");
  }

  @Test
  @Category(UnitTests.class)
  public void shouldGetIvyRepositories() {
    Map<String, String> repositories = artifactoryService.getRepositories(artifactoryConfig, null, WAR);
    assertThat(repositories).isNotNull();
    assertThat(repositories).containsKeys("harness-ivy");
    assertThat(repositories).doesNotContainKeys("docker");
  }

  @Test
  @Category(UnitTests.class)
  public void shouldGetDockerRepositories() {
    Map<String, String> repositories = artifactoryService.getRepositories(artifactoryConfig, null);
    assertThat(repositories).isNotNull();
    assertThat(repositories).containsKeys("docker");
    assertThat(repositories).doesNotContainKeys("harness-maven");
  }

  @Test
  @Category(UnitTests.class)
  public void shouldGetRpmRepositories() {
    Map<String, String> repositories = artifactoryService.getRepositories(artifactoryConfig, null, RPM);
    assertThat(repositories).isNotNull();
    assertThat(repositories).containsKeys("harness-rpm");
  }

  @Test
  @Category(UnitTests.class)
  public void shouldGetDockerImages() {
    List<String> repositories = artifactoryService.getRepoPaths(artifactoryConfig, null, "docker");
    assertThat(repositories).isNotNull();
    assertThat(repositories).contains("wingsplugins/todolist");
  }

  @Test
  @Category(UnitTests.class)
  public void shouldGetDockerTags() {
    List<BuildDetails> builds = artifactoryService.getBuilds(artifactoryConfig, null,
        ArtifactStreamAttributes.builder()
            .artifactStreamType(ArtifactStreamType.ARTIFACTORY.name())
            .metadataOnly(true)
            .jobName("docker")
            .imageName("wingsplugins/todolist")
            .artifactoryDockerRepositoryServer("harness.jfrog.com")
            .artifactServerEncryptedDataDetails(Collections.emptyList())
            .build(),
        50);
    assertThat(builds).isNotNull();
    assertThat(builds).extracting(buildDetails -> buildDetails.getNumber()).contains("latest");
  }

  @Test
  @Category(UnitTests.class)
  public void shouldGetRpmFilePaths() {
    List<BuildDetails> builds =
        artifactoryService.getFilePaths(artifactoryConfig, null, "harness-rpm", "todolist*", "generic", 50);
    assertThat(builds).isNotNull();
    assertThat(builds).extracting(buildDetails -> buildDetails.getNumber()).contains("todolist-1.0-2.x86_64.rpm");
  }

  @Test
  @Category(UnitTests.class)
  public void shouldGetCorrectBuildNoWithAnyWildcardMatch() {
    List<BuildDetails> builds = artifactoryService.getFilePaths(
        artifactoryConfig, null, "harness-maven", "io/harness/todolist/todolist/*/*.war", "any", 50);
    assertThat(builds).isNotNull();
    assertThat(builds)
        .extracting(buildDetails -> buildDetails.getNumber())
        .contains("1.0.0-SNAPSHOT/todolist-1.0.0-20170930.195402-1.war");
  }

  @Test
  @Category(UnitTests.class)
  public void shouldGetCorrectBuildNoForAtLeastOneWildcardPattern() {
    List<BuildDetails> builds = artifactoryService.getFilePaths(
        artifactoryConfig, null, "harness-maven", "io/harness/todolist/todolist/[0-9]+/*.war", "any", 50);
    assertThat(builds).isNotNull();
    assertThat(builds)
        .extracting(buildDetails -> buildDetails.getNumber())
        .contains("1.0.0-SNAPSHOT/todolist-1.0.0-20170930.195402-1.war");
  }

  @Test
  @Category(UnitTests.class)
  public void shouldGetCorrectBuildNoForArtifactPathsWithoutAnyWildcardCharacter() {
    List<BuildDetails> builds = artifactoryService.getFilePaths(
        artifactoryConfig, null, "harness-maven", "io/harness/todolist/todolist/1.0/todolist-1.0.war", "any", 50);
    assertThat(builds).isNotNull();
    assertThat(builds)
        .extracting(buildDetails -> buildDetails.getNumber())
        .contains("io/harness/todolist/todolist/1.0/todolist-1.0.war");
  }

  @Test
  @Category(UnitTests.class)
  public void shouldGetCorrectBuildNoForArtifactPathsWithoutAnyWildcardCharacter1() {
    List<BuildDetails> builds = artifactoryService.getFilePaths(
        artifactoryConfig, null, "harness-maven", "io/harness/todolist/todolist/1.0/*.war", "any", 50);
    assertThat(builds).isNotNull();
    assertThat(builds).extracting(buildDetails -> buildDetails.getNumber()).contains("todolist-1.0.war");
  }

  @Test(expected = WingsException.class)
  @Category(UnitTests.class)
  public void shouldDownloadRpmArtifacts() {
    ListNotifyResponseData listNotifyResponseData =
        artifactoryService.downloadArtifacts(artifactoryConfig, null, "harness-rpm",
            ImmutableMap.of(ARTIFACT_PATH, "harness-rpm/todolist-1.0-2.x86_64.rpm", ARTIFACT_FILE_NAME,
                "todolist-1.0-2.x86_64.rpm"),
            "delegateId", "taskId", "ACCOUNT_ID");
    assertThat(listNotifyResponseData).isNotNull();
  }

  @Test
  @Category(UnitTests.class)
  public void shouldValidateArtifactPath() {
    assertThat(artifactoryService.validateArtifactPath(artifactoryConfig, null, "harness-rpm", "todolist*", "generic"))
        .isTrue();
  }

  @Test
  @Category(UnitTests.class)
  public void shouldValidateArtifactPathAnonymous() {
    assertThat(artifactoryService.validateArtifactPath(
                   artifactoryConfigAnonymous, null, "harness-rpm", "todolist*", "generic"))
        .isTrue();
  }

  @Test(expected = WingsException.class)
  @Category(UnitTests.class)
  public void shouldValidateArtifactPathPassWordEmpty() {
    ArtifactoryConfig artifactoryConfigNoPassword =
        ArtifactoryConfig.builder().artifactoryUrl("some url").username("some username").build();
    artifactoryService.validateArtifactPath(artifactoryConfigNoPassword, null, "harness-rpm", "todolist*", "generic");
  }

  @Test(expected = WingsException.class)
  @Category(UnitTests.class)
  public void shouldValidateArtifactPathEmpty() {
    artifactoryService.validateArtifactPath(artifactoryConfig, null, "harness-rpm", "", "generic");
  }

  @Test
  @Category(UnitTests.class)
  public void shouldValidateArtifactPathMaven() {
    artifactoryService.validateArtifactPath(
        artifactoryConfig, null, "harness-rpm", "io/harness/todolist/*/todolist", "maven");
  }

  @Test(expected = WingsException.class)
  @Category(UnitTests.class)
  public void shouldDownloadRpmArtifact() {
    Pair<String, InputStream> pair = artifactoryService.downloadArtifact(artifactoryConfig, null, "harness-rpm",
        ImmutableMap.of(
            ARTIFACT_PATH, "harness-rpm/todolist-1.0-2.x86_64.rpm", ARTIFACT_FILE_NAME, "todolist-1.0-2.x86_64.rpm"));
    assertThat(pair).isNotNull();
  }

  @Test
  @Category(UnitTests.class)
  public void shouldGetFileSize() {
    Map<String, String> metadata = new HashMap<>();
    metadata.put(Constants.ARTIFACT_PATH, "harness-maven/io/harness/todolist/todolist/1.1/todolist-1.1.war");
    Long size = artifactoryService.getFileSize(artifactoryConfig, null, metadata);
    assertThat(size).isEqualTo(1776799L);
  }

  @Test
  @Category(UnitTests.class)
  public void shouldTestArtifactoryRunning() {
    assertThat(artifactoryService.isRunning(artifactoryConfig, null)).isTrue();
  }
}
