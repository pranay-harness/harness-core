package software.wings.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import io.harness.scm.ScmSecret;
import io.harness.scm.SecretName;
import lombok.extern.slf4j.Slf4j;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import software.wings.WingsBaseTest;
import software.wings.beans.DockerConfig;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.DockerArtifactStream;
import software.wings.helpers.ext.docker.DockerRegistryService;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.rules.Integration;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.DockerBuildService;
import software.wings.utils.ArtifactType;

import java.util.Collections;
import java.util.List;

/**
 * Created by anubhaw on 1/6/17.
 */
@Ignore
@Integration
@Slf4j
public class DockerBuildServiceImplTest extends WingsBaseTest {
  @Inject private DockerRegistryService dockerRegistryService;
  @Inject private ArtifactStreamService artifactStreamService;
  @Inject private ScmSecret scmSecret;

  @Inject @InjectMocks private DockerBuildService dockerBuildService;

  @Test
  @Category(UnitTests.class)
  @Ignore
  public void shouldGetBuilds() {
    DockerArtifactStream dockerArtifactStream = DockerArtifactStream.builder()
                                                    .appId("UXGI1f4vQa6nt5eXBcnv7A")
                                                    .imageName("library/mysql")
                                                    .settingId("knCLyrVjRjyUYM15RcjUQQ")
                                                    .sourceName(ArtifactType.DOCKER.name())
                                                    .serviceId("Yn57GaqwR9ioXq8YZ4V87Q")
                                                    .build();
    ArtifactStream artifactStream = artifactStreamService.create(dockerArtifactStream);
    logger.info(artifactStream.toString());
  }

  @Test
  @Category(UnitTests.class)
  public void shouldGetLastSuccessfulBuild() {
    DockerConfig dockerConfig =
        DockerConfig.builder()
            .dockerRegistryUrl("https://registry.hub.docker.com/v2/")
            .username("anubhaw")
            .password(scmSecret.decryptToCharArray(new SecretName("docker_config_anubhaw_password")))
            .build();
    List<BuildDetails> builds = dockerRegistryService.getBuilds(dockerConfig, null, "library/mysql", 5);
    logger.info(builds.toString());
  }

  @Test
  @Category(UnitTests.class)
  public void shouldValidateInvalidUrl() {
    DockerConfig dockerConfig =
        DockerConfig.builder()
            .dockerRegistryUrl("invalid_url")
            .username("anubhaw")
            .password(scmSecret.decryptToCharArray(new SecretName("docker_config_anubhaw_password")))
            .build();
    try {
      dockerBuildService.validateArtifactServer(dockerConfig, Collections.emptyList());
    } catch (WingsException e) {
      assertThat(e.getMessage()).isEqualTo(ErrorCode.INVALID_ARTIFACT_SERVER.toString());
      assertThat(e.getParams()).isNotEmpty();
      assertThat(e.getParams().get("message")).isEqualTo("Docker Registry URL must be a valid URL");
    }
  }

  @Test
  @Category(UnitTests.class)
  public void shouldValidateCredentials() {
    DockerConfig dockerConfig =
        DockerConfig.builder()
            .dockerRegistryUrl("https://registry.hub.docker.com/v2/")
            .username("invalid")
            .password(scmSecret.decryptToCharArray(new SecretName("docker_config_anubhaw_password")))
            .build();
    try {
      dockerRegistryService.validateCredentials(dockerConfig, null);
    } catch (WingsException e) {
      assertThat(e.getMessage()).isEqualTo(ErrorCode.INVALID_ARTIFACT_SERVER.toString());
      assertThat(e.getParams()).isNotEmpty();
      assertThat(e.getParams().get("message")).isEqualTo("Invalid Docker Registry credentials");
    }
  }
}
