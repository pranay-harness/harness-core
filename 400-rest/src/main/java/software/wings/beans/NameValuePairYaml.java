package software.wings.beans;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@TargetModule(HarnessModule._870_CG_YAML_BEANS)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class NameValuePairYaml extends NameValuePairAbstractYaml {
  @Builder
  public NameValuePairYaml(String name, String value, String valueType, List<AllowedValueYaml> allowedValueYamlList) {
    super(name, value, valueType, allowedValueYamlList);
  }
}
