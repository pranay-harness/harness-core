package io.harness.cdng.environment.yaml;

import static io.harness.rule.OwnerRule.VAIBHAV_SI;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.beans.ParameterField;
import io.harness.category.element.UnitTests;
import io.harness.ng.core.common.beans.NGTag;
import io.harness.ng.core.environment.beans.EnvironmentType;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.Collections;

public class EnvironmentYamlTest extends CategoryTest {
  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void testApplyOverrides() {
    EnvironmentYaml environmentYaml = EnvironmentYaml.builder()
                                          .identifier(ParameterField.createValueField("identifer"))
                                          .name(ParameterField.createValueField("identifer"))
                                          .tags(singletonMap("key1", "value1"))
                                          .type(EnvironmentType.PreProduction)
                                          .build();

    NGTag newTag = NGTag.builder().key("new-key1").value("new-value1").build();
    EnvironmentYaml overrideEnvYaml = EnvironmentYaml.builder()
                                          .identifier(ParameterField.createValueField("new-identifer"))
                                          .name(ParameterField.createValueField("new-name"))
                                          .tags(Collections.singletonMap("new-key1", "new-value1"))
                                          .type(EnvironmentType.Production)
                                          .build();

    EnvironmentYaml overriddenEnvironment = environmentYaml.applyOverrides(overrideEnvYaml);

    assertThat(overriddenEnvironment.getName().getValue()).isEqualTo("new-name");
    assertThat(overriddenEnvironment.getIdentifier().getValue()).isEqualTo("new-identifer");
    assertThat(overriddenEnvironment.getType()).isEqualTo(EnvironmentType.Production);
    assertThat(overriddenEnvironment.getTags().containsKey("new-key1")).isTrue();
    assertThat(overriddenEnvironment.getTags().get("new-key1")).isEqualTo("new-value1");
  }
}
