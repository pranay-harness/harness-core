package software.wings.beans;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
import io.harness.yaml.BaseYaml;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@TargetModule(Module._870_CG_YAML_BEANS)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public abstract class NameValuePairAbstractYaml extends BaseYaml {
  private String name;
  private String value;
  @EqualsAndHashCode.Exclude private String valueType;
  @EqualsAndHashCode.Exclude private List<AllowedValueYaml> allowedList = new ArrayList<>();

  public NameValuePairAbstractYaml(String name, String value, String valueType, List<AllowedValueYaml> allowedList) {
    this.name = name;
    this.value = value;
    this.valueType = valueType;
    this.allowedList = allowedList;
  }
}
