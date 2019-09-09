package software.wings.yaml.trigger;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.wings.yaml.BaseYaml;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class WebhookEventYaml extends BaseYaml {
  private String eventType;
  private String action;

  @Builder
  public WebhookEventYaml(String eventType, String action) {
    this.eventType = eventType;
    this.action = action;
  }
}
