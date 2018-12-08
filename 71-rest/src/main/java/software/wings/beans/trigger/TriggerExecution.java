package software.wings.beans.trigger;

import io.harness.beans.EmbeddedUser;
import lombok.Builder;
import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Indexes;
import software.wings.beans.Base;
import software.wings.beans.ExecutionArgs;
import software.wings.beans.WorkflowType;
import software.wings.beans.trigger.Condition.Type;

import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotNull;

@Data
@Entity(value = "triggerExecutions")
@Indexes(@Index(options = @IndexOptions(name = "search"), fields = { @Field("triggerId")
                                                                     , @Field("status") }))
public class TriggerExecution extends Base {
  public static final String TRIGGER_ID_KEY = "triggerId";
  public static final String WEBHOOK_TOKEN_KEY = "webhookToken";
  public static final String STATUS_KEY = "status";
  public static final String WORKFLOW_ID_KEY = "workflowId";
  public static final String WEBHOOK_EVENT_DETAILS_GIT_CONNECTOR_ID_KEY = "webhookEventDetails.gitConnectorId";
  public static final String WEBHOOK_EVENT_DETAILS_BRANCH_NAME_KEY = "webhookEventDetails.branchName";
  public static final String WEBHOOK_EVENT_DETAILS_WEBHOOK_SOURCE_KEY = "webhookEventDetails.webhookSource";

  @NotEmpty private String triggerId;
  @NotEmpty private String triggerName;
  String workflowExecutionId;
  private String workflowExecutionName;
  @NotNull @Indexed private Status status;
  private String message;
  private Condition.Type type;
  @Indexed private String webhookToken;
  private WebhookEventDetails webhookEventDetails;
  private String envId;
  private ExecutionArgs executionArgs;
  private String workflowId;
  private WorkflowType workflowType;

  @Indexed(options = @IndexOptions(expireAfterSeconds = 0))
  private Date validUntil = Date.from(OffsetDateTime.now().plusMonths(6).toInstant());

  @Builder
  public TriggerExecution(String uuid, String appId, EmbeddedUser createdBy, long createdAt, EmbeddedUser lastUpdatedBy,
      long lastUpdatedAt, List<String> keywords, String entityYamlPath, String triggerId, String triggerName,
      String workflowExecutionId, String workflowExecutionName, Status status, String message, Type type,
      String webhookToken, WebhookEventDetails webhookEventDetails) {
    super(uuid, appId, createdBy, createdAt, lastUpdatedBy, lastUpdatedAt, keywords, entityYamlPath);
    this.triggerId = triggerId;
    this.triggerName = triggerName;
    this.workflowExecutionId = workflowExecutionId;
    this.workflowExecutionName = workflowExecutionName;
    this.status = status;
    this.message = message;
    this.type = type;
    this.webhookToken = webhookToken;
    this.webhookEventDetails = webhookEventDetails;
  }

  public enum Status { FAILED, REJECTED, SUCCESS, RUNNING }

  @Data
  @Builder
  public static class WebhookEventDetails {
    String gitConnectorId;
    String branchName;
    String commitId;
    String prevCommitId;
    String payload;
    List<String> filePaths;
    String webhookSource;
    String webhookEventType;
    String prAction;
    Map<String, String> parameters;
  }
}
