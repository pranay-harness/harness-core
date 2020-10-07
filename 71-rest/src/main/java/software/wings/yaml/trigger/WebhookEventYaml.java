package software.wings.yaml.trigger;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.yaml.BaseYaml;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@OwnedBy(CDC)
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
