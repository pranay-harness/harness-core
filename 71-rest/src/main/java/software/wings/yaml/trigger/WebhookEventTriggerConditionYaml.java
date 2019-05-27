package software.wings.yaml.trigger;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
@JsonTypeName("WEBHOOK")
@JsonPropertyOrder({"harnessApiVersion"})
public class WebhookEventTriggerConditionYaml extends TriggerConditionYaml {
  public WebhookEventTriggerConditionYaml() {
    super.setType("WEBHOOK");
  }
}
