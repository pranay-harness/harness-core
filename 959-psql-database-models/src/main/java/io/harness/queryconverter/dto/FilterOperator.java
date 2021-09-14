/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.queryconverter.dto;

public enum FilterOperator {
  EQUALS,
  IN,
  NOT_IN,
  NOT_EQUALS,
  NOT_NULL,
  LIKE,
  TIME_AFTER,
  TIME_BEFORE,
  GREATER_OR_EQUALS,
  LESS_OR_EQUALS
}
