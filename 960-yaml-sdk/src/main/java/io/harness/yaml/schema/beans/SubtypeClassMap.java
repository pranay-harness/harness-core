/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.yaml.schema.beans;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@EqualsAndHashCode(callSuper = false)
@OwnedBy(DX)
public class SubtypeClassMap {
  /**
   * The subtype field value which will determine to get into current condition.
   */
  String subtypeEnum;
  /**
   * The definition key to which $ref will point.
   */
  String subTypeDefinitionKey;
  /**
   * Type of the class of this subtype
   */
  Class<?> subTypeClass;
}
