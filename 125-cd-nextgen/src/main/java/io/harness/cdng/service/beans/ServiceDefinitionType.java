package io.harness.cdng.service.beans;

import io.harness.beans.ExecutionStrategyType;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import io.fabric8.utils.Lists;
import java.util.Arrays;
import java.util.List;

public enum ServiceDefinitionType {
  @JsonProperty(ServiceSpecType.SSH) SSH("Ssh", Lists.newArrayList(ExecutionStrategyType.BASIC), ServiceSpecType.SSH),
  @JsonProperty(ServiceSpecType.KUBERNETES)
  KUBERNETES("Kubernetes",
      Lists.newArrayList(ExecutionStrategyType.ROLLING, ExecutionStrategyType.BLUE_GREEN, ExecutionStrategyType.CANARY),
      ServiceSpecType.KUBERNETES),
  @JsonProperty(ServiceSpecType.ECS)
  ECS("Ecs",
      Lists.newArrayList(ExecutionStrategyType.BASIC, ExecutionStrategyType.BLUE_GREEN, ExecutionStrategyType.CANARY),
      ServiceSpecType.ECS),
  @JsonProperty(ServiceSpecType.HELM)
  HELM("Helm", Lists.newArrayList(ExecutionStrategyType.BASIC), ServiceSpecType.HELM),
  @JsonProperty(ServiceSpecType.PCF)
  PCF("Pcf",
      Lists.newArrayList(ExecutionStrategyType.BASIC, ExecutionStrategyType.BLUE_GREEN, ExecutionStrategyType.CANARY),
      ServiceSpecType.PCF);

  private String displayName;
  private String yamlName;
  private List<ExecutionStrategyType> executionStrategies;

  @JsonCreator
  public static ServiceDefinitionType getServiceDefinitionType(@JsonProperty("type") String yamlName) {
    for (ServiceDefinitionType serviceDefinitionType : ServiceDefinitionType.values()) {
      if (serviceDefinitionType.yamlName.equalsIgnoreCase(yamlName)) {
        return serviceDefinitionType;
      }
    }
    throw new IllegalArgumentException(String.format(
        "Invalid value:%s, the expected values are: %s", yamlName, Arrays.toString(ServiceDefinitionType.values())));
  }

  ServiceDefinitionType(String displayName, List<ExecutionStrategyType> executionStrategies, String yamlName) {
    this.displayName = displayName;
    this.executionStrategies = executionStrategies;
    this.yamlName = yamlName;
  }

  public static List<ExecutionStrategyType> getExecutionStrategies(ServiceDefinitionType serviceDefinitionType) {
    return serviceDefinitionType.executionStrategies;
  }

  @JsonValue
  public String getYamlName() {
    return yamlName;
  }

  public static ServiceDefinitionType fromString(final String s) {
    return ServiceDefinitionType.getServiceDefinitionType(s);
  }
}
