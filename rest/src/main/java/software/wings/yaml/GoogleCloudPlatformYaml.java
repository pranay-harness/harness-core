package software.wings.yaml;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.GcpConfig;
import software.wings.beans.SettingAttribute;

public class GoogleCloudPlatformYaml extends GenericYaml {
  private final Logger logger = LoggerFactory.getLogger(getClass());

  @YamlSerialize public String name;
  @YamlSerialize public String serviceAccountKeyFileContent;

  public GoogleCloudPlatformYaml() {}

  public GoogleCloudPlatformYaml(SettingAttribute settingAttribute) {
    this.name = settingAttribute.getName();
    this.serviceAccountKeyFileContent = ((GcpConfig) settingAttribute.getValue()).getServiceAccountKeyFileContent();
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getServiceAccountKeyFileContent() {
    return serviceAccountKeyFileContent;
  }

  public void setServiceAccountKeyFileContent(String serviceAccountKeyFileContent) {
    this.serviceAccountKeyFileContent = serviceAccountKeyFileContent;
  }
}
