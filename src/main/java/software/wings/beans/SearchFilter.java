package software.wings.beans;

import java.util.List;

/**
 *  SearchFilter bean class.
 *
 *
 * @author Rishi
 *
 */
public class SearchFilter {
  public enum OP { EQ, LT, GT, CONTAINS, STARTS_WITH, IN, NOT_IN }
  ;

  private String fieldName;
  private String fieldValue;
  private List<String> fieldValues;
  private OP op;

  public String getFieldName() {
    return fieldName;
  }
  public void setFieldName(String fieldName) {
    this.fieldName = fieldName;
  }
  public String getFieldValue() {
    return fieldValue;
  }
  public void setFieldValue(String fieldValue) {
    this.fieldValue = fieldValue;
  }
  public OP getOp() {
    return op;
  }
  public void setOp(OP op) {
    this.op = op;
  }
  public List<String> getFieldValues() {
    return fieldValues;
  }
  public void setFieldValues(List<String> fieldValues) {
    this.fieldValues = fieldValues;
  }
}
