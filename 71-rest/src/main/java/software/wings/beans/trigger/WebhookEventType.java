package software.wings.beans.trigger;

import io.harness.exception.WingsException;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

public enum WebhookEventType {
  PULL_REQUEST("Pull Request", "pull_request"),
  PUSH("Push", "push"),
  PING("Ping", "ping");

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
      throw new WingsException(String.format("Unsupported Webhook Event Type %s.", val));
    }
    return t;
  }
}
