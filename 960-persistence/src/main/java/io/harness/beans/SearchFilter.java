/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.beans;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SearchFilter {
  public enum Operator {
    EQ,
    NOT_EQ,
    LT,
    LT_EQ,
    GE,
    GT,
    CONTAINS,
    STARTS_WITH,
    HAS,
    IN,
    NOT_IN,
    EXISTS,
    NOT_EXISTS,
    HAS_ALL,
    OR,
    AND,
    ELEMENT_MATCH;
  }

  private String fieldName;
  private Object[] fieldValues;
  private Operator op;
}
