package io.harness.ng.core.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.harness.ng.core.dto.secrets.SSHAuthDTO;
import io.harness.secretmanagerclient.SSHAuthScheme;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class SSHAuth {
  private SSHAuthScheme type;
  @JsonProperty("spec")
  @JsonTypeInfo(
      use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type", visible = true)
  BaseSSHSpec sshSpec;

  public SSHAuthDTO toDTO() {
    return SSHAuthDTO.builder().type(type).spec(sshSpec.toDTO()).build();
  }

  @Builder
  public SSHAuth(SSHAuthScheme type, BaseSSHSpec sshSpec) {
    this.type = type;
    this.sshSpec = sshSpec;
  }
}
