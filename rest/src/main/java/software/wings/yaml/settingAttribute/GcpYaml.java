package software.wings.yaml.settingAttribute;

import lombok.Data;
import software.wings.beans.GcpConfig;
import software.wings.beans.SettingAttribute;

@Data
public class GcpYaml extends SettingAttributeYaml {
  private String serviceAccountKeyFileContent;

  public GcpYaml() {
    super();
  }

  public GcpYaml(SettingAttribute settingAttribute) {
    super(settingAttribute);

    GcpConfig gcpConfig = (GcpConfig) settingAttribute.getValue();
    this.serviceAccountKeyFileContent = new String("dummy_config");
    // new String(gcpConfig.getServiceAccountKeyFileContent());
  }
}