package software.wings.service.intfc;

import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.helpers.ext.jenkins.BuildDetails;

import java.util.List;

public interface ArtifactCollectionService {
  Artifact collectArtifact(String appId, String artifactStreamId, BuildDetails buildDetails);

  Artifact collectArtifact(String artifactStreamId, BuildDetails buildDetails);

  void collectNewArtifactsAsync(String appId, ArtifactStream artifactStream, String permitId);

  void collectNewArtifactsAsync(ArtifactStream artifactStream, String permitId);

  Artifact collectNewArtifacts(String appId, ArtifactStream artifactStream, String buildNumber);

  List<Artifact> collectNewArtifacts(String appId, String artifactStreamId);
}
