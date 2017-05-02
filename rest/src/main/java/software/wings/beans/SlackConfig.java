package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.github.reinert.jjschema.Attributes;
import software.wings.settings.SettingValue;

/**
 * Created by anubhaw on 12/14/16.
 */
@JsonTypeName("SLACK")
public class SlackConfig extends SettingValue {
  @Attributes(title = "Slack Webhook URL") private String outgoingWebhookUrl;

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
  public String getOutgoingWebhookUrl() {
    return outgoingWebhookUrl;
  }

  /**
   * Sets incoming webhook url.
   *
   * @param outgoingWebhookUrl the incoming webhook url
   */
  public void setOutgoingWebhookUrl(String outgoingWebhookUrl) {
    this.outgoingWebhookUrl = outgoingWebhookUrl;
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private String outgoingWebhookUrl;

    private Builder() {}

    /**
     * A slack config builder.
     *
     * @return the builder
     */
    public static Builder aSlackConfig() {
      return new Builder();
    }

    /**
     * With outgoing webhook url builder.
     *
     * @param outgoingWebhookUrl the outgoing webhook url
     * @return the builder
     */
    public Builder withOutgoingWebhookUrl(String outgoingWebhookUrl) {
      this.outgoingWebhookUrl = outgoingWebhookUrl;
      return this;
    }

    /**
     * But builder.
     *
     * @return the builder
     */
    public Builder but() {
      return aSlackConfig().withOutgoingWebhookUrl(outgoingWebhookUrl);
    }

    /**
     * Build slack config.
     *
     * @return the slack config
     */
    public SlackConfig build() {
      SlackConfig slackConfig = new SlackConfig();
      slackConfig.setOutgoingWebhookUrl(outgoingWebhookUrl);
      return slackConfig;
    }
  }
}
