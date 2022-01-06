/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.template;

import static software.wings.beans.Variable.VariableBuilder.aVariable;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.TEMPLATE_NAME;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;

import software.wings.WingsBaseTest;
import software.wings.beans.template.Template;
import software.wings.beans.template.TemplateType;
import software.wings.beans.template.deploymenttype.CustomDeploymentTypeTemplate;
import software.wings.utils.WingsTestConstants;

import com.google.common.collect.ImmutableMap;
import java.util.Arrays;
import java.util.HashMap;
import org.junit.Test;
import org.junit.experimental.categories.Category;

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

  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void shouldTrimFields() {
    Template processedTemplate;
    Template template;
    template = Template.builder()
                   .accountId(ACCOUNT_ID)
                   .name(WingsTestConstants.TEMPLATE_NAME)
                   .templateObject(CustomDeploymentTypeTemplate.builder().build())
                   .variables(Arrays.asList(aVariable().name("k").value("v").build()))
                   .build();
    processedTemplate = processor.process(template);

    assertThat(processedTemplate).isEqualTo(template);

    template.setTemplateObject(CustomDeploymentTypeTemplate.builder()
                                   .hostObjectArrayPath("   path   ")
                                   .hostAttributes(new HashMap<String, String>() {
                                     {
                                       put("k1", " v1 ");
                                       put("k2", "v2 ");
                                     }
                                   })
                                   .build());
    processedTemplate = processor.process(template);
    assertThat(processedTemplate.getAccountId()).isEqualTo(ACCOUNT_ID);
    assertThat(processedTemplate.getName()).isEqualTo(TEMPLATE_NAME);
    assertThat(processedTemplate.getVariables()).containsExactly(aVariable().name("k").value("v").build());
    assertThat(processedTemplate.getTemplateObject())
        .isEqualTo(CustomDeploymentTypeTemplate.builder()
                       .hostObjectArrayPath("path")
                       .hostAttributes(new HashMap<String, String>() {
                         {
                           put("k1", "v1");
                           put("k2", "v2");
                         }
                       })
                       .build());
  }
}
