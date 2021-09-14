/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.evaluators;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.rule.OwnerRule.ARCHIT;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import com.google.api.client.util.Charsets;
import com.google.common.io.Resources;
import java.io.IOException;
import java.net.URL;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(CDC)
public class YamlExpressionEvaluatorTest extends CategoryTest {
  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testExpressionRender() throws IOException {
    ClassLoader classLoader = this.getClass().getClassLoader();
    final URL testFile = classLoader.getResource("inputset/pipeline.yaml");
    String yamlContent = Resources.toString(testFile, Charsets.UTF_8);
    YamlExpressionEvaluator yamlExpressionEvaluator = new YamlExpressionEvaluator(
        yamlContent, "pipeline.stages.stage1.spec.serviceConfig.serviceDefinition.spec.artifacts.primary.spec.tag");

    // Partial expression
    String renderExpression = yamlExpressionEvaluator.renderExpression("<+serviceConfig.service.name>");
    assertThat(renderExpression).isEqualTo("service1");

    // Partial expression
    renderExpression =
        yamlExpressionEvaluator.renderExpression("<+serviceConfig.serviceDefinition.spec.variables.serviceN1>");
    assertThat(renderExpression).isEqualTo("stringValue3");

    // Partial expression
    renderExpression = yamlExpressionEvaluator.renderExpression(
        "<+stages.stage1.spec.serviceConfig.serviceDefinition.spec.artifacts.sidecars.sidecar1.spec.connectorRef>",
        true);
    assertThat(renderExpression).isEqualTo("myDocker2");

    // FQN expression
    renderExpression = yamlExpressionEvaluator.renderExpression(
        "<+pipeline.stages.stage1.spec.serviceConfig.serviceDefinition.spec.artifacts.sidecars.sidecar1.spec.connectorRef>",
        true);
    assertThat(renderExpression).isEqualTo("myDocker2");
  }
}
