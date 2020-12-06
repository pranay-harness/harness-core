package software.wings.service.intfc.artifact;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import software.wings.beans.artifact.ArtifactStream;
import software.wings.helpers.ext.jenkins.BuildDetails;

import java.util.List;
import org.hibernate.validator.constraints.NotEmpty;

@OwnedBy(CDC)
public interface CustomBuildSourceService {
  List<BuildDetails> getBuilds(@NotEmpty String artifactStreamId);

  boolean validateArtifactSource(ArtifactStream artifactStream);
}
