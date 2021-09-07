package io.harness.beans.environment.pod.container;

import io.harness.delegate.beans.ci.pod.CIContainerType;
import io.harness.delegate.beans.ci.pod.ContainerResourceParams;
import io.harness.yaml.core.variables.SecretNGVariable;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;

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
  private boolean isHarnessManagedImage;
  private String stepIdentifier;
  private String stepName;
  private List<String> commands;
  private boolean isMainLiteEngine;
  private List<String> args;
  private List<Integer> ports;
  Map<String, String> envVars;
  Map<String, String> envVarsWithSecretRef;
  List<SecretNGVariable> secretVariables;
  private boolean privileged;
  private Integer runAsUser;
  private String imagePullPolicy;
}
