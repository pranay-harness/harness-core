/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.yaml.schema.beans;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;

import java.util.Set;
import javax.annotation.Nullable;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@OwnedBy(DX)
public class PossibleFieldTypes {
  String fieldName;

  Set<SupportedPossibleFieldTypes> fieldTypes;
  /**
   * default field type if it is in any of fieldTypes.
   */
  @Nullable SupportedPossibleFieldTypes defaultFieldType;

  FieldTypesMetadata fieldTypesMetadata;
}
