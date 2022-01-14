package io.harness.pms.variables;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.plan.YamlOutputProperties;
import io.harness.pms.contracts.plan.YamlProperties;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.Value;

@OwnedBy(HarnessTeam.PIPELINE)
@Value
@Builder
@Schema(name = "VariableMergeServiceResponse", description = "This contains Pipeline YAML with the version.")
public class VariableMergeServiceResponse {
  String yaml;
  Map<String, VariableResponseMapValue> metadataMap;
  List<String> errorResponses;
  // List of all the expressions registered by other services
  List<ServiceExpressionProperties> serviceExpressionPropertiesList;

  @Data
  @Builder
  public static class VariableResponseMapValue {
    YamlProperties yamlProperties;
    YamlOutputProperties yamlOutputProperties;
  }

  @Data
  @Builder
  public static class ServiceExpressionProperties {
    String serviceName;
    String expression;
  }
}
