package software.wings.yaml.settingAttribute;

import static software.wings.yaml.YamlHelper.ENCRYPTED_VALUE_STR;

import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.beans.HostConnectionAttributes;
import software.wings.beans.SettingAttribute;

@Data
@EqualsAndHashCode(callSuper = true)
public class HostConnectionAttributesYaml extends SettingAttributeYaml {
  private String connectionType;
  private String accessType;
  private String userName;
  private String key = ENCRYPTED_VALUE_STR;

  public HostConnectionAttributesYaml() {
    super();
  }

  public HostConnectionAttributesYaml(SettingAttribute settingAttribute) {
    super(settingAttribute);

    HostConnectionAttributes hostConnectionAttributes = (HostConnectionAttributes) settingAttribute.getValue();
    this.connectionType = hostConnectionAttributes.getConnectionType().name();
    this.accessType = hostConnectionAttributes.getAccessType().name();
    this.userName = hostConnectionAttributes.getUserName();
    this.key = hostConnectionAttributes.getEncryptedKey();
  }
}