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
