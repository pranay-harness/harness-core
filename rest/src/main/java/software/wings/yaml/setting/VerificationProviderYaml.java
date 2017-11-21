package software.wings.yaml.setting;

import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.settings.SettingValue;

@Data
@EqualsAndHashCode(callSuper = true)
public abstract class VerificationProviderYaml extends SettingValue.Yaml {
  public VerificationProviderYaml(String type, String name) {
    super(type, name);
  }
}