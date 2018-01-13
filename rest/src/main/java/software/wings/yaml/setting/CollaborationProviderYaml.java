package software.wings.yaml.setting;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.wings.settings.SettingValue;

/**
 * @author rktummala on 11/18/17
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public abstract class CollaborationProviderYaml extends SettingValue.Yaml {
  public CollaborationProviderYaml(String type, String harnessApiVersion) {
    super(type, harnessApiVersion);
  }
}
