package io.harness.artifact;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class ArtifactUtilitiesTest {
  @Test
  @Category(UnitTests.class)
  public void shouldExtractNexusRegistryUrl() {
    assertThat(ArtifactUtilities.getNexusRegistryUrl("https://nexus.harness.io", "5000", null))
        .isEqualTo("https://nexus.harness.io:5000");

    assertThat(ArtifactUtilities.getNexusRegistryUrl("https://nexus.harness.io", null, null))
        .isEqualTo("https://nexus.harness.io");

    assertThat(ArtifactUtilities.getNexusRegistryUrl("https://nexus.harness.io", null, "nexus3.harness.io"))
        .isEqualTo("https://nexus3.harness.io");

    assertThat(ArtifactUtilities.getNexusRegistryUrl("https://nexus.harness.io", null, "http://nexus3.harness.io"))
        .isEqualTo("http://nexus3.harness.io");

    assertThat(ArtifactUtilities.getNexusRegistryUrl("https://nexus.harness.io", null, "nexus3.harness.io:5000"))
        .isEqualTo("https://nexus3.harness.io:5000");

    assertThat(ArtifactUtilities.getNexusRegistryUrl("https://nexus.harness.io", null, "http://nexus3.harness.io:5000"))
        .isEqualTo("http://nexus3.harness.io:5000");
  }

  @Test
  @Category(UnitTests.class)
  public void shouldExtractNexusRepositoryName() {
    assertThat(
        ArtifactUtilities.getNexusRepositoryName("https://nexus.harness.io", "5000", null, "harness/todolist-sample"))
        .isEqualTo("nexus.harness.io:5000/harness/todolist-sample");

    assertThat(
        ArtifactUtilities.getNexusRepositoryName("https://nexus.harness.io", null, null, "harness/todolist-sample"))
        .isEqualTo("nexus.harness.io/harness/todolist-sample");

    assertThat(ArtifactUtilities.getNexusRepositoryName(
                   "https://nexus.harness.io", null, "nexus3.harness.io", "harness/todolist-sample"))
        .isEqualTo("nexus3.harness.io/harness/todolist-sample");

    assertThat(ArtifactUtilities.getNexusRepositoryName(
                   "https://nexus.harness.io", null, "http://nexus3.harness.io", "harness/todolist-sample"))
        .isEqualTo("nexus3.harness.io/harness/todolist-sample");

    assertThat(ArtifactUtilities.getNexusRepositoryName(
                   "https://nexus.harness.io", null, "nexus3.harness.io:5000", "harness/todolist-sample"))
        .isEqualTo("nexus3.harness.io:5000/harness/todolist-sample");

    assertThat(ArtifactUtilities.getNexusRepositoryName(
                   "https://nexus.harness.io", null, "http://nexus3.harness.io:5000", "harness/todolist-sample"))
        .isEqualTo("nexus3.harness.io:5000/harness/todolist-sample");
  }
}
