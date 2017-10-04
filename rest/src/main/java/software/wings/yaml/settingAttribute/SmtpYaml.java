package software.wings.yaml.settingAttribute;

import static software.wings.yaml.YamlHelper.ENCRYPTED_VALUE_STR;

import software.wings.beans.SettingAttribute;
import software.wings.helpers.ext.mail.SmtpConfig;
import software.wings.yaml.YamlSerialize;

public class SmtpYaml extends SettingAttributeYaml {
  @YamlSerialize private String host;
  @YamlSerialize private int port;
  @YamlSerialize private String fromAddress;
  @YamlSerialize private boolean useSSL;
  @YamlSerialize private String username;
  @YamlSerialize private String password = ENCRYPTED_VALUE_STR;

  public SmtpYaml() {
    super();
  }

  public SmtpYaml(SettingAttribute settingAttribute) {
    super(settingAttribute);

    SmtpConfig smtpConfig = (SmtpConfig) settingAttribute.getValue();
    this.host = smtpConfig.getHost();
    this.port = smtpConfig.getPort();
    this.fromAddress = smtpConfig.getFromAddress();
    this.useSSL = smtpConfig.isUseSSL();
    this.username = smtpConfig.getUsername();
  }
}