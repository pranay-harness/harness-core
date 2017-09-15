package software.wings.yaml.settingAttribute;

import software.wings.beans.GcpConfig;
import software.wings.beans.SettingAttribute;
import software.wings.yaml.YamlSerialize;

public class GcpYaml extends SettingAttributeYaml {
  @YamlSerialize private String serviceAccountKeyFileContent;

  public GcpYaml() {
    super();
  }

  public GcpYaml(SettingAttribute settingAttribute) {
    super(settingAttribute);

    GcpConfig gcpConfig = (GcpConfig) settingAttribute.getValue();
    this.serviceAccountKeyFileContent = gcpConfig.getServiceAccountKeyFileContent();
  }

  public String getServiceAccountKeyFileContent() {
    return serviceAccountKeyFileContent;
  }

  public void setServiceAccountKeyFileContent(String serviceAccountKeyFileContent) {
    this.serviceAccountKeyFileContent = serviceAccountKeyFileContent;
  }
}