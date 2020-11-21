package io.harness.ng.core.dto.secrets;

import io.harness.ng.core.models.SSHExecutionCredentialSpec;
import io.harness.ng.core.models.SecretSpec;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("SSHKey")
@JsonIgnoreProperties(ignoreUnknown = true)
public class SSHKeySpecDTO extends SecretSpecDTO {
  int port;
  @NotNull SSHAuthDTO auth;

  @Override
  @JsonIgnore
  public boolean isValidYaml() {
    return true;
  }

  @Override
  public SecretSpec toEntity() {
    return SSHExecutionCredentialSpec.builder().port(getPort()).auth(this.auth.toEntity()).build();
  }

  @Builder
  public SSHKeySpecDTO(int port, SSHAuthDTO auth) {
    this.port = port;
    this.auth = auth;
  }
}
