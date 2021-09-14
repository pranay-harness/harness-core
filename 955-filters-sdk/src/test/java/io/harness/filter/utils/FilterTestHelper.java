/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.filter.utils;

import io.harness.filter.dto.FilterDTO;

import lombok.experimental.UtilityClass;

@UtilityClass
public class FilterTestHelper {
  public FilterDTO createFilter(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier) {
    return FilterDTO.builder()
        .name("name")
        .orgIdentifier(orgIdentifier)
        .projectIdentifier(projectIdentifier)
        .identifier(identifier)
        .build();
  }
}
