package io.harness.ng.core.dto.secrets;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.encryption.SecretRefData;
import io.harness.encryption.SecretReference;
import io.harness.ng.core.models.SSHCredentialSpec;
import io.harness.ng.core.models.SSHKeyCredential;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("KeyReference")
@JsonIgnoreProperties(ignoreUnknown = true)
public class SSHKeyReferenceCredentialDTO extends SSHCredentialSpecDTO {
  @NotNull private String userName;
  @NotNull private String key;
  @ApiModelProperty(dataType = "string") @SecretReference private SecretRefData encryptedPassphrase;

  @Override
  public SSHCredentialSpec toEntity() {
    return SSHKeyCredential.builder()
        .userName(getUserName())
        .keyPath(getKey())
        .userName(getUserName())
        .encryptedPassphrase(getEncryptedPassphrase())
        .build();
  }
}
