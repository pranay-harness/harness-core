package software.wings.service.intfc;

import static software.wings.beans.artifact.Artifact.Status;

import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.Artifact.ContentStatus;
import software.wings.beans.artifact.ArtifactFile;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.service.intfc.ownership.OwnedByApplication;

import java.io.File;
import java.util.List;
import javax.validation.Valid;

/**
 * The Interface ArtifactService.
 */
public interface ArtifactService extends OwnedByApplication {
  /**
   * List.
   *
   * @param pageRequest  the page request
   * @param withServices the with services
   * @return the page response
   */
  PageResponse<Artifact> list(PageRequest<Artifact> pageRequest, boolean withServices);

  /***
   * List artifact sort by build nos
   * @param pageRequest
   * @return
   */
  PageResponse<Artifact> listSortByBuildNo(PageRequest<Artifact> pageRequest);

  /**
   * Creates the artifact and validates artifact type
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
  void updateStatus(String artifactId, String appId, Status status);

  /**
   * Update status.
   *
   * @param artifactId the artifact id
   * @param appId      the app id
   * @param status     the status
   */
  void updateStatus(String artifactId, String appId, Status status, String errorMessage);

  /***
   * Update status and content status
   * @param artifactId
   * @param appId
   * @param status
   * @param contentStatus
   */
  void updateStatus(String artifactId, String appId, Status status, ContentStatus contentStatus);

  /***
   * Update status
   * @param artifactId
   * @param appId
   * @param status
   * @param contentStatus
   * @param errorMessage
   */
  void updateStatus(String artifactId, String appId, Status status, ContentStatus contentStatus, String errorMessage);

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
   * Fetch latest artifact for artifact stream artifact.
   *
   * @param appId              the app id
   * @param artifactStreamId   the artifact stream id
   * @param artifactSourceName the artifact source name
   * @return the artifact
   */
  Artifact fetchLatestArtifactForArtifactStream(String appId, String artifactStreamId, String artifactSourceName);

  /**
   * Fetch latest artifact for artifact stream artifact.
   *
   * @param appId              the app id
   * @param artifactStreamId   the artifact stream id
   * @param artifactSourceName the artifact source name
   * @return the artifact
   */
  Artifact fetchLastCollectedArtifactForArtifactStream(
      String appId, String artifactStreamId, String artifactSourceName);

  /**
   * Gets artifact by build number.
   *
   * @param appId            the app id
   * @param artifactStreamId the artifact stream id
   * @param buildNumber      the build number
   * @return the artifact by build number
   */
  Artifact getArtifactByBuildNumber(String appId, String artifactStreamId, String buildNumber);

  /**
   * Gets artifact by build number.
   *
   * @param appId            the app id
   * @param artifactStreamId the artifact stream id
   * @param buildNumber      the build number
   * @return the artifact by build number
   */
  Artifact getArtifactByBuildNumber(String appId, String artifactStreamId, String artifactSource, String buildNumber);

  /**
   * Starts Artifact collection and returns
   * @param appId
   * @param artifactId
   */
  Artifact startArtifactCollection(String appId, String artifactId);

  /**
   * Gets content status if artifact does not have content status
   * @param artifact
   * @return
   */
  ContentStatus getArtifactContentStatus(Artifact artifact);

  /**
   * Delete by artifact stream.
   *
   * @param retentionSize the size of the artifacts to be retained
   */
  void deleteArtifacts(int retentionSize);

  /**
   * Delete by artifact files.
   */
  void deleteArtifactFiles();
}
