package io.harness.ng.core.dto.secrets;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.ng.core.models.SecretSpec;
import io.harness.ng.core.models.SecretTextSpec;
import io.harness.secretmanagerclient.ValueType;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("SecretText")
@JsonIgnoreProperties(ignoreUnknown = true)
public class SecretTextSpecDTO extends SecretSpecDTO {
  @NotNull private String secretManagerIdentifier;
  @NotNull private ValueType valueType;
  private String value;
  @JsonIgnore private boolean draft;

  @Override
  @JsonIgnore
  public boolean isValidYaml() {
    return valueType == ValueType.Reference || (valueType == ValueType.Inline && value == null);
  }

  @Override
  public SecretSpec toEntity() {
    return SecretTextSpec.builder()
        .secretManagerIdentifier(getSecretManagerIdentifier())
        .valueType(getValueType())
        .draft(isDraft())
        .build();
  }
}
