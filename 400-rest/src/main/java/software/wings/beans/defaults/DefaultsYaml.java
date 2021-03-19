package software.wings.beans.defaults;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import software.wings.beans.NameValuePair;
import software.wings.yaml.BaseEntityYaml;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@TargetModule(Module._870_CG_YAML_BEANS)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class DefaultsYaml extends BaseEntityYaml {
  private List<NameValuePair.Yaml> defaults;

  @Builder
  public DefaultsYaml(String type, String harnessApiVersion, List<NameValuePair.Yaml> defaults) {
    super(type, harnessApiVersion);
    this.defaults = defaults;
  }
}
