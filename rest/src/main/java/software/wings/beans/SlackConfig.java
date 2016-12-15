package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.github.reinert.jjschema.Attributes;
import software.wings.settings.SettingValue;

/**
 * Created by anubhaw on 12/14/16.
 */
@JsonTypeName("SLACK")
public class SlackConfig extends SettingValue {
  @Attributes(title = "Webhook Url") private String incomingWebhookUrl;

  /**
   * Instantiates a new setting value.
   */
  public SlackConfig() {
    super(SettingVariableTypes.SLACK.name());
  }

  /**
   * Gets incoming webhook url.
   *
   * @return the incoming webhook url
   */
  public String getIncomingWebhookUrl() {
    return incomingWebhookUrl;
  }

  /**
   * Sets incoming webhook url.
   *
   * @param incomingWebhookUrl the incoming webhook url
   */
  public void setIncomingWebhookUrl(String incomingWebhookUrl) {
    this.incomingWebhookUrl = incomingWebhookUrl;
  }
}
