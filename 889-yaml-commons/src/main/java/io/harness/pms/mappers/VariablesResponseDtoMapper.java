/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.pms.mappers;

import io.harness.pms.contracts.service.ServiceExpressionPropertiesProto;
import io.harness.pms.contracts.service.VariableMergeResponseProto;
import io.harness.pms.contracts.service.VariableResponseMapValueProto;
import io.harness.pms.variables.VariableMergeServiceResponse;
import io.harness.pms.variables.VariableMergeServiceResponse.ServiceExpressionProperties;
import io.harness.pms.variables.VariableMergeServiceResponse.VariableResponseMapValue;

import java.util.Map;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;

@UtilityClass
public class VariablesResponseDtoMapper {
  public VariableMergeResponseProto toProto(VariableMergeServiceResponse variableMergeServiceResponse) {
    Map<String, VariableResponseMapValueProto> metadataMap =
        variableMergeServiceResponse.getMetadataMap().entrySet().stream().collect(
            Collectors.toMap(Map.Entry::getKey, e -> toVariablesResponseMapProto(e.getValue())));
    return VariableMergeResponseProto.newBuilder()
        .setYaml(variableMergeServiceResponse.getYaml())
        .putAllMetadataMap(metadataMap)
        .addAllErrorResponses(variableMergeServiceResponse.getErrorResponses())
        .addAllServiceExpressionPropertiesList(variableMergeServiceResponse.getServiceExpressionPropertiesList()
                                                   .stream()
                                                   .map(VariablesResponseDtoMapper::toServiceExpressionProto)
                                                   .collect(Collectors.toList()))
        .build();
  }

  public VariableMergeServiceResponse toDto(VariableMergeResponseProto variableMergeResponseProto) {
    Map<String, VariableResponseMapValue> metadataMap =
        variableMergeResponseProto.getMetadataMapMap().entrySet().stream().collect(
            Collectors.toMap(Map.Entry::getKey, e -> toVariablesResponseMapValue(e.getValue())));
    return VariableMergeServiceResponse.builder()
        .yaml(variableMergeResponseProto.getYaml())
        .errorResponses(variableMergeResponseProto.getErrorResponsesList())
        .metadataMap(metadataMap)
        .serviceExpressionPropertiesList(variableMergeResponseProto.getServiceExpressionPropertiesListList()
                                             .stream()
                                             .map(VariablesResponseDtoMapper::toServiceExpressionProperties)
                                             .collect(Collectors.toList()))
        .build();
  }

  private VariableResponseMapValueProto toVariablesResponseMapProto(VariableResponseMapValue variableResponseMapValue) {
    return VariableResponseMapValueProto.newBuilder()
        .setYamlProperties(variableResponseMapValue.getYamlProperties())
        .setYamlOutputProperties(variableResponseMapValue.getYamlOutputProperties())
        .build();
  }

  private ServiceExpressionPropertiesProto toServiceExpressionProto(
      ServiceExpressionProperties serviceExpressionProperties) {
    return ServiceExpressionPropertiesProto.newBuilder()
        .setServiceName(serviceExpressionProperties.getServiceName())
        .setExpression(serviceExpressionProperties.getExpression())
        .build();
  }

  private VariableResponseMapValue toVariablesResponseMapValue(
      VariableResponseMapValueProto variableResponseMapValueProto) {
    return VariableResponseMapValue.builder()
        .yamlOutputProperties(variableResponseMapValueProto.getYamlOutputProperties())
        .yamlProperties(variableResponseMapValueProto.getYamlProperties())
        .build();
  }

  private ServiceExpressionProperties toServiceExpressionProperties(
      ServiceExpressionPropertiesProto serviceExpressionPropertiesProto) {
    return ServiceExpressionProperties.builder()
        .serviceName(serviceExpressionPropertiesProto.getServiceName())
        .expression(serviceExpressionPropertiesProto.getExpression())
        .build();
  }
}
