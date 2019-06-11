package software.wings.beans.trigger;

import static software.wings.beans.trigger.TriggerConditionType.WEBHOOK;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.beans.WebHookToken;
import software.wings.beans.trigger.WebhookSource.BitBucketEventType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by sgurubelli on 10/25/17.
 */

@JsonTypeName("WEBHOOK")
@Data
@Builder
@EqualsAndHashCode(callSuper = false)
public class WebHookTriggerCondition extends TriggerCondition {
  private WebHookToken webHookToken;
  private String artifactStreamId;
  @Builder.Default private Map<String, String> parameters = new HashMap<>();
  private WebhookSource webhookSource;
  private List<WebhookEventType> eventTypes;
  private List<PrAction> actions;
  private List<BitBucketEventType> bitBucketEvents;
  private List<String> filePaths;
  private String gitConnectorId;
  private String branchName;
  private String branchRegex;
  private boolean checkFileContentChanged;

  public WebHookTriggerCondition() {
    super(WEBHOOK);
  }

  public WebHookTriggerCondition(WebHookToken webHookToken, String artifactStreamId, Map<String, String> parameters,
      WebhookSource webhookSource, List<WebhookEventType> eventTypes, List<PrAction> actions,
      List<BitBucketEventType> bitBucketEvents, List<String> filePaths, String gitConnectorId, String branchName,
      String branchRegex, boolean checkFileContentChanged) {
    this();
    this.webHookToken = webHookToken;
    this.artifactStreamId = artifactStreamId;
    this.parameters = parameters;
    this.webhookSource = webhookSource;
    this.eventTypes = eventTypes;
    this.actions = actions;
    this.bitBucketEvents = bitBucketEvents;
    this.filePaths = filePaths;
    this.gitConnectorId = gitConnectorId;
    this.branchName = branchName;
    this.branchRegex = branchRegex;
    this.checkFileContentChanged = checkFileContentChanged;
  }
}
