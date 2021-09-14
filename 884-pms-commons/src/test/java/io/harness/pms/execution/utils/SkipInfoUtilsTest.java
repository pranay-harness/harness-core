/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.pms.execution.utils;

import static io.harness.rule.OwnerRule.GARVIT;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.pms.yaml.ParameterField;
import io.harness.rule.Owner;

import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(HarnessTeam.PIPELINE)
public class SkipInfoUtilsTest extends CategoryTest {
  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testGetSkipCondition() {
    assertThat(SkipInfoUtils.getSkipCondition(null)).isNull();
    assertThat(SkipInfoUtils.getSkipCondition(ParameterField.ofNull())).isNull();
    assertThat(SkipInfoUtils.getSkipCondition(ParameterField.createValueField("a"))).isEqualTo("a");
    assertThat(SkipInfoUtils.getSkipCondition(ParameterField.createExpressionField(true, "<+a>", null, true)))
        .isEqualTo("<+a>");
  }
}
