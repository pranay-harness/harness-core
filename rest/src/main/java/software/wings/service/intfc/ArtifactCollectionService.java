package software.wings.service.intfc;

import software.wings.beans.artifact.Artifact;
import software.wings.helpers.ext.jenkins.BuildDetails;

import java.util.List;

public interface ArtifactCollectionService {
  /***
   * Collects artifact from Meta-Data
   * @param appId
   * @param buildDetails
   * @return
   */
  Artifact collectArtifact(String appId, String artifactStreamId, BuildDetails buildDetails);

  /**
   * Collects new artifacts for the given artifact stream
   * @param appId
   * @param artifactStreamId
   */
  List<Artifact> collectNewArtifacts(String appId, String artifactStreamId);
}
