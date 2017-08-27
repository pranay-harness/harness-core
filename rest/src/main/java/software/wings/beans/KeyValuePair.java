package software.wings.beans;

/**
 * @author rktummala on 08/25/17
 */
public class KeyValuePair {
  private String key;
  private Value value;

  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public Value getValue() {
    return value;
  }

  public void setValue(Value value) {
    this.value = value;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;

    KeyValuePair that = (KeyValuePair) o;

    if (key != null ? !key.equals(that.key) : that.key != null)
      return false;
    return value != null ? value.equals(that.value) : that.value == null;
  }

  @Override
  public int hashCode() {
    int result = key != null ? key.hashCode() : 0;
    result = 31 * result + (value != null ? value.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "KeyValuePair{"
        + "key='" + key + '\'' + ", value=" + value + '}';
  }

  public static final class Builder {
    private String key;
    private Value value;

    private Builder() {}

    public static Builder aKeyValuePair() {
      return new Builder();
    }

    public Builder withKey(String key) {
      this.key = key;
      return this;
    }

    public Builder withValue(Value value) {
      this.value = value;
      return this;
    }

    public KeyValuePair build() {
      KeyValuePair keyValuePair = new KeyValuePair();
      keyValuePair.setKey(key);
      keyValuePair.setValue(value);
      return keyValuePair;
    }
  }

  private static class Value {
    private enum Type { STRING, ENTITY_SUMMARY }
    private Type type;
    private Object value;

    public Type getType() {
      return type;
    }

    public void setType(Type type) {
      this.type = type;
    }

    public Object getValue() {
      return value;
    }

    public void setValue(Object value) {
      this.value = value;
    }
  }
}
