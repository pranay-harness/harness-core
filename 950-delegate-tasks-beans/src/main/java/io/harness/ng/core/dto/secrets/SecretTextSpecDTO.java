/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.ng.core.dto.secrets;

import io.harness.ng.core.models.SecretSpec;
import io.harness.ng.core.models.SecretTextSpec;
import io.harness.secretmanagerclient.ValueType;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
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
public class SecretTextSpecDTO extends SecretSpecDTO {
  @NotNull private String secretManagerIdentifier;
  @NotNull private ValueType valueType;
  private String value;

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
