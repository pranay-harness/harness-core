/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.yaml.schema.beans;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.Set;
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
public class FieldSubtypeData {
  /**
   * Field name in jsonSchema on which {@link JsonSubTypes} is applied.
   */
  String fieldName;
  /**
   * name of discriminator on which the subtype resolution depends.
   */
  String discriminatorName;
  /**
   * Type of discriminator.
   */
  JsonTypeInfo.As discriminatorType;
  /**
   * Mapping of subTypes.
   */
  Set<SubtypeClassMap> subtypesMapping;
}
