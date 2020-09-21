package io.harness.ng.core.models;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.encryption.SecretRefData;
import io.harness.ng.core.dto.secrets.SSHCredentialSpecDTO;
import io.harness.ng.core.dto.secrets.SSHKeyReferenceCredentialDTO;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("KeyReference")
public class SSHKeyCredential extends SSHCredentialSpec {
  private String userName;
  private String keyPath;
  private SecretRefData encryptedPassphrase;

  @Override
  public SSHCredentialSpecDTO toDTO() {
    return SSHKeyReferenceCredentialDTO.builder()
        .userName(getUserName())
        .key(getKeyPath())
        .encryptedPassphrase(getEncryptedPassphrase())
        .build();
  }
}
