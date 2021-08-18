package software.wings.yaml.trigger;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.yaml.BaseYaml;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@OwnedBy(CDC)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TargetModule(HarnessModule._815_CG_TRIGGERS)
public class WebhookEventYaml extends BaseYaml {
  private String eventType;
  private String action;

  @Builder
  public WebhookEventYaml(String eventType, String action) {
    this.eventType = eventType;
    this.action = action;
  }
}
