/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.queryconverter.dto;

import lombok.Getter;

@Getter
public class SortCriteria {
  String field;
  SortOrder order = SortOrder.ASCENDING;
}
