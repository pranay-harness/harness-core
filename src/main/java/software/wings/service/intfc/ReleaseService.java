package software.wings.service.intfc;

import org.hibernate.validator.constraints.NotEmpty;
import software.wings.beans.ArtifactSource;
import software.wings.beans.Release;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;

import javax.validation.Valid;

/**
 * ReleaseService.
 *
 * @author Rishi
 */
public interface ReleaseService {
  /**
   * List.
   *
   * @param req the req
   * @return the page response
   */
  PageResponse<Release> list(PageRequest<Release> req);

  /**
   * Gets the.
   *
   * @param id    the id
   * @param appId the app id
   * @return the release
   */
  Release get(String id, String appId);

  /**
   * Creates the.
   *
   * @param release the release
   * @return the release
   */
  Release create(@Valid Release release);

  /**
   * Update.
   *
   * @param release the release
   * @return the release
   */
  Release update(@Valid Release release);

  /**
   * Add success count.
   *
   * @param appId     the app id
   * @param releaseId the release id
   * @param envId     the env id
   * @param count     the count
   */
  void addSuccessCount(String appId, String releaseId, String envId, int count);

  /**
   * Adds the artifact source.
   *
   * @param <T>            the generic type
   * @param id             the id
   * @param appId          the app id
   * @param artifactSource the artifact source
   * @return the release
   */
  <T extends ArtifactSource> Release addArtifactSource(
      @NotEmpty String id, @NotEmpty String appId, @Valid T artifactSource);

  /**
   * Update artifact source release.
   *
   * @param id             the id
   * @param appId          the app id
   * @param artifactSource the artifact source
   * @return the release
   */
  Release updateArtifactSource(String id, String appId, ArtifactSource artifactSource);

  /**
   * Delete artifact source.
   *
   * @param <T>                the generic type
   * @param id                 the id
   * @param appId              the app id
   * @param artifactSourceName the artifact source name
   * @return the release
   */
  <T extends ArtifactSource> Release deleteArtifactSource(
      @NotEmpty String id, @NotEmpty String appId, @NotEmpty String artifactSourceName);

  /**
   * Delete.
   *
   * @param id    the id
   * @param appId the app id
   * @return true, if successful
   */
  boolean delete(@NotEmpty String id, @NotEmpty String appId);

  /**
   * Soft delete.
   *
   * @param id    the id
   * @param appId the app id
   * @return the release
   */
  Release softDelete(String id, String appId);
}
