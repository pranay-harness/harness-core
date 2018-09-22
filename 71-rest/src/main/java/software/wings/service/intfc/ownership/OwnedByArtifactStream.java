package software.wings.service.intfc.ownership;

public interface OwnedByArtifactStream {
  /**
   * Prune if belongs to artifact stream.
   *
   * @param appId the app id
   * @param artifactStreamId the artifact stream id
   */
  void pruneByArtifactStream(String appId, String artifactStreamId);
}
