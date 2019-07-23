package software.wings.beans;

import io.harness.data.validator.Trimmed;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.yaml.BaseYaml;

import java.util.ArrayList;
import java.util.List;
import javax.validation.constraints.NotNull;

/**
 * Generic Name Value pair
 * @author rktummala on 10/27/17
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NameValuePair {
  @NotEmpty @Trimmed private String name;
  /*
    Value can only be of type String or in encrypted format
  */
  @NotNull private String value;

  /*
   Could be TEXT / ENCRYPTED_TEXT
   TODO: Why is this not an enum? @swagat
  */
  private String valueType;

  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  public abstract static class AbstractYaml extends BaseYaml {
    private String name;
    private String value;
    private String valueType;
    private List<AllowedValueYaml> allowedList = new ArrayList<>();

    public AbstractYaml(String name, String value, String valueType, List<AllowedValueYaml> allowedList) {
      this.name = name;
      this.value = value;
      this.valueType = valueType;
      this.allowedList = allowedList;
    }
  }

  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  public static class Yaml extends AbstractYaml {
    @Builder
    public Yaml(String name, String value, String valueType, List<AllowedValueYaml> allowedValueYamlList) {
      super(name, value, valueType, allowedValueYamlList);
    }
  }
}
