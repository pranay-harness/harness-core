package io.harness.delegate.beans.ci.pod;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode(callSuper = true)
public class CIK8ContainerParams extends ContainerParams {
  private CIContainerType containerType;

  @Builder
  public CIK8ContainerParams(CIContainerType containerType, String name,
      ImageDetailsWithConnector imageDetailsWithConnector, List<String> commands, List<String> args, String workingDir,
      List<Integer> ports, Map<String, String> envVars, Map<String, String> envVarsWithSecretRef,
      Map<String, SecretVarParams> secretEnvVars, Map<String, SecretVolumeParams> secretVolumes, String imageSecret,
      Map<String, String> volumeToMountPath, ContainerResourceParams containerResourceParams,
      ContainerSecrets containerSecrets, Integer runAsUser, boolean privileged) {
    super(name, imageDetailsWithConnector, commands, args, workingDir, ports, envVars, envVarsWithSecretRef,
        secretEnvVars, secretVolumes, imageSecret, volumeToMountPath, containerResourceParams, containerSecrets,
        runAsUser, privileged);
    this.containerType = containerType;
  }

  @Override
  public Type getType() {
    return ContainerParams.Type.K8;
  }
}
