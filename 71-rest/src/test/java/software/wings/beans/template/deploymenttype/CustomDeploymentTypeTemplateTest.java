package software.wings.beans.template.deploymenttype;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableMap;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;

public class CustomDeploymentTypeTemplateTest extends WingsBaseTest {
  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void but() {
    CustomDeploymentTypeTemplate template = CustomDeploymentTypeTemplate.builder()
                                                .fetchInstanceScript("echo a")
                                                .hostAttributes(ImmutableMap.of("k", "v"))
                                                .hostObjectArrayPath("hosts")
                                                .build();
    assertThat(template.but().build()).isEqualTo(template);
  }
}
