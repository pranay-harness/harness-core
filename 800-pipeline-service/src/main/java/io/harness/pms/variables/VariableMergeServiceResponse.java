/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.pms.variables;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.plan.YamlOutputProperties;
import io.harness.pms.contracts.plan.YamlProperties;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.Value;

@OwnedBy(HarnessTeam.PIPELINE)
@Value
@Builder
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
