package software.wings.helpers.ext.artifactory;

import static io.harness.rule.OwnerRule.AADITI;
import static io.harness.rule.OwnerRule.GEORGE;
import static io.harness.rule.OwnerRule.SRINIVAS;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.utils.ArtifactType.RPM;
import static software.wings.utils.ArtifactType.WAR;

import com.google.common.collect.ImmutableMap;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.exception.WingsException;
import io.harness.rule.Owner;
import io.harness.waiter.ListNotifyResponseData;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import software.wings.beans.artifact.Artifact.ArtifactMetadataKeys;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.beans.artifact.ArtifactStreamType;
import software.wings.beans.config.ArtifactoryConfig;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.service.impl.security.EncryptionServiceImpl;
import software.wings.utils.ArtifactType;
import software.wings.utils.RepositoryType;

import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ArtifactoryServiceTest extends CategoryTest {
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
  public void setUp() throws IllegalAccessException {
    FieldUtils.writeField(artifactoryService, "encryptionService", new EncryptionServiceImpl(null, null), true);
  }

  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldGetMavenRepositories() {
    Map<String, String> repositories = artifactoryService.getRepositories(artifactoryConfig, null, WAR);
    assertThat(repositories).isNotNull();
    assertThat(repositories).containsKeys("harness-maven");
    assertThat(repositories).doesNotContainKeys("docker");
  }

  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldGetIvyRepositories() {
    Map<String, String> repositories = artifactoryService.getRepositories(artifactoryConfig, null, WAR);
    assertThat(repositories).isNotNull();
    assertThat(repositories).containsKeys("harness-ivy");
    assertThat(repositories).doesNotContainKeys("docker");
  }

  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldGetDockerRepositoriesWithArtifactType() {
    Map<String, String> repositories = artifactoryService.getRepositories(artifactoryConfig, null, ArtifactType.DOCKER);
    assertThat(repositories).isNotNull();
    assertThat(repositories).containsKeys("docker");
    assertThat(repositories).doesNotContainKeys("harness-maven");
  }

  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldGetDockerRepositories() {
    Map<String, String> repositories = artifactoryService.getRepositories(artifactoryConfig, null);
    assertThat(repositories).isNotNull();
    assertThat(repositories).containsKeys("docker");
    assertThat(repositories).doesNotContainKeys("harness-maven");
  }

  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void testGetRepositoriesForMavenWithPackageType() {
    Map<String, String> repositories = artifactoryService.getRepositories(artifactoryConfig, null, "maven");
    assertThat(repositories).isNotNull();
    assertThat(repositories).containsKeys("harness-maven");
    assertThat(repositories).containsKeys("harness-maven-snapshots");
    assertThat(repositories).doesNotContainKeys("docker");
  }

  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldGetRpmRepositories() {
    Map<String, String> repositories = artifactoryService.getRepositories(artifactoryConfig, null, RPM);
    assertThat(repositories).isNotNull();
    assertThat(repositories).containsKeys("harness-rpm");
  }

  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldGetDockerImages() {
    List<String> repositories = artifactoryService.getRepoPaths(artifactoryConfig, null, "docker");
    assertThat(repositories).isNotNull();
    assertThat(repositories).contains("wingsplugins/todolist");
  }

  @Test
  @Owner(developers = GEORGE)
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
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldGetRpmFilePaths() {
    List<BuildDetails> builds =
        artifactoryService.getFilePaths(artifactoryConfig, null, "harness-rpm", "todolist*", "generic", 50);
    assertThat(builds).isNotNull();
    assertThat(builds).extracting(buildDetails -> buildDetails.getNumber()).contains("todolist-1.0-2.x86_64.rpm");
  }

  @Test
  @Owner(developers = AADITI)
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
  @Owner(developers = AADITI)
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
  @Owner(developers = AADITI)
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
  @Owner(developers = AADITI)
  @Category(UnitTests.class)
  public void shouldGetCorrectBuildNoForArtifactPathsWithoutAnyWildcardCharacter1() {
    List<BuildDetails> builds = artifactoryService.getFilePaths(
        artifactoryConfig, null, "harness-maven", "io/harness/todolist/todolist/1.0/*.war", "any", 50);
    assertThat(builds).isNotNull();
    assertThat(builds).extracting(buildDetails -> buildDetails.getNumber()).contains("todolist-1.0.war");
  }

  @Test(expected = WingsException.class)
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldDownloadRpmArtifacts() {
    ListNotifyResponseData listNotifyResponseData =
        artifactoryService.downloadArtifacts(artifactoryConfig, null, "harness-rpm",
            ImmutableMap.of(ArtifactMetadataKeys.artifactPath, "harness-rpm/todolist-1.0-2.x86_64.rpm",
                ArtifactMetadataKeys.artifactFileName, "todolist-1.0-2.x86_64.rpm"),
            "delegateId", "taskId", "ACCOUNT_ID");
    assertThat(listNotifyResponseData).isNotNull();
  }

  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldValidateArtifactPath() {
    assertThat(artifactoryService.validateArtifactPath(artifactoryConfig, null, "harness-rpm", "todolist*", "generic"))
        .isTrue();
  }

  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldValidateArtifactPathAnonymous() {
    assertThat(artifactoryService.validateArtifactPath(
                   artifactoryConfigAnonymous, null, "harness-rpm", "todolist*", "generic"))
        .isTrue();
  }

  @Test(expected = WingsException.class)
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldValidateArtifactPathPasswordEmpty() {
    ArtifactoryConfig artifactoryConfigNoPassword =
        ArtifactoryConfig.builder().artifactoryUrl("some url").username("some username").build();
    artifactoryService.validateArtifactPath(artifactoryConfigNoPassword, null, "harness-rpm", "todolist*", "generic");
  }

  @Test(expected = WingsException.class)
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldValidateArtifactPathEmpty() {
    artifactoryService.validateArtifactPath(artifactoryConfig, null, "harness-rpm", "", "generic");
  }

  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldValidateArtifactPathMaven() {
    artifactoryService.validateArtifactPath(
        artifactoryConfig, null, "harness-rpm", "io/harness/todolist/*/todolist", "maven");
  }

  @Test(expected = WingsException.class)
  @Owner(developers = AADITI)
  @Category(UnitTests.class)
  public void shouldDownloadRpmArtifact() {
    Pair<String, InputStream> pair = artifactoryService.downloadArtifact(artifactoryConfig, null, "harness-rpm",
        ImmutableMap.of(ArtifactMetadataKeys.artifactPath, "harness-rpm/todolist-1.0-2.x86_64.rpm",
            ArtifactMetadataKeys.artifactFileName, "todolist-1.0-2.x86_64.rpm"));
    assertThat(pair).isNotNull();
  }

  @Test
  @Owner(developers = AADITI)
  @Category(UnitTests.class)
  public void shouldGetFileSize() {
    Map<String, String> metadata = new HashMap<>();
    metadata.put(ArtifactMetadataKeys.artifactPath, "harness-maven/io/harness/todolist/todolist/1.1/todolist-1.1.war");
    Long size = artifactoryService.getFileSize(artifactoryConfig, null, metadata);
    assertThat(size).isEqualTo(1776799L);
  }

  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldTestArtifactoryRunning() {
    assertThat(artifactoryService.isRunning(artifactoryConfig, null)).isTrue();
  }

  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void testGetRepositoriesWithRepositoryType() {
    Map<String, String> repositories =
        artifactoryService.getRepositories(artifactoryConfig, null, RepositoryType.docker);
    assertThat(repositories).isNotNull();
    assertThat(repositories).containsKeys("docker");
    assertThat(repositories).doesNotContainKeys("harness-maven");
  }

  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void testGetMavenRepositoriesWithRepositoryType() {
    Map<String, String> repositories =
        artifactoryService.getRepositories(artifactoryConfig, null, RepositoryType.maven);
    assertThat(repositories).isNotNull();
    assertThat(repositories).containsKeys("harness-maven");
    assertThat(repositories).doesNotContainKeys("docker");
  }

  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void testGetAnyRepositoriesWithRepositoryType() {
    Map<String, String> repositories = artifactoryService.getRepositories(artifactoryConfig, null, RepositoryType.any);
    assertThat(repositories).isNotNull();
    assertThat(repositories).containsKeys("harness-rpm");
    assertThat(repositories).doesNotContainKeys("docker");
  }

  @Test
  @Owner(developers = AADITI)
  @Category(UnitTests.class)
  public void shouldGetFilePathsWithWildCardForAnonymousUser() {
    List<BuildDetails> builds =
        artifactoryService.getFilePaths(artifactoryConfigAnonymous, null, "harness-maven", "tdlist/*/*.war", "any", 50);
    assertThat(builds).isNotNull();
    assertThat(builds)
        .extracting(buildDetails -> buildDetails.getNumber())
        .contains("tdlist/1.1/tdlist-1.1.war", "tdlist/1.2/tdlist-1.2.war");
  }

  @Test
  @Owner(developers = AADITI)
  @Category(UnitTests.class)
  public void shouldGetFilePathsForAnonymousUser() {
    List<BuildDetails> builds =
        artifactoryService.getFilePaths(artifactoryConfigAnonymous, null, "harness-maven", "myartifact", "any", 50);
    assertThat(builds).isNotNull();
    assertThat(builds).extracting(buildDetails -> buildDetails.getNumber()).contains("myartifact2");
  }
}
