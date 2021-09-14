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
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@OwnedBy(DX)
public class SwaggerDefinitionsMetaInfo {
  Set<FieldSubtypeData> subtypeClassMap;
  Set<OneOfMapping> oneOfMappings;
  Set<PossibleFieldTypes> fieldPossibleTypes;
  Set<FieldEnumData> fieldEnumData;
  Set<String> notEmptyStringFields;
}
