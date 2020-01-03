package software.wings.beans;

import static io.harness.rule.OwnerRule.SRINIVAS;
import static io.harness.rule.OwnerRule.UNKNOWN;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;

public class DockerConfigTest extends WingsBaseTest {
  @Test
  @Owner(developers = UNKNOWN)
  @Category(UnitTests.class)
  // test with a URL that doesn't end on a / and make sure a / gets added
  public void testConstructorWithUrlWithoutSlash() {
    String urlWithoutSlash = "http://some.docker.com/v2/registry";
    DockerConfig config = new DockerConfig(urlWithoutSlash, "vasya", "pupkin".toCharArray(), "account", "encrypted");
    assertThat(config.getDockerRegistryUrl()).endsWith("/");

    // now test with a url with trailing slash
    config = new DockerConfig(urlWithoutSlash.concat("/"), "vasya", "pupkin".toCharArray(), "account", "encrypted");
    assertThat(config.getDockerRegistryUrl()).endsWith("/");
  }

  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldDefaultUserNameEmpty() {
    // Normal Config
    DockerConfig dockerConfig = new DockerConfig();
    assertThat(dockerConfig.getUsername()).isNotNull().isEqualTo("");
    // Builder default
    DockerConfig dockerConfig1 = DockerConfig.builder().dockerRegistryUrl("some registry url").build();
    assertThat(dockerConfig1.getUsername()).isNotNull().isEqualTo("");
  }

  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldDockerHasCredentials() {
    DockerConfig dockerConfig = DockerConfig.builder()
                                    .dockerRegistryUrl("some registry url")
                                    .username("some username")
                                    .password("some password".toCharArray())
                                    .build();
    assertThat(dockerConfig.hasCredentials()).isTrue();
  }

  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldDockerHasNoCredentials() {
    DockerConfig dockerConfig = new DockerConfig();
    assertThat(dockerConfig.getUsername()).isNotNull().isEqualTo("");
    assertThat(dockerConfig.hasCredentials()).isFalse();
  }
}