package io.harness.ng.core.dto.secrets;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.ng.core.models.BaseSSHSpec;
import io.harness.ng.core.models.SSHConfig;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("SSH")
@JsonIgnoreProperties(ignoreUnknown = true)
public class SSHConfigDTO extends BaseSSHSpecDTO {
  @NotNull SSHCredentialType credentialType;

  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "credentialType",
      visible = true)
  @Valid
  @NotNull
  private SSHCredentialSpecDTO spec;

  @Builder
  public SSHConfigDTO(SSHCredentialType credentialType, SSHCredentialSpecDTO spec) {
    this.credentialType = credentialType;
    this.spec = spec;
  }

  @Override
  public BaseSSHSpec toEntity() {
    return SSHConfig.builder().credentialType(getCredentialType()).spec(getSpec().toEntity()).build();
  }
}
