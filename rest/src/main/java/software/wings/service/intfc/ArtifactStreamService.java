package software.wings.service.intfc;

import org.hibernate.validator.constraints.NotEmpty;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.ArtifactStreamAction;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;

import javax.validation.Valid;

/**
 * ArtifactStreamService.
 *
 * @author Rishi
 */
public interface ArtifactStreamService {
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
   * @param appId the app id
   * @param artifactStreamId    the id
   * @return the artifact stream
   */
  ArtifactStream get(String appId, String artifactStreamId);

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
   * @param appId the app id
   * @param artifactStreamId    the id
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
   * @param appId      the app id
   * @param streamId   the stream id
   * @param workflowId the action id
   * @return the artifact stream
   */
  ArtifactStream deleteStreamAction(String appId, String streamId, String workflowId);

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
   * @param artifact the artifact
   */
  void triggerStreamActionAsync(Artifact artifact);
}
