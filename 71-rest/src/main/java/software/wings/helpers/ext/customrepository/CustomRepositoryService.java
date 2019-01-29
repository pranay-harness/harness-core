package software.wings.helpers.ext.customrepository;

import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.helpers.ext.jenkins.BuildDetails;

import java.util.List;

public interface CustomRepositoryService {
  List<BuildDetails> getBuilds(ArtifactStreamAttributes artifactStreamAttributes);
}
