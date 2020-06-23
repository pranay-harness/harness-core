package io.harness.beans.environment.pod.container;

import lombok.Builder;
import lombok.Data;
import lombok.Value;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.beans.ci.pod.CIContainerType;
import software.wings.beans.ci.pod.ContainerResourceParams;

import java.util.List;
import java.util.Map;

/**
 * Stores all details require to spawn container
 */

@Data
@Value
@Builder
public class ContainerDefinitionInfo {
  @NotEmpty private String name;
  @NotEmpty private ContainerImageDetails containerImageDetails;
  @NotEmpty private CIContainerType containerType;
  @NotEmpty private ContainerResourceParams containerResourceParams;
  private List<String> commands;
  private List<String> args;
  private List<Integer> ports;
  private Map<String, String> volumeToMountPath;
}