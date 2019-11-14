package software.wings.beans.config;

import static io.harness.rule.OwnerRule.UNKNOWN;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.OwnerRule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class ArtifactoryConfigTest extends CategoryTest {
  @Test
  @Owner(developers = UNKNOWN)
  @Category(UnitTests.class)
  public void shouldDefaultUserNameEmpty() {
    ArtifactoryConfig artifactoryConfig = new ArtifactoryConfig();
    assertThat(artifactoryConfig.getUsername()).isNotNull().isEqualTo("");

    // Builder default
    ArtifactoryConfig artifactoryConfig1 = ArtifactoryConfig.builder().artifactoryUrl("some registry url").build();
    assertThat(artifactoryConfig1.getUsername()).isNotNull().isEqualTo("");
  }

  @Test
  @Owner(developers = UNKNOWN)
  @Category(UnitTests.class)
  public void shouldArtifactoryHasCredentials() {
    ArtifactoryConfig artifactoryConfig = ArtifactoryConfig.builder()
                                              .artifactoryUrl("some registry url")
                                              .username("some username")
                                              .password("some password".toCharArray())
                                              .build();
    assertThat(artifactoryConfig.hasCredentials()).isTrue();
  }

  @Test
  @Owner(developers = UNKNOWN)
  @Category(UnitTests.class)
  public void shouldArtifactoryHasNoCredentials() {
    ArtifactoryConfig artifactoryConfig = new ArtifactoryConfig();
    assertThat(artifactoryConfig.getUsername()).isNotNull().isEqualTo("");
    assertThat(artifactoryConfig.hasCredentials()).isFalse();
  }
}