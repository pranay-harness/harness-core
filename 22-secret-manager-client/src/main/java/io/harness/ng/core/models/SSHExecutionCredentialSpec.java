package io.harness.ng.core.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.ng.core.dto.secrets.SSHKeySpecDTO;
import io.harness.ng.core.dto.secrets.SecretSpecDTO;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Optional;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("SSHKey")
public class SSHExecutionCredentialSpec extends SecretSpec {
  int port;
  SSHAuth auth;

  @Override
  public SecretSpecDTO toDTO() {
    return SSHKeySpecDTO.builder()
        .port(getPort())
        .auth(Optional.ofNullable(auth).map(SSHAuth::toDTO).orElse(null))
        .build();
  }
}
