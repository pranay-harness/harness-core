package software.wings.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import lombok.AllArgsConstructor;
import lombok.Getter;

@OwnedBy(CDC)
@Getter
@AllArgsConstructor
public class SlackMessage {
  private String outgoingWebhookUrl;
  private String slackChannel;
  private String senderName;
  private String message;
}
