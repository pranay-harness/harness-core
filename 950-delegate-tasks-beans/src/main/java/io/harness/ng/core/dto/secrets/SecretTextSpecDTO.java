package io.harness.ng.core.dto.secrets;

import io.harness.SecretConstants;
import io.harness.ng.core.models.SecretSpec;
import io.harness.ng.core.models.SecretTextSpec;
import io.harness.secretmanagerclient.ValueType;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Optional;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("SecretText")
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(name = "SecretTextSpec", description = "This has details of encrypted text secret.")
public class SecretTextSpecDTO extends SecretSpecDTO {
  @NotNull @Schema(description = SecretConstants.SECRET_MANAGER_IDENTIFIER) private String secretManagerIdentifier;
  @Schema(description = "This has details to specify if the secret value is inline or referenced.")
  @NotNull
  private ValueType valueType;
  @Schema(description = "Value of the Secret") private String value;

  @Override
  @JsonIgnore
  public Optional<String> getErrorMessageForInvalidYaml() {
    if (valueType == ValueType.Inline && value != null) {
      return Optional.of("Inline secret text cannot be provided in YAML.");
    }
    if (valueType == ValueType.Reference && value == null) {
      return Optional.of("value cannot be empty for reference secret text in YAML.");
    }
    return Optional.empty();
  }

  @Override
  public SecretSpec toEntity() {
    return SecretTextSpec.builder()
        .secretManagerIdentifier(getSecretManagerIdentifier())
        .valueType(getValueType())
        .build();
  }
}
