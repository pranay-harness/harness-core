package software.wings.api;

/**
 * Created by peeyushaggarwal on 7/14/16.
 */
public class ExecutionDataValue {
  private String displayName;
  private Object value;

  /**
   * Getter for property 'displayName'.
   *
   * @return Value for property 'displayName'.
   */
  public String getDisplayName() {
    return displayName;
  }

  /**
   * Setter for property 'displayName'.
   *
   * @param displayName Value to set for property 'displayName'.
   */
  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  /**
   * Getter for property 'value'.
   *
   * @return Value for property 'value'.
   */
  public Object getValue() {
    return value;
  }

  /**
   * Setter for property 'value'.
   *
   * @param value Value to set for property 'value'.
   */
  public void setValue(Object value) {
    this.value = value;
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private String displayName;
    private Object value;

    private Builder() {}

    /**
     * An execution data value builder.
     *
     * @return the builder
     */
    public static Builder anExecutionDataValue() {
      return new Builder();
    }

    /**
     * With display name builder.
     *
     * @param displayName the display name
     * @return the builder
     */
    public Builder withDisplayName(String displayName) {
      this.displayName = displayName;
      return this;
    }

    /**
     * With value builder.
     *
     * @param value the value
     * @return the builder
     */
    public Builder withValue(Object value) {
      this.value = value;
      return this;
    }

    /**
     * But builder.
     *
     * @return the builder
     */
    public Builder but() {
      return anExecutionDataValue().withDisplayName(displayName).withValue(value);
    }

    /**
     * Build execution data value.
     *
     * @return the execution data value
     */
    public ExecutionDataValue build() {
      ExecutionDataValue executionDataValue = new ExecutionDataValue();
      executionDataValue.setDisplayName(displayName);
      executionDataValue.setValue(value);
      return executionDataValue;
    }
  }
}
