package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonTypeName;

import java.util.Objects;

/**
 * Created by peeyushaggarwal on 6/9/16.
 */
@JsonTypeName("STRING")
public class StringSettingValue extends SettingValue {
  /**
   * The String value.
   */
  String value;

  /**
   * Instantiates a new String setting value.
   */
  public StringSettingValue() {
    super(SettingVariableTypes.STRING);
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
    final StringSettingValue other = (StringSettingValue) obj;
    return Objects.equals(this.value, other.value);
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    /**
     * The Value.
     */
    String value;
    private SettingVariableTypes type;

    private Builder() {}

    /**
     * A string setting value builder.
     *
     * @return the builder
     */
    public static Builder aStringSettingValue() {
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
     * With type builder.
     *
     * @param type the type
     * @return the builder
     */
    public Builder withType(SettingVariableTypes type) {
      this.type = type;
      return this;
    }

    /**
     * But builder.
     *
     * @return the builder
     */
    public Builder but() {
      return aStringSettingValue().withValue(value).withType(type);
    }

    /**
     * Build string setting value.
     *
     * @return the string setting value
     */
    public StringSettingValue build() {
      StringSettingValue stringSettingValue = new StringSettingValue();
      stringSettingValue.setValue(value);
      stringSettingValue.setType(type);
      return stringSettingValue;
    }
  }
}
