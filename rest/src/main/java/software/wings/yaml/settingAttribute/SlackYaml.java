package software.wings.yaml.settingAttribute;

import software.wings.beans.SettingAttribute;
import software.wings.beans.SlackConfig;

public class SlackYaml extends SettingAttributeYaml {
  private String outgoingWebhookUrl;

  public SlackYaml() {
    super();
  }

  public SlackYaml(SettingAttribute settingAttribute) {
    super(settingAttribute);

    SlackConfig slackConfig = (SlackConfig) settingAttribute.getValue();
    this.outgoingWebhookUrl = slackConfig.getOutgoingWebhookUrl();
  }

  public String getOutgoingWebhookUrl() {
    return outgoingWebhookUrl;
  }

  public void setOutgoingWebhookUrl(String outgoingWebhookUrl) {
    this.outgoingWebhookUrl = outgoingWebhookUrl;
  }
}