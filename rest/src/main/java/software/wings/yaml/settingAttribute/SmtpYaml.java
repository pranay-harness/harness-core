package software.wings.yaml.settingAttribute;

import static software.wings.yaml.YamlHelper.ENCRYPTED_VALUE_STR;

import software.wings.beans.SettingAttribute;
import software.wings.helpers.ext.mail.SmtpConfig;

public class SmtpYaml extends SettingAttributeYaml {
  private String host;
  private int port;
  private String fromAddress;
  private boolean useSSL;
  private String username;
  private String password = ENCRYPTED_VALUE_STR;

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

  public String getHost() {
    return host;
  }

  public void setHost(String host) {
    this.host = host;
  }

  public int getPort() {
    return port;
  }

  public void setPort(int port) {
    this.port = port;
  }

  public String getFromAddress() {
    return fromAddress;
  }

  public void setFromAddress(String fromAddress) {
    this.fromAddress = fromAddress;
  }

  public boolean isUseSSL() {
    return useSSL;
  }

  public void setUseSSL(boolean useSSL) {
    this.useSSL = useSSL;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }
}