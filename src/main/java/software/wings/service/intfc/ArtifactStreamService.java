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
   * Gets the.
   *
   * @param id    the id
   * @param appId the app id
   * @return the release
   */
  ArtifactStream get(String id, String appId);

  /**
   * Creates the.
   *
   * @param artifactStream the artifact source
   * @return the release
   */
  ArtifactStream create(@Valid ArtifactStream artifactStream);

  /**
   * Update.
   *
   * @param artifactStream the artifact source
   * @return the release
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
