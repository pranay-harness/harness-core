package io.harness.ng.core.dto.secrets;

import static io.harness.yamlSchema.NGSecretReferenceConstants.SECRET_REF_PATTERN;

import io.harness.beans.DecryptableEntity;
import io.harness.encryption.SecretRefData;
import io.harness.encryption.SecretReference;
import io.harness.ng.core.models.SSHCredentialSpec;
import io.harness.ng.core.models.SSHKeyCredential;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("KeyReference")
@JsonIgnoreProperties(ignoreUnknown = true)
public class SSHKeyReferenceCredentialDTO extends SSHCredentialSpecDTO implements DecryptableEntity {
  @NotNull private String userName;
  @ApiModelProperty(dataType = "string")
  @SecretReference
  @Pattern(regexp = SECRET_REF_PATTERN)
  private SecretRefData key;
  @ApiModelProperty(dataType = "string")
  @SecretReference
  @Pattern(regexp = SECRET_REF_PATTERN)
  private SecretRefData encryptedPassphrase;

  @Override
  public SSHCredentialSpec toEntity() {
    return SSHKeyCredential.builder()
        .userName(getUserName())
        .encryptedPassphrase(getEncryptedPassphrase())
        .key(getKey())
        .build();
  }
}
