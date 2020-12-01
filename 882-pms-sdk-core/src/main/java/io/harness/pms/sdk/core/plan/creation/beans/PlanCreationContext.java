package io.harness.pms.sdk.core.plan.creation.beans;

import io.harness.pms.yaml.YamlField;

import lombok.Value;

@Value
public class PlanCreationContext {
  YamlField currentField;
}
