package software.wings.service.impl.template;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableMap;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;
import software.wings.beans.template.TemplateType;
import software.wings.beans.template.deploymenttype.CustomDeploymentTypeTemplate;

public class CustomDeploymentTypeProcessorTest extends WingsBaseTest {
  private CustomDeploymentTypeProcessor processor = new CustomDeploymentTypeProcessor();

  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void getTemplateType() {
    assertThat(processor.getTemplateType()).isEqualTo(TemplateType.CUSTOM_DEPLOYMENT_TYPE);
  }

  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void checkTemplateDetailsChanged() {
    CustomDeploymentTypeTemplate newTemplate;
    CustomDeploymentTypeTemplate oldTemplate;

    oldTemplate = CustomDeploymentTypeTemplate.builder().build();
    assertThat(processor.checkTemplateDetailsChanged(oldTemplate, oldTemplate)).isFalse();

    newTemplate = CustomDeploymentTypeTemplate.builder().fetchInstanceScript("abc").build();
    assertThat(processor.checkTemplateDetailsChanged(oldTemplate, newTemplate)).isTrue();

    oldTemplate = newTemplate;
    newTemplate = CustomDeploymentTypeTemplate.builder().fetchInstanceScript("abc-foo").build();
    assertThat(processor.checkTemplateDetailsChanged(oldTemplate, newTemplate)).isTrue();

    oldTemplate = newTemplate;
    newTemplate = CustomDeploymentTypeTemplate.builder().hostAttributes(ImmutableMap.of("k1", "v1")).build();
    assertThat(processor.checkTemplateDetailsChanged(oldTemplate, newTemplate)).isTrue();

    oldTemplate = newTemplate;
    newTemplate = CustomDeploymentTypeTemplate.builder()
                      .hostAttributes(ImmutableMap.of("k1", "v1"))
                      .hostObjectArrayPath("Instances")
                      .build();
    assertThat(processor.checkTemplateDetailsChanged(oldTemplate, newTemplate)).isTrue();
    assertThat(processor.checkTemplateDetailsChanged(newTemplate, newTemplate)).isFalse();

    oldTemplate = newTemplate;
    newTemplate = CustomDeploymentTypeTemplate.builder()
                      .hostAttributes(ImmutableMap.of("k1", ""))
                      .hostObjectArrayPath("Instances")
                      .build();
    assertThat(processor.checkTemplateDetailsChanged(oldTemplate, newTemplate)).isTrue();
    assertThat(processor.checkTemplateDetailsChanged(newTemplate, newTemplate)).isFalse();

    oldTemplate = newTemplate;
    newTemplate = CustomDeploymentTypeTemplate.builder()
                      .hostAttributes(ImmutableMap.of("k1", ""))
                      .fetchInstanceScript("curl")
                      .hostObjectArrayPath("Instances")
                      .build();
    assertThat(processor.checkTemplateDetailsChanged(oldTemplate, newTemplate)).isTrue();
    assertThat(processor.checkTemplateDetailsChanged(newTemplate, newTemplate)).isFalse();

    oldTemplate = newTemplate;
    newTemplate = CustomDeploymentTypeTemplate.builder()
                      .hostAttributes(ImmutableMap.of("k1", ""))
                      .fetchInstanceScript("curl ${url}")
                      .hostObjectArrayPath("Instances")
                      .build();
    assertThat(processor.checkTemplateDetailsChanged(oldTemplate, newTemplate)).isTrue();
    assertThat(processor.checkTemplateDetailsChanged(newTemplate, newTemplate)).isFalse();

    assertThat(processor.checkTemplateDetailsChanged(newTemplate, CustomDeploymentTypeTemplate.builder().build()))
        .isTrue();
  }
}