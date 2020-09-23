package io.harness.ng.core.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.ng.core.dto.secrets.SSHKeySpecDTO;
import io.harness.ng.core.dto.secrets.SecretSpecDTO;
import io.harness.secretmanagerclient.SSHAuthScheme;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("SSHKey")
public class SSHExecutionCredentialSpec extends SecretSpec {
  SSHAuthScheme authScheme;
  int port;

  @JsonProperty("spec")
  @JsonTypeInfo(
      use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "authScheme", visible = true)
  BaseSSHSpec sshSpec;

  @Override
  public SecretSpecDTO toDTO() {
    return SSHKeySpecDTO.builder().port(getPort()).authScheme(getAuthScheme()).spec(getSshSpec().toDTO()).build();
  }
}
