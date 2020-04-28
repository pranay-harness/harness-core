package software.wings.yaml.trigger;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
@JsonTypeName("WEBHOOK")
@JsonPropertyOrder({"harnessApiVersion"})
public class WebhookEventTriggerConditionYaml extends TriggerConditionYaml {
  private String repositoryType;
  private List<String> eventType = new ArrayList<>();
  private List<String> action = new ArrayList<>();
  private List<String> releaseActions = new ArrayList<>();
  private String branchName;

  public WebhookEventTriggerConditionYaml() {
    super.setType("WEBHOOK");
  }

  @lombok.Builder
  WebhookEventTriggerConditionYaml(String repositoryType, String branchName, List<String> eventType,
      List<String> action, List<String> releaseActions) {
    super.setType("WEBHOOK");
    this.eventType = eventType;
    this.action = action;
    this.releaseActions = releaseActions;
    this.repositoryType = repositoryType;
    this.branchName = branchName;
  }
}
