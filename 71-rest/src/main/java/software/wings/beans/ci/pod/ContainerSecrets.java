package software.wings.beans.ci.pod;

import io.harness.security.encryption.EncryptableSettingWithEncryptionDetails;
import lombok.Builder;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
@Builder
public class ContainerSecrets {
  @Builder.Default private Map<String, EncryptedVariableWithType> encryptedSecrets = new HashMap<>();
  @Builder.Default
  private Map<String, EncryptableSettingWithEncryptionDetails> publishArtifactEncryptedValues = new HashMap<>();
}
