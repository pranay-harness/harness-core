package software.wings.beans;

import io.harness.data.validator.Trimmed;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.yaml.BaseYaml;

import javax.validation.constraints.NotNull;

/**
 * Generic Name Value pair
 * @author rktummala on 10/27/17
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NameValuePair {
  @NotEmpty @Trimmed private String name;
  /*
    Value can only be of type String or in encrypted format
  */
  @NotNull private String value;

  /*
   Could be TEXT / ENCRYPTED_TEXT
  */
  private String valueType;

  @Data
  @EqualsAndHashCode(callSuper = true)
  @NoArgsConstructor
  public abstract static class AbstractYaml extends BaseYaml {
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
