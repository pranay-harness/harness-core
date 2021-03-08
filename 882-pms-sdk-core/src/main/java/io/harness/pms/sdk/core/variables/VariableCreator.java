package io.harness.pms.sdk.core.variables;

import io.harness.pms.sdk.core.variables.beans.VariableCreationContext;
import io.harness.pms.sdk.core.variables.beans.VariableCreationResponse;
import io.harness.pms.yaml.YamlField;

import java.util.Map;
import java.util.Set;

public interface VariableCreator {
  Map<String, Set<String>> getSupportedTypes();
  VariableCreationResponse createVariablesForField(VariableCreationContext ctx, YamlField field);
}
