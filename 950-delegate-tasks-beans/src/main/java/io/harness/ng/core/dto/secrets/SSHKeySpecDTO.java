package io.harness.ng.core.dto.secrets;

import io.harness.ng.core.models.SSHExecutionCredentialSpec;
import io.harness.ng.core.models.SecretSpec;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Optional;
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
@Schema(name = "SSHKeySpec", description = "This is the SSH key authentication details defined in Harness.")
public class SSHKeySpecDTO extends SecretSpecDTO {
  @Schema(description = "SSH port") int port;
  @NotNull SSHAuthDTO auth;

  @Override
  @JsonIgnore
  public Optional<String> getErrorMessageForInvalidYaml() {
    return Optional.empty();
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
