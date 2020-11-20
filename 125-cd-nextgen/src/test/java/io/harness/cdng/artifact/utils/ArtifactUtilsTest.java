package io.harness.cdng.artifact.utils;

import static io.harness.rule.OwnerRule.ARCHIT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.cdng.artifact.bean.ArtifactConfig;
import io.harness.cdng.artifact.bean.ArtifactOutcome;
import io.harness.cdng.artifact.bean.ArtifactSpecWrapper;
import io.harness.cdng.artifact.bean.DockerArtifactOutcome;
import io.harness.cdng.artifact.bean.yaml.ArtifactListConfig;
import io.harness.cdng.artifact.bean.yaml.DockerHubArtifactConfig;
import io.harness.cdng.artifact.bean.yaml.SidecarArtifact;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.Arrays;
import java.util.List;

public class ArtifactUtilsTest extends CategoryTest {
  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void shouldAppendIfNecessary() {
    StringBuilder keyBuilder = new StringBuilder("KEY_BUILDER");
    String value = "VALUE";
    String expectedString = "KEY_BUILDER:VALUE";
    ArtifactUtils.appendIfNecessary(keyBuilder, value);
    assertThat(keyBuilder.toString().equals(expectedString)).isTrue();

    keyBuilder = new StringBuilder("KEY_BUILDER");
    value = "";
    expectedString = "KEY_BUILDER";
    ArtifactUtils.appendIfNecessary(keyBuilder, value);
    assertThat(expectedString.equals(keyBuilder.toString())).isTrue();
  }

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void shouldThrowExceptionInAppend() {
    String value = "ABC";
    StringBuilder keyBuilder = null;
    String errorMessage = "Key string builder cannot be null";
    assertThatThrownBy(() -> ArtifactUtils.appendIfNecessary(keyBuilder, value))
        .isInstanceOf(InvalidRequestException.class)
        .matches(ex -> ex.getMessage().equals(errorMessage));
  }

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void shouldGenerateSameUniqueHashForSameList() {
    List<String> firstList = Arrays.asList("Value1", "Value2", "AnotherValue");
    List<String> secondList = Arrays.asList("Value2", "AnotherValue", "Value1");
    List<String> thirdList = Arrays.asList("Value2", null, "AnotherValue", "", "Value1");

    String firstListHash = ArtifactUtils.generateUniqueHashFromStringList(firstList);
    String secondListHash = ArtifactUtils.generateUniqueHashFromStringList(secondList);
    String thirdListHash = ArtifactUtils.generateUniqueHashFromStringList(thirdList);

    assertThat(firstListHash.equals(secondListHash)).isTrue();
    assertThat(thirdListHash.equals(secondListHash)).isTrue();
  }

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testIsPrimaryArtifact() {
    DockerHubArtifactConfig config = DockerHubArtifactConfig.builder().primaryArtifact(true).build();
    boolean primaryArtifact = config.isPrimaryArtifact();
    assertThat(primaryArtifact).isTrue();
    config = DockerHubArtifactConfig.builder().primaryArtifact(false).build();
    primaryArtifact = config.isPrimaryArtifact();
    assertThat(primaryArtifact).isFalse();

    ArtifactOutcome artifactOutcome = DockerArtifactOutcome.builder().primaryArtifact(true).build();
    primaryArtifact = artifactOutcome.isPrimaryArtifact();
    assertThat(primaryArtifact).isTrue();
    artifactOutcome = DockerArtifactOutcome.builder().primaryArtifact(false).build();
    primaryArtifact = artifactOutcome.isPrimaryArtifact();
    assertThat(primaryArtifact).isFalse();
  }

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testGetArtifactKey() {
    DockerHubArtifactConfig artifactConfig =
        DockerHubArtifactConfig.builder().primaryArtifact(true).identifier("ARTIFACT1").build();
    String artifactKey = ArtifactUtils.getArtifactKey(artifactConfig);
    assertThat(artifactKey).isEqualTo("ARTIFACT1");
    artifactConfig = DockerHubArtifactConfig.builder().primaryArtifact(false).identifier("ARTIFACT1").build();
    artifactKey = ArtifactUtils.getArtifactKey(artifactConfig);
    assertThat(artifactKey).isEqualTo("sidecars.ARTIFACT1");
  }

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testConvertArtifactListConfig() {
    DockerHubArtifactConfig primaryArtifact =
        DockerHubArtifactConfig.builder().primaryArtifact(true).identifier("ARTIFACT1").build();
    DockerHubArtifactConfig sidecarArtifact =
        DockerHubArtifactConfig.builder().primaryArtifact(false).identifier("ARTIFACT2").build();
    ArtifactListConfig artifactListConfig =
        ArtifactListConfig.builder()
            .primary(ArtifactSpecWrapper.builder().artifactConfig(primaryArtifact).build())
            .sidecar(SidecarArtifact.builder().artifactConfig(sidecarArtifact).build())
            .build();
    List<ArtifactConfig> artifactsList = ArtifactUtils.convertArtifactListIntoArtifacts(artifactListConfig);
    assertThat(artifactsList).containsOnly(primaryArtifact, sidecarArtifact);
  }
}
