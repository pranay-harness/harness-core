package software.wings.beans;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SlackMessage {
  private String outgoingWebhookUrl;
  private String slackChannel;
  private String senderName;
  private String message;
}
