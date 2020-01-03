package software.wings.service.impl;

import static io.harness.rule.OwnerRule.GARVIT;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import io.harness.rule.Owner;
import io.harness.scm.ScmSecret;
import io.harness.scm.SecretName;
import lombok.extern.slf4j.Slf4j;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import software.wings.WingsBaseTest;
import software.wings.beans.DockerConfig;
import software.wings.helpers.ext.docker.DockerRegistryService;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.rules.Integration;
import software.wings.service.intfc.DockerBuildService;

import java.util.Collections;
import java.util.List;

/**
 * Created by anubhaw on 1/6/17.
 */
@Integration
@Slf4j
public class DockerBuildServiceImplTest extends WingsBaseTest {
  private static final String DOCKER_REGISTRY_URL = "https://registry.hub.docker.com/v2/";

  @Inject private DockerRegistryService dockerRegistryService;
  @Inject private ScmSecret scmSecret;

  @Inject @InjectMocks private DockerBuildService dockerBuildService;

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  @Ignore("TODO: please provide clear motivation why this test is ignored")
  public void shouldGetBuildsWithoutCredentials() {
    DockerConfig dockerConfig = DockerConfig.builder().dockerRegistryUrl(DOCKER_REGISTRY_URL).build();
    List<BuildDetails> builds = dockerRegistryService.getBuilds(dockerConfig, null, "library/mysql", 5);
    logger.info(builds.toString());
    assertThat(builds.size()).isEqualTo(5);
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  @Ignore("TODO: please provide clear motivation why this test is ignored")
  public void shouldGetBuildsWithCredentials() {
    DockerConfig dockerConfig =
        DockerConfig.builder()
            .dockerRegistryUrl(DOCKER_REGISTRY_URL)
            .username("anubhaw")
            .password(scmSecret.decryptToCharArray(new SecretName("docker_config_anubhaw_password")))
            .build();
    List<BuildDetails> builds = dockerRegistryService.getBuilds(dockerConfig, null, "library/mysql", 5);
    logger.info(builds.toString());
    assertThat(builds.size()).isGreaterThanOrEqualTo(5);
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  @Ignore("TODO: please provide clear motivation why this test is ignored")
  public void shouldGetLastSuccessfulBuild() {
    DockerConfig dockerConfig =
        DockerConfig.builder()
            .dockerRegistryUrl(DOCKER_REGISTRY_URL)
            .username("anubhaw")
            .password(scmSecret.decryptToCharArray(new SecretName("docker_config_anubhaw_password")))
            .build();
    BuildDetails build = dockerRegistryService.getLastSuccessfulBuild(dockerConfig, null, "library/mysql");
    logger.info(build.toString());
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  @Ignore("TODO: please provide clear motivation why this test is ignored")
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
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  @Ignore("TODO: please provide clear motivation why this test is ignored")
  public void shouldValidateCredentials() {
    DockerConfig dockerConfig =
        DockerConfig.builder()
            .dockerRegistryUrl(DOCKER_REGISTRY_URL)
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
