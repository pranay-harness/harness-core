/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.persistence.converters;

import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UuidAccess;

import java.time.Duration;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Id;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldNameConstants(innerTypeName = "DurationTestEntityKeys")
public class DurationTestEntity implements PersistentEntity, UuidAccess {
  @Id String uuid;
  Duration testDuration;
}
