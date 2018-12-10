package software.wings.beans.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class ArtifactoryConfigTest {
  @Test
  public void shouldDefaultUserNameEmpty() {
    ArtifactoryConfig artifactoryConfig = new ArtifactoryConfig();
    assertThat(artifactoryConfig.getUsername()).isNotNull().isEqualTo("");

    // Builder default
    ArtifactoryConfig artifactoryConfig1 = ArtifactoryConfig.builder().artifactoryUrl("some registry url").build();
    assertThat(artifactoryConfig1.getUsername()).isNotNull().isEqualTo("");
  }

  @Test
  public void shouldArtifactoryHasCredentials() {
    ArtifactoryConfig artifactoryConfig = ArtifactoryConfig.builder()
                                              .artifactoryUrl("some registry url")
                                              .username("some username")
                                              .password("some password".toCharArray())
                                              .build();
    assertThat(artifactoryConfig.hasCredentials()).isTrue();
  }

  @Test
  public void shouldArtifactoryHasNoCredentials() {
    ArtifactoryConfig artifactoryConfig = new ArtifactoryConfig();
    assertThat(artifactoryConfig.getUsername()).isNotNull().isEqualTo("");
    assertThat(artifactoryConfig.hasCredentials()).isFalse();
  }
}