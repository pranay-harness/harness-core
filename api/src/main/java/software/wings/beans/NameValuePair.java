package software.wings.beans;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.wings.yaml.BaseYaml;

/**
 * Generic Name Value pair
 * @author rktummala on 10/27/17
 */
@Data
@Builder
public class NameValuePair {
  private String name;
  /*
    Value can only be of type String / number types
  */
  private String value;

  /*
   Could be String, Integer, Long, etc.
  */
  private String valueType;

  @Data
  @EqualsAndHashCode(callSuper = true)
  @NoArgsConstructor
  public static class Yaml extends BaseYaml {
    private String name;
    private String value;
    private String valueType;

    public static final class Builder {
      private String name;
      private String value;
      private String valueType;

      private Builder() {}

      public static Builder aYaml() {
        return new Builder();
      }

      public Builder withName(String name) {
        this.name = name;
        return this;
      }

      public Builder withValue(String value) {
        this.value = value;
        return this;
      }

      public Builder withValueType(String valueType) {
        this.valueType = valueType;
        return this;
      }

      public Builder but() {
        return aYaml().withName(name).withValue(value).withValueType(valueType);
      }

      public Yaml build() {
        Yaml yaml = new Yaml();
        yaml.setName(name);
        yaml.setValue(value);
        yaml.setValueType(valueType);
        return yaml;
      }
    }
  }
}
