package software.wings.service.intfc;

import java.io.File;

import software.wings.beans.Artifact;
import software.wings.beans.PageRequest;
import software.wings.beans.PageResponse;

public interface ArtifactService {
  public PageResponse<Artifact> list(PageRequest<Artifact> pageRequest);

  public Artifact create(String applicationId, String releaseId, String artifactSourceName);

  public File download(String applicationId, String artifactId);

  public Artifact get(String applicationId, String artifactId);
}
