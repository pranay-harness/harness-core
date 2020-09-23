package io.harness.ng.core.models;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.ng.core.dto.secrets.SecretFileSpecDTO;
import io.harness.ng.core.dto.secrets.SecretSpecDTO;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("SecretFile")
public class SecretFileSpec extends SecretSpec {
  private String secretManagerIdentifier;
  private boolean draft;

  @Override
  public SecretSpecDTO toDTO() {
    return SecretFileSpecDTO.builder().secretManagerIdentifier(getSecretManagerIdentifier()).draft(isDraft()).build();
  }
}
