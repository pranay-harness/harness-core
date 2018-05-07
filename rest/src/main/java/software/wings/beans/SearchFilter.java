package software.wings.beans;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SearchFilter {
  public enum Operator {
    EQ,
    NOT_EQ,
    LT,
    GE,
    GT,
    CONTAINS,
    STARTS_WITH,
    HAS,
    IN,
    NOT_IN,
    EXISTS,
    NOT_EXISTS,
    OR,
    AND;
  }

  private String fieldName;
  private Object[] fieldValues;
  private Operator op;
}
