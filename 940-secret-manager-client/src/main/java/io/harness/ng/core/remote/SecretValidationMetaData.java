/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.ng.core.remote;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.EXISTING_PROPERTY;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

import io.harness.secretmanagerclient.SecretType;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import javax.validation.constraints.NotNull;
import lombok.Data;

@Data
@JsonTypeInfo(use = NAME, property = "type", include = EXISTING_PROPERTY, visible = true)
public abstract class SecretValidationMetaData {
  @NotNull private SecretType type;
}
