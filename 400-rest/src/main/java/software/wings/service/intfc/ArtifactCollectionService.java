package software.wings.service.intfc;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.helpers.ext.jenkins.BuildDetails;

import java.util.List;
import java.util.Map;

@OwnedBy(CDC)
@TargetModule(HarnessModule._870_CG_ORCHESTRATION)
public interface ArtifactCollectionService {
  Artifact collectArtifact(String artifactStreamId, BuildDetails buildDetails);

  void collectNewArtifactsAsync(ArtifactStream artifactStream, String permitId);

  Artifact collectNewArtifacts(String appId, ArtifactStream artifactStream, String buildNumber);

  Artifact collectNewArtifacts(
      String appId, ArtifactStream artifactStream, String buildNumber, Map<String, Object> artifactVariables);

  List<Artifact> collectNewArtifacts(String appId, String artifactStreamId);
}
