package software.wings.yaml.setting;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.wings.settings.SettingValue;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public abstract class VerificationProviderYaml extends SettingValue.Yaml {
  public VerificationProviderYaml(String type, String harnessApiVersion) {
    super(type, harnessApiVersion);
  }
}