/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.ng.core.dto.secrets;

import io.harness.ng.core.models.SecretSpec;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.Optional;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "type", visible = true)
@JsonSubTypes(value =
    {
      @JsonSubTypes.Type(value = SecretTextSpecDTO.class, name = "SecretText")
      , @JsonSubTypes.Type(value = SecretFileSpecDTO.class, name = "SecretFile"),
          @JsonSubTypes.Type(value = SSHKeySpecDTO.class, name = "SSHKey"),
    })
public abstract class SecretSpecDTO {
  public abstract Optional<String> getErrorMessageForInvalidYaml();

  public abstract SecretSpec toEntity();
}
