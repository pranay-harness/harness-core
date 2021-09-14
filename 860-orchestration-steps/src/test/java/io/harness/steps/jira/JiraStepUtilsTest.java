/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.steps.jira;

import static io.harness.rule.OwnerRule.GARVIT;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.pms.yaml.ParameterField;
import io.harness.rule.Owner;

import com.google.common.collect.ImmutableMap;
import java.util.Collections;
import java.util.Map;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(HarnessTeam.PIPELINE)
public class JiraStepUtilsTest extends CategoryTest {
  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testProcessJiraFieldsInParameters() {
    assertThat(JiraStepUtils.processJiraFieldsInParameters(null)).isEmpty();
    assertThat(JiraStepUtils.processJiraFieldsInParameters(Collections.emptyMap())).isEmpty();

    assertThatThrownBy(()
                           -> JiraStepUtils.processJiraFieldsInParameters(ImmutableMap.of("a",
                               ParameterField.createValueField("va"), "b", ParameterField.createValueField(null), "c",
                               ParameterField.createExpressionField(true, "<+abc>", null, true))))
        .hasMessage("Field [c] has invalid jira field value");

    Map<String, String> fields = JiraStepUtils.processJiraFieldsInParameters(
        ImmutableMap.of("a", ParameterField.createValueField("va"), "b", ParameterField.createValueField(null)));
    assertThat(fields.size()).isEqualTo(1);
    assertThat(fields.get("a")).isEqualTo("va");
  }
}
