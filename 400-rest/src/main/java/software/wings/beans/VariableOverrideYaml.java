package software.wings.beans;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@TargetModule(Module._870_CG_YAML_BEANS)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public final class VariableOverrideYaml extends NameValuePairAbstractYaml {
  private String serviceName;

  @lombok.Builder
  public VariableOverrideYaml(
      String name, String value, String valueType, List<AllowedValueYaml> allowedValueYamls, String serviceName) {
    super(name, value, valueType, allowedValueYamls);
    this.serviceName = serviceName;
  }
}
