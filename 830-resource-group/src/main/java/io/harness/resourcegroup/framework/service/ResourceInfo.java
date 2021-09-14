/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.resourcegroup.framework.service;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Value
@Builder
public class ResourceInfo {
  String accountIdentifier;
  String orgIdentifier;
  String projectIdentifier;
  String resourceIdentifier;
  String resourceType;
}
