package io.harness.cdng.artifact.utils;

import static io.harness.rule.OwnerRule.ARCHIT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;

import io.harness.category.element.UnitTests;
import io.harness.cdng.artifact.bean.artifactsource.DockerArtifactSourceAttributes;
import io.harness.cdng.artifact.delegate.task.ArtifactTaskParameters;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;

import java.util.Arrays;
import java.util.List;

public class ArtifactUtilsTest extends WingsBaseTest {
  private final DockerArtifactSourceAttributes dockerArtifactSourceAttributes =
      DockerArtifactSourceAttributes.builder()
          .dockerhubConnector("DOCKER_CONNECTOR")
          .imagePath("DOCKER_IMAGE")
          .tag("tag")
          .tagRegex("tagRegex")
          .build();

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
  public void shouldGetArtifactTaskParametersForDocker() {
    ArtifactTaskParameters taskParameters =
        ArtifactUtils.getArtifactTaskParameters(ACCOUNT_ID, dockerArtifactSourceAttributes);
    assertThat(taskParameters.getAccountId()).isEqualTo(ACCOUNT_ID);
    assertThat(taskParameters.getAttributes()).isInstanceOf(DockerArtifactSourceAttributes.class);

    DockerArtifactSourceAttributes attributes = (DockerArtifactSourceAttributes) taskParameters.getAttributes();
    assertThat(attributes.getDockerhubConnector()).isEqualTo("DOCKER_CONNECTOR");
    assertThat(attributes.getImagePath()).isEqualTo("DOCKER_IMAGE");
    assertThat(attributes.getTag()).isEqualTo("tag");
    assertThat(attributes.getTagRegex()).isEqualTo("tagRegex");
  }
}