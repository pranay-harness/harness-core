package software.wings.service.intfc;

import org.hibernate.validator.constraints.NotEmpty;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.beans.WebHookToken;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.trigger.Trigger;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.utils.validation.Create;
import software.wings.utils.validation.Update;

import java.util.List;
import java.util.Map;
import javax.validation.Valid;

/**
 * Created by sgurubelli on 10/26/17.
 */
public interface TriggerService {
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
   * Delete by application.
   *
   * @param appId the app id
   */
  void deleteByApp(String appId);

  /**
   * Delete triggers for pipeline
   *
   * @param appId
   * @param pipelineId
   */
  void deleteTriggersForPipeline(String appId, String pipelineId);

  /**
   * Delete triggers for ArtifactStream
   *
   * @param appId
   * @param artifactStreamId
   */
  void deleteTriggersForArtifactStream(String appId, String artifactStreamId);

  /**
   * Generate web hook token web hook token.
   *
   * @param appId     the app id
   * @param triggerId the stream id
   * @return the web hook token
   */
  WebHookToken generateWebHookToken(String appId, String triggerId);

  /**
   * Trigger stream action.
   *
   * @param artifact the artifact
   */
  void triggerExecutionPostArtifactCollectionAsync(Artifact artifact);

  /**
   * Trigger post pipeline completion async
   *
   * @param appId
   * @param pipelineId
   */
  void triggerExecutionPostPipelineCompletionAsync(String appId, String pipelineId);

  /**
   * Trigger scheduled stream action.
   *
   * @param appId     the app id
   * @param triggerId the trigger id
   */
  void triggerScheduledExecutionAsync(String appId, String triggerId);

  /**
   * Trigger execution by webhook with the given service build numbers
   *
   * @param appId
   * @param webHookToken
   * @param serviceBuildNumbers
   * @param parameters
   * @return
   */
  WorkflowExecution triggerExecutionByWebHook(
      String appId, String webHookToken, Map<String, String> serviceBuildNumbers, Map<String, String> parameters);

  /**
   * Trigger execution by webhook with the given artifact
   *
   * @param appId
   * @param webHookToken
   * @param artifact
   * @param parameters
   * @return
   */
  WorkflowExecution triggerExecutionByWebHook(
      String appId, String webHookToken, Artifact artifact, Map<String, String> parameters);

  /**
   * Triggers that have actions on Pipeline
   *
   * @param appId
   * @param pipelineId
   * @return List<Trigger></Trigger>
   */
  List<Trigger> getTriggersHasPipelineAction(String appId, String pipelineId);

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
}
