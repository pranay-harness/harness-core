package software.wings.beans;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import software.wings.WingsBaseTest;

public class DockerConfigTest extends WingsBaseTest {
  @Test
  // test with a URL that doesn't end on a / and make sure a / gets added
  public void testConstructorWithUrlWithoutSlash() {
    String urlWithoutSlash = "http://some.docker.com/v2/registry";
    DockerConfig config = new DockerConfig(urlWithoutSlash, "vasya", "pupkin".toCharArray(), "account", "encrypted");
    assertThat(config.getDockerRegistryUrl()).endsWith("/");

    // now test with a url with trailing slash
    config = new DockerConfig(urlWithoutSlash.concat("/"), "vasya", "pupkin".toCharArray(), "account", "encrypted");
    assertThat(config.getDockerRegistryUrl()).endsWith("/");
  }
}