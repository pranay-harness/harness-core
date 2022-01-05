package io.harness.ng.core.dto.secrets;

import io.harness.ng.core.models.SSHAuth;
import io.harness.secretmanagerclient.SSHAuthScheme;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Schema(name = "SSHAuth", description = "This is the SSH Authentication specification defined in Harness.")
public class SSHAuthDTO {
  @JsonProperty("type")
  @NotNull
  @Schema(description = "Specifies authentication scheme, SSH or Kerberos")
  private SSHAuthScheme authScheme;

  @JsonTypeInfo(
      use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "type", visible = true)
  @NotNull
  @Valid
  private BaseSSHSpecDTO spec;

  @Builder
  public SSHAuthDTO(SSHAuthScheme type, BaseSSHSpecDTO spec) {
    this.authScheme = type;
    this.spec = spec;
  }

  public SSHAuth toEntity() {
    return SSHAuth.builder().type(authScheme).sshSpec(spec.toEntity()).build();
  }
}
