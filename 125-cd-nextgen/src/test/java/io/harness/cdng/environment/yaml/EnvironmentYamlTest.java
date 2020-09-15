package io.harness.cdng.environment.yaml;

import static io.harness.rule.OwnerRule.VAIBHAV_SI;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.beans.ParameterField;
import io.harness.category.element.UnitTests;
import io.harness.ng.core.common.beans.Tag;
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
    EnvironmentYaml environmentYaml =
        EnvironmentYaml.builder()
            .identifier(ParameterField.createValueField("identifer"))
            .name(ParameterField.createValueField("identifer"))
            .tags(Collections.singletonList(Tag.builder().key("key1").value("value1").build()))
            .type(EnvironmentType.PreProduction)
            .build();

    Tag newTag = Tag.builder().key("new-key1").value("new-value1").build();
    EnvironmentYaml overrideEnvYaml = EnvironmentYaml.builder()
                                          .identifier(ParameterField.createValueField("new-identifer"))
                                          .name(ParameterField.createValueField("new-name"))
                                          .tags(Collections.singletonList(newTag))
                                          .type(EnvironmentType.Production)
                                          .build();

    EnvironmentYaml overriddenEnvironment = environmentYaml.applyOverrides(overrideEnvYaml);

    assertThat(overriddenEnvironment.getName().getValue()).isEqualTo("new-name");
    assertThat(overriddenEnvironment.getIdentifier().getValue()).isEqualTo("new-identifer");
    assertThat(overriddenEnvironment.getType()).isEqualTo(EnvironmentType.Production);
    assertThat(overriddenEnvironment.getTags().get(0)).isEqualTo(newTag);
  }
}