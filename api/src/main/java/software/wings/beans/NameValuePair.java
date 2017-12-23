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
    Value can only be of type String or in encrypted format
  */
  private String value;

  /*
   Could be TEXT / ENCRYPTED_TEXT
  */
  private String valueType;

  @Data
  @EqualsAndHashCode(callSuper = true)
  @NoArgsConstructor
  public static abstract class AbstractYaml extends BaseYaml {
    private String name;
    private String value;
    private String valueType;

    public AbstractYaml(String name, String value, String valueType) {
      this.name = name;
      this.value = value;
      this.valueType = valueType;
    }
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @NoArgsConstructor
  public static class Yaml extends AbstractYaml {
    @Builder
    public Yaml(String name, String value, String valueType) {
      super(name, value, valueType);
    }
  }
}
