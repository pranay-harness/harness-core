package io.harness.ng.core.dto.secrets;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.ng.core.models.SSHExecutionCredentialSpec;
import io.harness.ng.core.models.SecretSpec;
import io.harness.secretmanagerclient.SSHAuthScheme;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("SSHKey")
@JsonIgnoreProperties(ignoreUnknown = true)
public class SSHKeySpecDTO extends SecretSpecDTO {
  @NotNull SSHAuthScheme authScheme;
  int port;

  @JsonTypeInfo(
      use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "authScheme", visible = true)
  @NotNull
  @Valid
  private BaseSSHSpecDTO spec;

  @Builder
  public SSHKeySpecDTO(SSHAuthScheme authScheme, int port, BaseSSHSpecDTO spec) {
    this.authScheme = authScheme;
    this.port = port;
    this.spec = spec;
  }

  @Override
  @JsonIgnore
  public boolean isValidYaml() {
    return true;
  }

  @Override
  public SecretSpec toEntity() {
    return SSHExecutionCredentialSpec.builder()
        .authScheme(getAuthScheme())
        .port(getPort())
        .sshSpec(getSpec().toEntity())
        .build();
  }
}
