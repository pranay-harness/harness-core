package io.harness.ng.core.dto.secrets;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.beans.DecryptableEntity;
import io.harness.encryption.SecretRefData;
import io.harness.encryption.SecretReference;
import io.harness.ng.core.models.SSHCredentialSpec;
import io.harness.ng.core.models.SSHKeyPathCredential;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("KeyPath")
@JsonIgnoreProperties(ignoreUnknown = true)
public class SSHKeyPathCredentialDTO extends SSHCredentialSpecDTO implements DecryptableEntity {
  @NotNull private String userName;
  @NotNull private String keyPath;
  @ApiModelProperty(dataType = "string") @SecretReference private SecretRefData encryptedPassphrase;

  @Override
  public SSHCredentialSpec toEntity() {
    return SSHKeyPathCredential.builder()
        .userName(getUserName())
        .keyPath(getKeyPath())
        .encryptedPassphrase(getEncryptedPassphrase())
        .build();
  }
}
