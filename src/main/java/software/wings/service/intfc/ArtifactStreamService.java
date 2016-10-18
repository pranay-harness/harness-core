package software.wings.service.intfc;

import org.hibernate.validator.constraints.NotEmpty;
import software.wings.beans.artifact.ArtifactSource;
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
  PageResponse<ArtifactSource> list(PageRequest<ArtifactSource> req);

  /**
   * Gets the.
   *
   * @param id    the id
   * @param appId the app id
   * @return the release
   */
  ArtifactSource get(String id, String appId);

  /**
   * Creates the.
   *
   * @param artifactSource the artifact source
   * @return the release
   */
  ArtifactSource create(@Valid ArtifactSource artifactSource);

  /**
   * Update.
   *
   * @param artifactSource the artifact source
   * @return the release
   */
  ArtifactSource update(@Valid ArtifactSource artifactSource);

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
