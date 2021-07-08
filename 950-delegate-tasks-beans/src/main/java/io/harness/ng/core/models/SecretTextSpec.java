package io.harness.ng.core.models;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.dto.secrets.SecretSpecDTO;
import io.harness.ng.core.dto.secrets.SecretTextSpecDTO;
import io.harness.secretmanagerclient.ValueType;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;

@OwnedBy(PL)
@Data
@Builder
@FieldNameConstants(innerTypeName = "SecretTextSpecKeys")
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("SecretText")
@JsonIgnoreProperties(ignoreUnknown = true)
public class SecretTextSpec extends SecretSpec {
  private String secretManagerIdentifier;
  private ValueType valueType;

  @Override
  public SecretSpecDTO toDTO() {
    return SecretTextSpecDTO.builder()
        .secretManagerIdentifier(getSecretManagerIdentifier())
        .valueType(getValueType())
        .build();
  }
}
