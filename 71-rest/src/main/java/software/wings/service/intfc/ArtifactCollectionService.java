package software.wings.service.intfc;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.helpers.ext.jenkins.BuildDetails;

import java.util.List;

@OwnedBy(CDC)
public interface ArtifactCollectionService {
  Artifact collectArtifact(String artifactStreamId, BuildDetails buildDetails);

  void collectNewArtifactsAsync(ArtifactStream artifactStream, String permitId);

  Artifact collectNewArtifacts(String appId, ArtifactStream artifactStream, String buildNumber);

  List<Artifact> collectNewArtifacts(String appId, String artifactStreamId);
}
