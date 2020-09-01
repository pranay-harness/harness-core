package io.harness.cdng.service.beans;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import io.fabric8.utils.Lists;
import io.harness.beans.ExecutionStrategyType;

import java.util.Arrays;
import java.util.List;

public enum ServiceDefinitionType {
  @JsonProperty("Ssh") SSH("Ssh", Lists.newArrayList(ExecutionStrategyType.BASIC)),
  @JsonProperty("Kubernetes")
  KUBERNETES("Kubernetes",
      Lists.newArrayList(
          ExecutionStrategyType.ROLLING, ExecutionStrategyType.BLUE_GREEN, ExecutionStrategyType.CANARY)),
  @JsonProperty("Ecs")
  ECS("Ecs",
      Lists.newArrayList(ExecutionStrategyType.BASIC, ExecutionStrategyType.BLUE_GREEN, ExecutionStrategyType.CANARY)),
  @JsonProperty("Helm") HELM("Helm", Lists.newArrayList(ExecutionStrategyType.BASIC)),
  @JsonProperty("Pcf")
  PCF("Pcf",
      Lists.newArrayList(ExecutionStrategyType.BASIC, ExecutionStrategyType.BLUE_GREEN, ExecutionStrategyType.CANARY));

  private String displayName;
  private List<ExecutionStrategyType> executionStrategies;

  @JsonCreator
  public static ServiceDefinitionType getServiceDefinitionType(@JsonProperty("type") String displayName) {
    for (ServiceDefinitionType serviceDefinitionType : ServiceDefinitionType.values()) {
      if (serviceDefinitionType.displayName.equalsIgnoreCase(displayName)) {
        return serviceDefinitionType;
      }
    }
    throw new IllegalArgumentException(String.format(
        "Invalid value:%s, the expected values are: %s", displayName, Arrays.toString(ServiceDefinitionType.values())));
  }

  ServiceDefinitionType(String displayName, List<ExecutionStrategyType> executionStrategies) {
    this.displayName = displayName;
    this.executionStrategies = executionStrategies;
  }

  public static List<ExecutionStrategyType> getExecutionStrategies(ServiceDefinitionType serviceDefinitionType) {
    return serviceDefinitionType.executionStrategies;
  }

  @JsonValue
  public String getDisplayName() {
    return displayName;
  }

  public static ServiceDefinitionType fromString(final String s) {
    return ServiceDefinitionType.getServiceDefinitionType(s);
  }
}
