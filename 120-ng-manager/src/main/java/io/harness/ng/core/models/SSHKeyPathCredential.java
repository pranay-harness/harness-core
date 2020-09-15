package io.harness.ng.core.models;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.encryption.SecretRefData;
import io.harness.ng.core.dto.secrets.SSHCredentialSpecDTO;
import io.harness.ng.core.dto.secrets.SSHKeyPathCredentialDTO;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("KeyPath")
public class SSHKeyPathCredential extends SSHCredentialSpec {
  private String userName;
  private String keyPath;
  private SecretRefData encryptedPassphrase;

  @Override
  public SSHCredentialSpecDTO toDTO() {
    return SSHKeyPathCredentialDTO.builder()
        .userName(getUserName())
        .keyPath(getKeyPath())
        .encryptedPassphrase(getEncryptedPassphrase())
        .build();
  }
}
