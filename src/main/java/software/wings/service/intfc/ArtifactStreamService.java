package software.wings.service.intfc;

import org.hibernate.validator.constraints.NotEmpty;
import software.wings.beans.artifact.ArtifactStream;
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
   * @param id    the id
   * @param appId the app id
   * @return the artifact stream
   */
  ArtifactStream get(String id, String appId);

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
   * @param id    the id
   * @param appId the app id
   * @return true, if successful
   */
  boolean delete(@NotEmpty String id, @NotEmpty String appId);

  /**
   * Delete by application.
   *
   * @param appId the app id
   */
  void deleteByApplication(String appId);
}
