package io.harness.ng.core.dto.secrets;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.encryption.SecretRefData;
import io.harness.encryption.SecretReference;
import io.harness.ng.core.models.SSHCredentialSpec;
import io.harness.ng.core.models.SSHPasswordCredential;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("Password")
@JsonIgnoreProperties(ignoreUnknown = true)
public class SSHPasswordCredentialDTO extends SSHCredentialSpecDTO {
  @NotNull private String userName;
  @ApiModelProperty(dataType = "string") @NotNull @SecretReference private SecretRefData password;

  @Override
  public SSHCredentialSpec toEntity() {
    return SSHPasswordCredential.builder().userName(getUserName()).password(getPassword()).build();
  }
}
