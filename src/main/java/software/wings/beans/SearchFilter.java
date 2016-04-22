package software.wings.beans;

import java.util.List;

/**
 * SearchFilter bean class.
 *
 * @author Rishi
 */
public class SearchFilter {
  private String fieldName;
  private Object fieldValue;
  private List<String> fieldValues;
  private Operator op;

  public String getFieldName() {
    return fieldName;
  }

  public void setFieldName(String fieldName) {
    this.fieldName = fieldName;
  }

  public Object getFieldValue() {
    return fieldValue;
  }

  public void setFieldValue(Object fieldValue) {
    this.fieldValue = fieldValue;
  }

  public Operator getOp() {
    return op;
  }

  public void setOp(Operator op) {
    this.op = op;
  }

  public List<String> getFieldValues() {
    return fieldValues;
  }

  public void setFieldValues(List<String> fieldValues) {
    this.fieldValues = fieldValues;
  }

  public enum Operator { EQ, LT, GT, CONTAINS, STARTS_WITH, IN, NOT_IN }
}
