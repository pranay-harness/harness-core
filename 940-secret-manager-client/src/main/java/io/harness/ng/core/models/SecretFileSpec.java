package io.harness.ng.core.models;

import io.harness.ng.core.dto.secrets.SecretFileSpecDTO;
import io.harness.ng.core.dto.secrets.SecretSpecDTO;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("SecretFile")
@JsonIgnoreProperties(ignoreUnknown = true)
public class SecretFileSpec extends SecretSpec {
  private String secretManagerIdentifier;

  @Override
  public SecretSpecDTO toDTO() {
    return SecretFileSpecDTO.builder().secretManagerIdentifier(getSecretManagerIdentifier()).build();
  }
}
