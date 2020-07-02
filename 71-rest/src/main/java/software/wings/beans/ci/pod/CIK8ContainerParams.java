package software.wings.beans.ci.pod;

import io.harness.security.encryption.EncryptedDataDetail;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
@EqualsAndHashCode(callSuper = true)
public class CIK8ContainerParams extends ContainerParams {
  private CIContainerType containerType;

  @Builder
  public CIK8ContainerParams(CIContainerType containerType, String name,
      ImageDetailsWithConnector imageDetailsWithConnector, List<String> commands, List<String> args, String workingDir,
      List<Integer> ports, Map<String, String> envVars, Map<String, EncryptedDataDetail> encryptedSecrets,
      Map<String, SecretKeyParams> secretEnvVars, Map<String, String> volumeToMountPath,
      ContainerResourceParams containerResourceParams) {
    super(name, imageDetailsWithConnector, commands, args, workingDir, ports, envVars, encryptedSecrets, secretEnvVars,
        volumeToMountPath, containerResourceParams);
    this.containerType = containerType;
  }

  @Override
  public ContainerParams.Type getType() {
    return ContainerParams.Type.K8;
  }
}