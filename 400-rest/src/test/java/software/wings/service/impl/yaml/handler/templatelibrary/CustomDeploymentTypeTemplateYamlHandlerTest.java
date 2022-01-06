/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.yaml.handler.templatelibrary;

import static software.wings.beans.yaml.ChangeContext.Builder.aChangeContext;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;

import software.wings.WingsBaseTest;
import software.wings.beans.template.Template;
import software.wings.beans.template.deploymenttype.CustomDeploymentTypeTemplate;
import software.wings.beans.yaml.ChangeContext;
import software.wings.yaml.templatelibrary.CustomDeploymentTypeTemplateYaml;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class CustomDeploymentTypeTemplateYamlHandlerTest extends WingsBaseTest {
  @Inject private CustomDeploymentTypeTemplateYamlHandler yamlHandler;

  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void setBaseTemplate() {
    Template template = Template.builder().build();
    ChangeContext changeContext = aChangeContext()
                                      .withYaml(CustomDeploymentTypeTemplateYaml.builder()
                                                    .hostObjectArrayPath("Instances")
                                                    .hostAttributes(ImmutableMap.of("k", "v"))
                                                    .fetchInstanceScript("echo abc")
                                                    .build())
                                      .build();

    yamlHandler.setBaseTemplate(template, changeContext, null);

    CustomDeploymentTypeTemplate customDeploymentTypeTemplate =
        (CustomDeploymentTypeTemplate) template.getTemplateObject();

    assertThat(customDeploymentTypeTemplate.getFetchInstanceScript()).isEqualTo("echo abc");
    assertThat(customDeploymentTypeTemplate.getHostObjectArrayPath()).isEqualTo("Instances");
    assertThat(customDeploymentTypeTemplate.getHostAttributes()).isEqualTo(ImmutableMap.of("k", "v"));
  }

  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void toYaml() {
    Template template = Template.builder()
                            .templateObject(CustomDeploymentTypeTemplate.builder()
                                                .hostObjectArrayPath("Instances")
                                                .hostAttributes(ImmutableMap.of("k", "v"))
                                                .fetchInstanceScript("echo abc")
                                                .build())
                            .build();
    final CustomDeploymentTypeTemplateYaml yaml = yamlHandler.toYaml(template, "appId");

    assertThat(yaml.getFetchInstanceScript()).isEqualTo("echo abc");
    assertThat(yaml.getHostObjectArrayPath()).isEqualTo("Instances");
    assertThat(yaml.getHostAttributes()).isEqualTo(ImmutableMap.of("k", "v"));
  }
}
