package software.wings.service.intfc.artifact;

import org.hibernate.validator.constraints.NotEmpty;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.helpers.ext.jenkins.BuildDetails;

import java.util.List;

public interface CustomBuildSourceService {
  List<BuildDetails> getBuilds(@NotEmpty String artifactStreamId);

  boolean validateArtifactSource(ArtifactStream artifactStream);
}
