package io.harness.app.yaml;

import static io.harness.rule.OwnerRule.ALEKSANDAR;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.app.impl.CIManagerTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class YAMLTest extends CIManagerTest {
  private String yamlString = "dummyYamlString";

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void getPipelineYAML() {
    YAML yaml = YAML.builder().pipelineYAML(yamlString).build();
    assertThat(yaml.getPipelineYAML()).isEqualTo(yamlString);
  }

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void testEquals() {
    YAML yaml = YAML.builder().pipelineYAML(yamlString).build();
    YAML otherYaml = YAML.builder().pipelineYAML(yamlString).build();
    assertThat(yaml.equals(otherYaml)).isTrue();
  }

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void testHashCode() {
    YAML yaml = YAML.builder().pipelineYAML(yamlString).build();
    YAML otherYaml = YAML.builder().pipelineYAML(yamlString).build();
    assertThat(yaml.hashCode()).isEqualTo(otherYaml.hashCode());
  }
}