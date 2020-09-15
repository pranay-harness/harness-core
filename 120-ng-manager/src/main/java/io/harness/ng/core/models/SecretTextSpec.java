package io.harness.ng.core.models;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.ng.core.dto.secrets.SecretSpecDTO;
import io.harness.ng.core.dto.secrets.SecretTextSpecDTO;
import io.harness.secretmanagerclient.ValueType;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("SecretText")
public class SecretTextSpec extends SecretSpec {
  private String secretManagerIdentifier;
  private ValueType valueType;
  private boolean draft;

  @Override
  public SecretSpecDTO toDTO() {
    return SecretTextSpecDTO.builder()
        .secretManagerIdentifier(getSecretManagerIdentifier())
        .valueType(getValueType())
        .draft(isDraft())
        .build();
  }
}
