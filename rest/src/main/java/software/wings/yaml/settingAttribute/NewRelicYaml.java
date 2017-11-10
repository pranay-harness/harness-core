package software.wings.yaml.settingAttribute;

import lombok.Data;
import software.wings.beans.SettingAttribute;

@Data
public class NewRelicYaml extends SettingAttributeYaml {
  private String apiKey;

  public NewRelicYaml() {}

  public NewRelicYaml(SettingAttribute settingAttribute) {
    super(settingAttribute);
  }
}
