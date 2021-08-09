package io.harness.pms.variables;

import io.harness.pms.contracts.plan.YamlOutputProperties;
import io.harness.pms.contracts.plan.YamlProperties;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.Value;

@Value
@Builder
public class VariableMergeServiceResponse {
  String yaml;
  Map<String, VariableResponseMapValue> metadataMap;
  List<String> errorResponses;

  @Data
  @Builder
  public static class VariableResponseMapValue {
    YamlProperties yamlProperties;
    YamlOutputProperties yamlOutputProperties;
  }
}
