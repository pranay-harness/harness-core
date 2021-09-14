/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.beans.template.deploymenttype;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;

import software.wings.WingsBaseTest;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.junit.experimental.categories.Category;

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
