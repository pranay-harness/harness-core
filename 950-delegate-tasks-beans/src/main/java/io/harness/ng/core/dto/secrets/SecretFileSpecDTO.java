package io.harness.ng.core.dto.secrets;

import io.harness.SecretConstants;
import io.harness.ng.core.models.SecretFileSpec;
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

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("SecretFile")
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(name = "SecretFileSpe", description = "This has details of Secret File defined in harness")
public class SecretFileSpecDTO extends SecretSpecDTO {
  @Schema(description = SecretConstants.SECRET_MANAGER_IDENTIFIER) @NotNull private String secretManagerIdentifier;

  @Override
  @JsonIgnore
  public Optional<String> getErrorMessageForInvalidYaml() {
    return Optional.empty();
  }

  @Override
  public SecretSpec toEntity() {
    return SecretFileSpec.builder().secretManagerIdentifier(getSecretManagerIdentifier()).build();
  }
}
