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

  /**
   * Gets field name.
   *
   * @return the field name
   */
  public String getFieldName() {
    return fieldName;
  }

  /**
   * Sets field name.
   *
   * @param fieldName the field name
   */
  public void setFieldName(String fieldName) {
    this.fieldName = fieldName;
  }

  /**
   * Gets op.
   *
   * @return the op
   */
  public Operator getOp() {
    return op;
  }

  /**
   * Sets op.
   *
   * @param op the op
   */
  public void setOp(Operator op) {
    this.op = op;
  }

  /**
   * Get field values object [ ].
   *
   * @return the object [ ]
   */
  public Object[] getFieldValues() {
    return fieldValues;
  }

  /**
   * Sets field values.
   *
   * @param fieldValues the field values
   */
  public void setFieldValues(Object... fieldValues) {
    this.fieldValues = fieldValues;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((fieldName == null) ? 0 : fieldName.hashCode());
    result = prime * result + Arrays.hashCode(fieldValues);
    result = prime * result + ((op == null) ? 0 : op.hashCode());
    return result;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    SearchFilter other = (SearchFilter) obj;
    if (fieldName == null) {
      if (other.fieldName != null) {
        return false;
      }
    } else if (!fieldName.equals(other.fieldName)) {
      return false;
    }
    if (!Arrays.equals(fieldValues, other.fieldValues)) {
      return false;
    }
    if (op != other.op) {
      return false;
    }
    return true;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("fieldName", fieldName)
        .add("fieldValues", fieldValues)
        .add("op", op)
        .toString();
  }

  /**
   * The Enum Operator.
   */
  public enum Operator {
    /**
     * Eq operator.
     */
    EQ,
    /**
     * Not eq operator.
     */
    NOT_EQ,
    /**
     * Lt operator.
     */
    LT,
    /**
     * Ge operator.
     */
    GE,
    /**
     * Gt operator.
     */
    GT,
    /**
     * Contains operator.
     */
    CONTAINS,
    /**
     * Starts with operator.
     */
    STARTS_WITH,
    /**
     * Has element operator.
     */
    HAS,
    /**
     * In operator.
     */
    IN,
    /**
     * Not in operator.
     */
    NOT_IN,
    /**
     * Exists operator.
     */
    EXISTS,

    NOT_EXISTS,

    OR,

    AND;
  }

  /**
   * The Class Builder.
   */
  public static final class Builder {
    private String fieldName;
    private Object[] fieldValues;
    private Operator op;

    private Builder() {}

    /**
     * A search filter.
     *
     * @return the builder
     */
    public static Builder aSearchFilter() {
      return new Builder();
    }

    /**
     * With field.
     *
     * @param fieldName   the field name
     * @param op          the op
     * @param fieldValues the field values
     * @return the builder
     */
    public Builder withField(String fieldName, Operator op, Object... fieldValues) {
      this.fieldName = fieldName;
      this.fieldValues = fieldValues;
      this.op = op;
      return this;
    }

    /**
     * Builds the.
     *
     * @return the search filter
     */
    public SearchFilter build() {
      SearchFilter searchFilter = new SearchFilter();
      searchFilter.setFieldName(fieldName);
      searchFilter.setFieldValues(fieldValues);
      searchFilter.setOp(op);
      return searchFilter;
    }
  }
}
