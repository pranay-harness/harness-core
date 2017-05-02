package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonTypeName;
import software.wings.settings.SettingValue;

import java.util.Objects;

/**
 * Created by peeyushaggarwal on 6/9/16.
 */
@JsonTypeName("STRING")
public class StringValue extends SettingValue {
  /**
   * The String value.
   */
  private String value;

  /**
   * Instantiates a new String setting value.
   */
  public StringValue() {
    super(SettingVariableTypes.STRING.name());
  }

  /**
   * Getter for property 'value'.
   *
   * @return Value for property 'value'.
   */
  public String getValue() {
    return value;
  }

  /**
   * Setter for property 'value'.
   *
   * @param value Value to set for property 'value'.
   */
  public void setValue(String value) {
    this.value = value;
  }

  @Override
  public int hashCode() {
    return Objects.hash(value);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final StringValue other = (StringValue) obj;
    return Objects.equals(this.value, other.value);
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    /**
     * The Value.
     */
    private String value;

    private Builder() {}

    /**
     * A string setting value builder.
     *
     * @return the builder
     */
    public static Builder aStringValue() {
      return new Builder();
    }

    /**
     * With value builder.
     *
     * @param value the value
     * @return the builder
     */
    public Builder withValue(String value) {
      this.value = value;
      return this;
    }

    /**
     * But builder.
     *
     * @return the builder
     */
    public Builder but() {
      return aStringValue().withValue(value);
    }

    /**
     * Build string setting value.
     *
     * @return the string setting value
     */
    public StringValue build() {
      StringValue stringValue = new StringValue();
      stringValue.setValue(value);
      return stringValue;
    }
  }
}
