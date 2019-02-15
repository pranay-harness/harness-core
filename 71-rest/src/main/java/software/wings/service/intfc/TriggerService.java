package software.wings.service.intfc;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.beans.WorkflowType;
import io.harness.validation.Create;
import io.harness.validation.Update;
import org.hibernate.validator.constraints.NotEmpty;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.beans.WebHookToken;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.trigger.Trigger;
import software.wings.beans.trigger.TriggerExecution;
import software.wings.beans.trigger.WebhookEventType;
import software.wings.beans.trigger.WebhookParameters;
import software.wings.beans.trigger.WebhookSource;
import software.wings.helpers.ext.trigger.response.TriggerResponse;
import software.wings.service.intfc.ownership.OwnedByApplication;
import software.wings.service.intfc.ownership.OwnedByArtifactStream;
import software.wings.service.intfc.ownership.OwnedByPipeline;
import software.wings.service.intfc.ownership.OwnedByWorkflow;

import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.validation.Valid;

/**
 * Created by sgurubelli on 10/26/17.
 */
public interface TriggerService extends OwnedByApplication, OwnedByPipeline, OwnedByArtifactStream, OwnedByWorkflow {
  /**
   * List.
   *
   * @param pageRequest the req
   * @return the page response
   */
  PageResponse<Trigger> list(PageRequest<Trigger> pageRequest);

  /**
   * Get artifact stream.
   *
   * @param appId     the app id
   * @param triggerId the id
   * @return the artifact stream
   */
  Trigger get(String appId, String triggerId);

  /**
   * Create artifact stream.
   *
   * @param trigger the artifact stream
   * @return the artifact stream
   */
  @ValidationGroups(Create.class) Trigger save(@Valid Trigger trigger);

  /**
   * Update artifact stream.
   *
   * @param trigger the artifact stream
   * @return the artifact stream
   */
  @ValidationGroups(Update.class) Trigger update(@Valid Trigger trigger);

  /**
   * Delete.
   *
   * @param appId     the app id
   * @param triggerId the id
   * @return true, if successful
   */
  boolean delete(@NotEmpty String appId, @NotEmpty String triggerId);

  /**
   * Generate web hook token web hook token.
   *
   * @param appId     the app id
   * @param triggerId the stream id
   * @return the web hook token
   */
  WebHookToken generateWebHookToken(String appId, String triggerId);

  void triggerExecutionPostArtifactCollectionAsync(String appId, String artifactStreamId, List<Artifact> artifacts);

  /**
   * Trigger post pipeline completion async
   *
   * @param appId
   * @param pipelineId
   */
  void triggerExecutionPostPipelineCompletionAsync(String appId, String pipelineId);

  /***
   * Trigger
   * @param trigger
   * @param scheduledFireTime
   */
  void triggerScheduledExecutionAsync(Trigger trigger, Date scheduledFireTime);

  /**
   * Trigger execution by webhook with the given service build numbers
   *
   * @param appId
   * @param webHookToken
   * @param serviceBuildNumbers
   * @param parameters
   * @param triggerExecution
   * @return
   */
  WorkflowExecution triggerExecutionByWebHook(String appId, String webHookToken,
      Map<String, String> serviceBuildNumbers, Map<String, String> parameters, TriggerExecution triggerExecution);

  /**
   * Triggers that have actions on Pipeline
   *
   * @param appId
   * @param pipelineId
   * @return List<Trigger></Trigger>
   */
  List<Trigger> getTriggersHasPipelineAction(String appId, String pipelineId);

  /**
   * Triggers that have actions on Pipeline
   *
   * @param appId
   * @param workflowId
   * @return List<Trigger></Trigger>
   */
  List<Trigger> getTriggersHasWorkflowAction(String appId, String workflowId);

  /**
   * Triggers that have actions on Artifact Stream
   *
   * @param appId
   * @param artifactStreamId
   * @return List<Trigger></Trigger>
   */
  List<Trigger> getTriggersHasArtifactStreamAction(String appId, String artifactStreamId);

  /**
   * Updates by App to resync the filed names with the updated values
   *
   * @param appId
   */
  void updateByApp(String appId);

  /**
   * Gets the cron expression
   *
   * @param expression
   * @return
   */
  String getCronDescription(String expression);

  /**
   * Get trigger
   * @param token
   * @return
   */
  Trigger getTriggerByWebhookToken(String token);

  /***
   * Trigger execution by webhook
   * @param trigger
   * @param parameters
   * @param triggerExecution
   * @return
   */
  WorkflowExecution triggerExecutionByWebHook(
      Trigger trigger, Map<String, String> parameters, TriggerExecution triggerExecution);

  /***
   *
   * @param appId
   * @param workflowId
   * @param workflowType
   * @param webhookSource
   * @param eventType
   * @return List of webhook parameters
   */
  WebhookParameters listWebhookParameters(String appId, String workflowId, WorkflowType workflowType,
      WebhookSource webhookSource, WebhookEventType eventType);

  /**
   * Trigger execution by service infra
   * @param appId
   * @param infraMappingId
   */
  boolean triggerExecutionByServiceInfra(String appId, String infraMappingId);

  List<String> obtainTriggerNamesReferencedByTemplatedEntityId(String appId, @NotEmpty String entityId);

  void handleTriggerTaskResponse(
      @NotEmpty String appId, @NotEmpty String triggerExecutionId, TriggerResponse triggerResponse);
}
