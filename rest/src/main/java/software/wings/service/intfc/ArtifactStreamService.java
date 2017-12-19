package software.wings.service.intfc;

import org.hibernate.validator.constraints.NotEmpty;
import software.wings.beans.WebHookToken;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.ArtifactStreamAction;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.stencils.Stencil;

import java.util.List;
import java.util.Map;
import javax.validation.Valid;

/**
 * ArtifactStreamService.
 *
 * @author Rishi
 */
public interface ArtifactStreamService extends OwnedByService {
  /**
   * List.
   *
   * @param req the req
   * @return the page response
   */
  PageResponse<ArtifactStream> list(PageRequest<ArtifactStream> req);

  /**
   * Get artifact stream.
   *
   * @param appId            the app id
   * @param artifactStreamId the id
   * @return the artifact stream
   */
  ArtifactStream get(String appId, String artifactStreamId);

  ArtifactStream getArtifactStreamByName(String appId, String serviceId, String artifactStreamName);

  /**
   * Create artifact stream.
   *
   * @param artifactStream the artifact stream
   * @return the artifact stream
   */
  ArtifactStream create(@Valid ArtifactStream artifactStream);

  /**
   * Update artifact stream.
   *
   * @param artifactStream the artifact stream
   * @return the artifact stream
   */
  ArtifactStream update(@Valid ArtifactStream artifactStream);

  /**
   * Delete.
   *
   * @param appId            the app id
   * @param artifactStreamId the id
   * @return true, if successful
   */
  boolean delete(@NotEmpty String appId, @NotEmpty String artifactStreamId);

  /**
   * Delete by application.
   *
   * @param appId the app id
   */
  void deleteByApplication(String appId);

  /**
   * Add stream action artifact stream.
   *
   * @param appId                the app id
   * @param streamId             the stream id
   * @param artifactStreamAction the artifact stream action
   * @return the artifact stream
   */
  ArtifactStream addStreamAction(String appId, String streamId, ArtifactStreamAction artifactStreamAction);

  /**
   * Delete stream action artifact stream.
   *
   * @param appId    the app id
   * @param streamId the stream id
   * @param actionId the action id
   * @return the artifact stream
   */
  ArtifactStream deleteStreamAction(String appId, String streamId, String actionId);

  /**
   * Update stream action artifact stream.
   *
   * @param appId                the app id
   * @param streamId             the stream id
   * @param artifactStreamAction the artifact stream action
   * @return the artifact stream
   */
  ArtifactStream updateStreamAction(String appId, String streamId, ArtifactStreamAction artifactStreamAction);

  /**
   * Trigger stream action.
   *
   * @param appId             the appId
   * @param artifact             the artifact
   * @param artifactStreamAction the artifact stream action
   * @return the workflow execution
   */
  WorkflowExecution triggerStreamAction(String appId, Artifact artifact, ArtifactStreamAction artifactStreamAction);

  /**
   * Trigger stream action.
   *
   * @param artifact the artifact
   */
  void triggerStreamActionPostArtifactCollectionAsync(Artifact artifact);

  /**
   * Trigger scheduled stream action.
   *
   * @param appId      the app id
   * @param streamId   the stream id
   * @param workflowId the workflow id
   */
  void triggerScheduledStreamAction(String appId, String streamId, String workflowId);

  /**
   * Gets artifact stream schema.
   *
   * @param appId     the app id
   * @param serviceId the service id
   * @return the artifact stream schema
   */
  List<Stencil> getArtifactStreamSchema(String appId, String serviceId);

  WorkflowExecution triggerStreamAction(
      String appId, Artifact artifact, ArtifactStreamAction artifactStreamAction, Map<String, String> parameters);

  /**
   * Gets build source.
   *
   * @param appId     the app id
   * @param serviceId the service id
   * @return the build source
   */
  Map<String, String> getSupportedBuildSourceTypes(String appId, String serviceId);

  /**
   * Generate web hook token web hook token.
   *
   * @param appId    the app id
   * @param streamId the stream id
   * @return the web hook token
   */
  WebHookToken generateWebHookToken(String appId, String streamId);

  List<ArtifactStream> getArtifactStreamsForService(String appId, String serviceId);
}
