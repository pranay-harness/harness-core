package io.harness.ng.core.dto.secrets;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.ng.core.models.SecretFileSpec;
import io.harness.ng.core.models.SecretSpec;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("SecretFile")
@JsonIgnoreProperties(ignoreUnknown = true)
public class SecretFileSpecDTO extends SecretSpecDTO {
  @NotNull private String secretManagerIdentifier;
  @JsonIgnore private boolean draft;

  @Override
  @JsonIgnore
  public boolean isValidYaml() {
    return true;
  }

  @Override
  public SecretSpec toEntity() {
    return SecretFileSpec.builder().secretManagerIdentifier(getSecretManagerIdentifier()).draft(isDraft()).build();
  }
}
