package software.wings.service.intfc.trigger;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.validation.Create;
import io.harness.validation.Update;
import org.hibernate.validator.constraints.NotEmpty;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.trigger.DeploymentTrigger;
import software.wings.beans.trigger.TriggerArtifactVariable;
import software.wings.beans.trigger.TriggerExecution;
import software.wings.beans.trigger.WebhookSource;
import software.wings.service.intfc.ownership.OwnedByApplication;
import software.wings.service.intfc.ownership.OwnedByArtifactStream;
import software.wings.service.intfc.ownership.OwnedByPipeline;
import software.wings.service.intfc.ownership.OwnedByWorkflow;

import java.util.List;
import java.util.Map;
import javax.validation.Valid;

@OwnedBy(CDC)
public interface DeploymentTriggerService
    extends OwnedByApplication, OwnedByPipeline, OwnedByArtifactStream, OwnedByWorkflow {
  @ValidationGroups(Create.class) DeploymentTrigger save(@Valid DeploymentTrigger deploymentTrigger, boolean migration);

  @ValidationGroups(Update.class) DeploymentTrigger update(@Valid DeploymentTrigger trigger);

  PageResponse<DeploymentTrigger> list(PageRequest<DeploymentTrigger> pageRequest, boolean withTags, String tagFilter);

  void delete(@NotEmpty String appId, @NotEmpty String triggerId);

  DeploymentTrigger get(@NotEmpty String appId, @NotEmpty String triggerId, boolean readPrimaryVariablesValueNames);

  DeploymentTrigger getWithoutRead(@NotEmpty String appId, @NotEmpty String triggerId);

  Map<String, WebhookSource.WebhookEventInfo> fetchWebhookChildEvents(String webhookSource);

  Map<String, String> fetchCustomExpressionList(String webhookSource);

  PageResponse<DeploymentTrigger> list(PageRequest<DeploymentTrigger> pageRequest);

  DeploymentTrigger getTriggerByWebhookToken(String token);

  void triggerExecutionPostArtifactCollectionAsync(
      String accountId, String appId, String artifactStreamId, List<Artifact> artifacts);

  /**
   * Gets the cron expression
   *
   * @param expression
   * @return
   */
  String getCronDescription(String expression);

  void triggerScheduledExecutionAsync(DeploymentTrigger trigger);

  void triggerExecutionPostPipelineCompletionAsync(String appId, String pipelineId);

  List<String> getTriggersHasPipelineAction(String appId, String pipelineId);

  List<String> getTriggersHasWorkflowAction(String appId, String workflowId);

  List<String> getTriggersHasArtifactStreamAction(String accountId, String appId, String artifactStreamId);

  WorkflowExecution triggerExecutionByWebHook(DeploymentTrigger deploymentTrigger, Map<String, String> parameters,
      List<TriggerArtifactVariable> artifactVariables, TriggerExecution triggerExecution);
}