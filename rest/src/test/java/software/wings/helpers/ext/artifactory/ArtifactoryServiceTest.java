package software.wings.helpers.ext.artifactory;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.config.ArtifactoryConfig.Builder.anArtifactoryConfig;
import static software.wings.utils.ArtifactType.RPM;
import static software.wings.utils.ArtifactType.WAR;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import software.wings.beans.config.ArtifactoryConfig;
import software.wings.helpers.ext.jenkins.BuildDetails;

import java.util.List;
import java.util.Map;

// import javax.inject.Inject;

/**
 * Created by sgurubelli on 9/29/17.
 */
public class ArtifactoryServiceTest {
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  @InjectMocks ArtifactoryService artifactoryService = new ArtifactoryServiceImpl();

  /**
   * The Wire mock rule.
   */
  @Rule public WireMockRule wireMockRule = new WireMockRule(8881);

  String url = "http://localhost:8881/artifactory/";

  private ArtifactoryConfig artifactoryConfig = anArtifactoryConfig()
                                                    .withArtifactoryUrl(url)
                                                    .withUsername("admin")
                                                    .withPassword("dummy123!".toCharArray())
                                                    .build();

  @Test
  public void shouldGetMavenRepositories() {
    Map<String, String> repositories = artifactoryService.getRepositories(artifactoryConfig, WAR);
    assertThat(repositories).isNotNull();
    assertThat(repositories).containsKeys("harness-maven");
    assertThat(repositories).doesNotContainKeys("docker");
  }

  @Test
  public void shouldGetDockerRepositories() {
    Map<String, String> repositories = artifactoryService.getRepositories(artifactoryConfig);
    assertThat(repositories).isNotNull();
    assertThat(repositories).containsKeys("docker");
    assertThat(repositories).doesNotContainKeys("harness-maven");
  }

  @Test
  public void shouldGetRpmRepositories() {
    Map<String, String> repositories = artifactoryService.getRepositories(artifactoryConfig, RPM);
    assertThat(repositories).isNotNull();
    assertThat(repositories).containsKeys("harness-rpm");
    assertThat(repositories).doesNotContainKeys("harness-maven");
  }

  @Test
  public void shouldGetDockerImages() {
    List<String> repositories = artifactoryService.getRepoPaths(artifactoryConfig, "docker");
    assertThat(repositories).isNotNull();
    assertThat(repositories).contains("wingsplugins/todolist");
  }

  @Test
  public void shouldGetDockerTags() {
    List<BuildDetails> builds = artifactoryService.getBuilds(artifactoryConfig, "docker", "wingsplugins/todolist", 50);
    assertThat(builds).isNotNull();
    assertThat(builds).extracting(buildDetails -> buildDetails.getNumber()).contains("latest");
  }

  @Test
  public void shouldGetRpmFilePaths() {
    List<BuildDetails> builds =
        artifactoryService.getFilePaths(artifactoryConfig, "harness-rpm", "", "todolist*", RPM, 50);
    assertThat(builds).isNotNull();
    assertThat(builds).extracting(buildDetails -> buildDetails.getNumber()).contains("todolist-1.0-2.x86_64.rpm");
  }

  @Test
  public void shouldGetGroupIds() {
    List<String> groupIds = artifactoryService.getRepoPaths(artifactoryConfig, "harness-maven");
    assertThat(groupIds).isNotNull();
    assertThat(groupIds).contains("io.harness");
    assertThat(groupIds).contains("io.harness.portal");
  }

  @Test
  public void shouldGetGroupIdsForAnonymousUser() {
    stubFor(get(urlPathEqualTo("/artifactory/api/storage/harness-maven-snapshots"))
                .withQueryParam("deep", containing("1"))
                .willReturn(aResponse().withStatus(403)));
    // stubFor(get(urlPathEqualTo("/artifactory/api/storage/harness-maven-snapshots/io/")).willReturn(aResponse().withBody().withStatus(403)));
    List<String> groupIds = artifactoryService.getRepoPaths(artifactoryConfig, "harness-maven-snapshots");
    assertThat(groupIds).isNotNull();
    assertThat(groupIds).contains("io.harness");
    assertThat(groupIds).contains("io.harness.portal");
  }
}
