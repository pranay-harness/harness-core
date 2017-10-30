package software.wings.yaml;

import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.GcpConfig;
import software.wings.beans.SettingAttribute;

@Data
public class GoogleCloudPlatformYaml extends GenericYaml {
  private final Logger logger = LoggerFactory.getLogger(getClass());

  @YamlSerialize public String name;
  @YamlSerialize public char[] serviceAccountKeyFileContent;

  public GoogleCloudPlatformYaml() {}

  public GoogleCloudPlatformYaml(SettingAttribute settingAttribute) {
    this.name = settingAttribute.getName();
    this.serviceAccountKeyFileContent = ((GcpConfig) settingAttribute.getValue()).getServiceAccountKeyFileContent();
  }
}
