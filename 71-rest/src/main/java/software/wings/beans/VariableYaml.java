package software.wings.beans;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.wings.yaml.BaseYamlWithType;

import java.util.List;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public final class VariableYaml extends BaseYamlWithType {
  private String name;
  private String description;
  private boolean mandatory;
  private String value;
  private boolean fixed;
  private String allowedValues; // todo: toYaml() don't convert to allowedValues
  private List<AllowedValueYaml> allowedList;

  @Builder
  public VariableYaml(String type, String name, String description, boolean mandatory, String value, boolean fixed,
      String allowedValues, List<AllowedValueYaml> allowedList) {
    super(type);
    this.name = name;
    this.description = description;
    this.mandatory = mandatory;
    this.value = value;
    this.fixed = fixed;
    this.allowedValues = allowedValues;
    this.allowedList = allowedList;
  }
}
