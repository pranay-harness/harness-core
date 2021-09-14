/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.beans.yaml.extended;

import io.harness.encryption.SecretRefData;

import com.fasterxml.jackson.annotation.JsonTypeName;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@JsonTypeName("Secret")
@TypeAlias("customSecretVariable")
public class CustomSecretVariable implements CustomVariable {
  @Builder.Default @NotNull Type type = Type.SECRET;
  @NotNull String name;
  @NotNull SecretRefData value;
}
