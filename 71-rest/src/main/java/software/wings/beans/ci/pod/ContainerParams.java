package software.wings.beans.ci.pod;

import io.harness.security.encryption.EncryptableSettingWithEncryptionDetails;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
import java.util.Map;

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
  private Map<String, EncryptedDataDetail> encryptedSecrets;
  private Map<String, SecretKeyParams> secretEnvVars;
  private Map<String, EncryptableSettingWithEncryptionDetails> publishArtifactEncryptedValues;
  private Map<String, String> volumeToMountPath;
  private ContainerResourceParams containerResourceParams;

  public abstract ContainerParams.Type getType();

  public enum Type {
    K8, // Generic K8 container configuration
    K8_GIT_CLONE, // K8 container configuration to clone a git repository
  }
}