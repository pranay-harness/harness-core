package software.wings.service.intfc;

import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.helpers.ext.jenkins.BuildDetails;

/**
 * The interface Artifact collection service.
 */
public interface ArtifactCollectionService {
  /***
   * Collects artifact from Meta-Data
   * @param appId the app id
   * @param artifactStreamId the artifact stream id
   * @param buildDetails the build details
   * @return artifact
   */
  Artifact collectArtifact(String appId, String artifactStreamId, BuildDetails buildDetails);

  /**
   * Collects new artifacts for the given artifact stream
   *
   * @param appId          the app id
   * @param artifactStream the artifact stream
   * @param permitId       the permit id
   */
  void collectNewArtifactsAsync(String appId, ArtifactStream artifactStream, String permitId);

  /**
   * Collect new artifacts artifact.
   *
   * @param appId          the app id
   * @param artifactStream the artifact stream
   * @param buildNumber    the build number
   * @return the artifact
   */
  Artifact collectNewArtifacts(String appId, ArtifactStream artifactStream, String buildNumber);
}
