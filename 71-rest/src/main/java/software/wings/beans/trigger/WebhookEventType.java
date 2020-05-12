package software.wings.beans.trigger;

import io.harness.exception.InvalidRequestException;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

public enum WebhookEventType {
  PULL_REQUEST("On Pull Request", "pull_request"),
  PUSH("On Push", "push"),
  REPO("On Repo", "repo"),
  ISSUE("On Issue", "issue"),
  PING("On Ping", "ping"),
  DELETE("On Delete", "delete"),
  ANY("Any", "any"),
  OTHER("Other", "other"),
  RELEASE("On Release", "release"),
  PACKAGE("On Package", "package");

  @Getter private String displayName;
  @Getter private String value;

  WebhookEventType(String displayName, String value) {
    this.displayName = displayName;
    this.value = value;
    WebhookHolder.map.put(value, this);
  }

  private static class WebhookHolder { static Map<String, WebhookEventType> map = new HashMap<>(); }

  public static WebhookEventType find(String val) {
    WebhookEventType t = WebhookHolder.map.get(val);
    if (t == null) {
      throw new InvalidRequestException(String.format("Unsupported Webhook Event Type %s.", val));
    }
    return t;
  }
}
