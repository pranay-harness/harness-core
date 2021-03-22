package software.wings.beans;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import software.wings.yaml.BaseEntityYaml;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@TargetModule(Module._870_CG_YAML_BEANS)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public final class AccountYaml extends BaseEntityYaml {
  private List<NameValuePairYaml> defaults = new ArrayList<>();

  @lombok.Builder
  public AccountYaml(String type, String harnessApiVersion, List<NameValuePairYaml> defaults) {
    super(type, harnessApiVersion);
    this.defaults = defaults;
  }
}
