package io.harness.delegate.beans.ci.pod;

import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public abstract class ContainerParams {
  private String name;
  private ImageDetailsWithConnector imageDetailsWithConnector;
  private List<String> commands;
  private List<String> args;
  private String workingDir;
  private List<Integer> ports;
  private Map<String, String> envVars;
  private Map<String, SecretVarParams> secretEnvVars;
  private Map<String, SecretVolumeParams> secretVolumes;
  private Map<String, String> volumeToMountPath;
  private ContainerResourceParams containerResourceParams;
  private ContainerSecrets containerSecrets;

  public abstract ContainerParams.Type getType();

  public enum Type {
    K8, // Generic K8 container configuration
    K8_GIT_CLONE, // K8 container configuration to clone a git repository
  }
}
