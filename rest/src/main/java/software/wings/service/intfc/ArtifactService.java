package software.wings.service.intfc;

import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.ArtifactFile;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;

import java.io.File;
import java.util.List;
import javax.validation.Valid;

/**
 * The Interface ArtifactService.
 */
public interface ArtifactService {
  /**
   * List.
   *
   * @param pageRequest  the page request
   * @param withServices the with services
   * @return the page response
   */
  PageResponse<Artifact> list(PageRequest<Artifact> pageRequest, boolean withServices);

  /**
   * Creates the.
   *
   * @param artifact the artifact
   * @return the artifact
   */
  Artifact create(@Valid Artifact artifact);

  /**
   * Update.
   *
   * @param artifact the artifact
   * @return the artifact
   */
  Artifact update(@Valid Artifact artifact);

  /**
   * Update status.
   *
   * @param artifactId the artifact id
   * @param appId      the app id
   * @param status     the status
   */
  void updateStatus(String artifactId, String appId, Artifact.Status status);

  /**
   * Adds the artifact file.
   *
   * @param artifactId    the artifact id
   * @param appId         the app id
   * @param artifactFiles the artifact files
   */
  void addArtifactFile(String artifactId, String appId, List<ArtifactFile> artifactFiles);

  /**
   * Download.
   *
   * @param appId      the app id
   * @param artifactId the artifact id
   * @return the file
   */
  File download(String appId, String artifactId);

  /**
   * Gets the.
   *
   * @param appId      the app id
   * @param artifactId the artifact id
   * @return the artifact
   */
  Artifact get(String appId, String artifactId);

  /**
   * Get artifact.
   *
   * @param appId        the app id
   * @param artifactId   the artifact id
   * @param withServices the with services
   * @return the artifact
   */
  Artifact get(String appId, String artifactId, boolean withServices);

  /**
   * Soft delete.
   *
   * @param appId      the app id
   * @param artifactId the artifact id
   * @return the artifact
   */
  boolean delete(String appId, String artifactId);

  /**
   * Delete by application.
   *
   * @param appId the app id
   */
  void deleteByApplication(String appId);

  /**
   * Fetch latest artifact for artifact stream artifact.
   *
   * @param appId            the app id
   * @param artifactStreamId the artifact stream id
   * @return the artifact
   */
  Artifact fetchLatestArtifactForArtifactStream(String appId, String artifactStreamId);
}
