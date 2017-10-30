package software.wings.yaml.settingAttribute;

import lombok.Data;
import software.wings.beans.GcpConfig;
import software.wings.beans.SettingAttribute;
import software.wings.yaml.YamlSerialize;

@Data
public class GcpYaml extends SettingAttributeYaml {
  @YamlSerialize private char[] serviceAccountKeyFileContent;

  public GcpYaml() {
    super();
  }

  public GcpYaml(SettingAttribute settingAttribute) {
    super(settingAttribute);

    GcpConfig gcpConfig = (GcpConfig) settingAttribute.getValue();
    this.serviceAccountKeyFileContent = gcpConfig.getServiceAccountKeyFileContent();
  }
}