/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
