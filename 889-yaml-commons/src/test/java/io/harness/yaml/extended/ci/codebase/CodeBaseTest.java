package io.harness.yaml.extended.ci.codebase;

import static io.harness.rule.OwnerRule.ALEKSANDAR;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.utils.YamlPipelineUtils;
import io.harness.yaml.extended.ci.codebase.impl.BranchBuildSpec;

import java.io.IOException;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class CodeBaseTest extends CategoryTest {
  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void shouldDeserializeCodebase() throws IOException {
    String yaml = "connectorRef: springboot\n"
        + "repoName: abc\n"
        + "build:\n"
        + "  type: branch\n"
        + "  spec:\n"
        + "    branch: main";
    CodeBase actual = YamlPipelineUtils.read(yaml, CodeBase.class);
    assertThat(actual.getConnectorRef()).isEqualTo("springboot");
    assertThat(actual.getRepoName()).isEqualTo("abc");
    assertThat(actual.getBuild().getValue().getType()).isEqualTo(BuildType.BRANCH);
    assertThat(((BranchBuildSpec) actual.getBuild().getValue().getSpec()).getBranch().getValue()).isEqualTo("main");
  }
}