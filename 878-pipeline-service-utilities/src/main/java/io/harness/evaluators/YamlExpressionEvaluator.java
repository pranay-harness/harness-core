/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.evaluators;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.expression.EngineExpressionEvaluator;
import io.harness.pms.yaml.YAMLFieldNameConstants;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlUtils;

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.List;

@OwnedBy(HarnessTeam.CDC)
public class YamlExpressionEvaluator extends EngineExpressionEvaluator {
  private final String yaml;
  private final String fqnPathToElement;

  public YamlExpressionEvaluator(String yaml, String fqnPathToElement) {
    super(null);
    this.yaml = yaml;
    this.fqnPathToElement = fqnPathToElement;
  }

  @Override
  protected void initialize() {
    super.initialize();
    addToContext("__yamlExpression",
        YamlExpressionFunctor.builder()
            .rootYamlField(getPipelineYamlField())
            .fqnPathToElement(fqnPathToElement)
            .build());
  }

  @Override
  protected List<String> fetchPrefixes() {
    ImmutableList.Builder<String> listBuilder = ImmutableList.builder();
    return listBuilder.add("__yamlExpression").addAll(super.fetchPrefixes()).build();
  }

  private YamlField getPipelineYamlField() {
    try {
      YamlField yamlField = YamlUtils.readTree(yaml);
      return yamlField.getNode().getField(YAMLFieldNameConstants.PIPELINE);
    } catch (IOException e) {
      throw new InvalidRequestException("Not valid yaml passed.");
    }
  }
}
