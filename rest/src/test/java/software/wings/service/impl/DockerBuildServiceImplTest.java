package software.wings.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.DockerConfig.Builder.aDockerConfig;

import org.junit.Ignore;
import org.junit.Test;
import org.mockito.InjectMocks;
import software.wings.WingsBaseTest;
import software.wings.beans.DockerConfig;
import software.wings.beans.ErrorCode;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.DockerArtifactStream;
import software.wings.exception.WingsException;
import software.wings.helpers.ext.docker.DockerRegistryService;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.rules.Integration;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.DockerBuildService;
import software.wings.utils.ArtifactType;

import java.util.List;
import javax.inject.Inject;

/**
 * Created by anubhaw on 1/6/17.
 */
@Ignore
@Integration
public class DockerBuildServiceImplTest extends WingsBaseTest {
  @Inject private DockerRegistryService dockerRegistryService;
  @Inject private ArtifactStreamService artifactStreamService;

  @Inject @InjectMocks private DockerBuildService dockerBuildService;

  @Test
  @Ignore
  public void shouldGetBuilds() {
    DockerArtifactStream dockerArtifactStream = DockerArtifactStream.Builder.aDockerArtifactStream()
                                                    .withAppId("UXGI1f4vQa6nt5eXBcnv7A")
                                                    .withImageName("library/mysql")
                                                    .withAutoDownload(true)
                                                    .withAutoApproveForProduction(true)
                                                    .withSettingId("knCLyrVjRjyUYM15RcjUQQ")
                                                    .withSourceName(ArtifactType.DOCKER.name())
                                                    .withServiceId("Yn57GaqwR9ioXq8YZ4V87Q")
                                                    .build();
    ArtifactStream artifactStream = artifactStreamService.create(dockerArtifactStream);
    System.out.println(artifactStream.toString());
  }

  @Test
  public void shouldGetLastSuccessfulBuild() {
    DockerConfig dockerConfig = aDockerConfig()
                                    .withDockerRegistryUrl("https://registry.hub.docker.com/v2/")
                                    .withUsername("anubhaw")
                                    .withPassword("anubhaw@Dhub".toCharArray())
                                    .build();
    List<BuildDetails> builds = dockerRegistryService.getBuilds(dockerConfig, "library/mysql", 5);
    System.out.println(builds);
  }

  @Test
  public void shouldValidateInvalidUrl() {
    DockerConfig dockerConfig = aDockerConfig()
                                    .withDockerRegistryUrl("invalid_url")
                                    .withUsername("anubhaw")
                                    .withPassword("anubhaw@Dhub".toCharArray())
                                    .build();
    try {
      dockerBuildService.validateArtifactServer(dockerConfig);
    } catch (WingsException e) {
      assertThat(e.getMessage()).isEqualTo(ErrorCode.INVALID_ARTIFACT_SERVER.toString());
      assertThat(e.getParams()).isNotEmpty();
      assertThat(e.getParams().get("message")).isEqualTo("Docker Registry URL must be a valid URL");
    }
  }

  @Test
  public void shouldValidateCredentials() {
    DockerConfig dockerConfig = aDockerConfig()
                                    .withDockerRegistryUrl("https://registry.hub.docker.com/v2/")
                                    .withUsername("invalid")
                                    .withPassword("anubhaw@Dhub".toCharArray())
                                    .build();
    try {
      dockerRegistryService.validateCredentials(dockerConfig);
    } catch (WingsException e) {
      assertThat(e.getMessage()).isEqualTo(ErrorCode.INVALID_ARTIFACT_SERVER.toString());
      assertThat(e.getParams()).isNotEmpty();
      assertThat(e.getParams().get("message")).isEqualTo("Invalid Docker Registry credentials");
    }
  }
}
