package io.harness.beans.environment.pod.container;

import io.harness.beans.yaml.extended.CustomSecretVariable;
import lombok.Builder;
import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.beans.ci.pod.CIContainerType;
import software.wings.beans.ci.pod.ContainerResourceParams;

import java.util.List;
import java.util.Map;

/**
 * Stores all details require to spawn container
 */

@Data
@Builder
public class ContainerDefinitionInfo {
  @NotEmpty private String name;
  @NotEmpty private ContainerImageDetails containerImageDetails;
  @NotEmpty private CIContainerType containerType;
  @NotEmpty private ContainerResourceParams containerResourceParams;
  private List<String> commands;
  private boolean isMainLiteEngine;
  private List<String> args;
  private List<Integer> ports;
  private Map<String, String> volumeToMountPath;
  Map<String, String> envVars;
  List<CustomSecretVariable> secretVariables;
  private String workingDirectory;
}