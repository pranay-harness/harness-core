package software.wings.beans;

/**
 *  SearchFilter bean class.
 *
 *
 * @author Rishi
 *
 */
public class SearchFilter {
  public enum OP { EQ, LT, GT, CONTAINS, STARTSWITH }
  ;

  private String fieldName;
  private String fieldValue;
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
}
