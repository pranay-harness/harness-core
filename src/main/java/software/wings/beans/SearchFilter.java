package software.wings.beans;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

import java.util.List;

/**
 * SearchFilter bean class.
 *
 * @author Rishi
 */
public class SearchFilter {
  private String fieldName;
  private Object fieldValue;
  private List<Object> fieldValues;
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

  public List<Object> getFieldValues() {
    return fieldValues;
  }

  public void setFieldValues(List<Object> fieldValues) {
    this.fieldValues = fieldValues;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SearchFilter that = (SearchFilter) o;
    return Objects.equal(fieldName, that.fieldName) && Objects.equal(fieldValue, that.fieldValue)
        && Objects.equal(fieldValues, that.fieldValues) && op == that.op;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(fieldName, fieldValue, fieldValues, op);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("fieldName", fieldName)
        .add("fieldValue", fieldValue)
        .add("fieldValues", fieldValues)
        .add("op", op)
        .toString();
  }

  public enum Operator { EQ, LT, GT, CONTAINS, STARTS_WITH, IN, NOT_IN }
}
