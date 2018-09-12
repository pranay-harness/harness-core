package software.wings.helpers.ext.artifactory;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.internal.util.reflection.Whitebox.setInternalState;
import static software.wings.common.Constants.ARTIFACT_FILE_NAME;
import static software.wings.common.Constants.ARTIFACT_PATH;
import static software.wings.utils.ArtifactType.RPM;
import static software.wings.utils.ArtifactType.WAR;

import com.google.common.collect.ImmutableMap;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.harness.exception.WingsException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import software.wings.beans.config.ArtifactoryConfig;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.service.impl.security.EncryptionServiceImpl;
import software.wings.waitnotify.ListNotifyResponseData;

import java.util.List;
import java.util.Map;

/**
 * Created by sgurubelli on 9/29/17.
 */
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

  @Before
  public void setUp() {
    setInternalState(artifactoryService, "encryptionService", new EncryptionServiceImpl());
  }

  @Test
  public void shouldGetMavenRepositories() {
    Map<String, String> repositories = artifactoryService.getRepositories(artifactoryConfig, null, WAR);
    assertThat(repositories).isNotNull();
    assertThat(repositories).containsKeys("harness-maven");
    assertThat(repositories).doesNotContainKeys("docker");
  }

  @Test
  public void shouldGetDockerRepositories() {
    Map<String, String> repositories = artifactoryService.getRepositories(artifactoryConfig, null);
    assertThat(repositories).isNotNull();
    assertThat(repositories).containsKeys("docker");
    assertThat(repositories).doesNotContainKeys("harness-maven");
  }

  @Test
  public void shouldGetRpmRepositories() {
    Map<String, String> repositories = artifactoryService.getRepositories(artifactoryConfig, null, RPM);
    assertThat(repositories).isNotNull();
    assertThat(repositories).containsKeys("harness-rpm");
  }

  @Test
  public void shouldGetDockerImages() {
    List<String> repositories = artifactoryService.getRepoPaths(artifactoryConfig, null, "docker");
    assertThat(repositories).isNotNull();
    assertThat(repositories).contains("wingsplugins/todolist");
  }

  @Test
  public void shouldGetDockerTags() {
    List<BuildDetails> builds =
        artifactoryService.getBuilds(artifactoryConfig, null, "docker", "wingsplugins/todolist", 50);
    assertThat(builds).isNotNull();
    assertThat(builds).extracting(buildDetails -> buildDetails.getNumber()).contains("latest");
  }

  @Test
  public void shouldGetRpmFilePaths() {
    List<BuildDetails> builds =
        artifactoryService.getFilePaths(artifactoryConfig, null, "harness-rpm", "todolist*", "generic", 50);
    assertThat(builds).isNotNull();
    assertThat(builds).extracting(buildDetails -> buildDetails.getNumber()).contains("todolist-1.0-2.x86_64.rpm");
  }

  @Test
  public void shouldGetGroupIds() {
    List<String> groupIds = artifactoryService.getRepoPaths(artifactoryConfig, null, "harness-maven");
    assertThat(groupIds).isNotNull();
    assertThat(groupIds).contains("io.harness");
    assertThat(groupIds).contains("io.harness.portal");
  }

  @Test
  public void shouldGetGroupIdsForAnonymousUser() {
    stubFor(get(urlPathEqualTo("/artifactory/api/storage/harness-maven-snapshots"))
                .withQueryParam("deep", containing("1"))
                .willReturn(aResponse().withStatus(403)));
    List<String> groupIds = artifactoryService.getRepoPaths(artifactoryConfig, null, "harness-maven-snapshots");
    assertThat(groupIds).isNotNull();
    //    assertThat(groupIds).contains("io.harness");
    //  assertThat(groupIds).contains("io.harness");
  }

  @Test
  public void shouldGetArtifactIds() {
    List<String> groupIds =
        artifactoryService.getArtifactIds(artifactoryConfig, null, "harness-maven-snapshots", "io.harness");
    assertThat(groupIds).isEmpty();
  }

  @Test
  public void shouldGetLatestVersion() {
    BuildDetails buildDetails = artifactoryService.getLatestVersion(
        artifactoryConfig, null, "harness-maven", "io.harness.todolist", "todolist");
    assertThat(buildDetails).isNotNull();
    assertThat(buildDetails.getNumber()).isEqualTo("1.1");
  }

  @Test
  public void shouldGetLatestSnapshotVersion() {
    BuildDetails buildDetails = artifactoryService.getLatestVersion(
        artifactoryConfig, null, "harness-maven-snapshots", "io.harness.todolist", "snapshot");
    assertThat(buildDetails).isNotNull();
    assertThat(buildDetails.getNumber()).isEqualTo("1.1");
  }

  @Test
  public void shouldDownloadArtifacts() {
    ListNotifyResponseData listNotifyResponseData = artifactoryService.downloadArtifacts(artifactoryConfig, null,
        "harness-maven-snapshots", "io.harness.todolist", asList("todolist"), "io/harness/todolist",
        ImmutableMap.of("buildNo", "1.1"), "delegateId", "taskId", "ACCOUNT_ID");

    assertThat(listNotifyResponseData).isNotNull();
  }

  @Test(expected = WingsException.class)
  public void shouldDownloadRpmArtifacts() {
    ListNotifyResponseData listNotifyResponseData = artifactoryService.downloadArtifacts(artifactoryConfig, null,
        "harness-rpm", "io.harness.todolist", asList("todolist"), "io/harness/todolist",
        ImmutableMap.of(
            ARTIFACT_PATH, "harness-rpm/todolist-1.0-2.x86_64.rpm", ARTIFACT_FILE_NAME, "todolist-1.0-2.x86_64.rpm"),
        "delegateId", "taskId", "ACCOUNT_ID");
    assertThat(listNotifyResponseData).isNotNull();
  }

  @Test
  public void shouldValidateArtifactPath() {
    artifactoryService.validateArtifactPath(artifactoryConfig, null, "harness-rpm", "todolist*", "generic");
  }

  @Test(expected = WingsException.class)
  public void shouldValidateArtifactPathEmpty() {
    artifactoryService.validateArtifactPath(artifactoryConfig, null, "harness-rpm", "", "generic");
  }

  @Test
  public void shouldValidateArtifactPathMaven() {
    artifactoryService.validateArtifactPath(
        artifactoryConfig, null, "harness-rpm", "io/harness/todolist/*/todolist", "maven");
  }
}
