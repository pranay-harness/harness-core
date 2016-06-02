package software.wings.beans;

import com.google.common.base.MoreObjects;

import java.util.Arrays;

/**
 * SearchFilter bean class.
 *
 * @author Rishi
 */
public class SearchFilter {
  private String fieldName;
  private Object[] fieldValues;
  private Operator op;

  public String getFieldName() {
    return fieldName;
  }

  public void setFieldName(String fieldName) {
    this.fieldName = fieldName;
  }

  public Operator getOp() {
    return op;
  }

  public void setOp(Operator op) {
    this.op = op;
  }

  public Object[] getFieldValues() {
    return fieldValues;
  }

  public void setFieldValues(Object... fieldValues) {
    this.fieldValues = fieldValues;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((fieldName == null) ? 0 : fieldName.hashCode());
    result = prime * result + Arrays.hashCode(fieldValues);
    result = prime * result + ((op == null) ? 0 : op.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    SearchFilter other = (SearchFilter) obj;
    if (fieldName == null) {
      if (other.fieldName != null)
        return false;
    } else if (!fieldName.equals(other.fieldName))
      return false;
    if (!Arrays.equals(fieldValues, other.fieldValues))
      return false;
    if (op != other.op)
      return false;
    return true;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("fieldName", fieldName)
        .add("fieldValues", fieldValues)
        .add("op", op)
        .toString();
  }

  public enum Operator { EQ, LT, GT, CONTAINS, STARTS_WITH, IN, NOT_IN }

  public static final class Builder {
    private String fieldName;
    private Object[] fieldValues;
    private Operator op;

    private Builder() {}

    public static Builder aSearchFilter() {
      return new Builder();
    }

    public Builder withField(String fieldName, Operator op, Object... fieldValues) {
      this.fieldName = fieldName;
      this.fieldValues = fieldValues;
      this.op = op;
      return this;
    }

    public SearchFilter build() {
      SearchFilter searchFilter = new SearchFilter();
      searchFilter.setFieldName(fieldName);
      searchFilter.setFieldValues(fieldValues);
      searchFilter.setOp(op);
      return searchFilter;
    }
  }
}
