package software.wings.service.intfc;

import software.wings.beans.Artifact;
import software.wings.beans.PageRequest;
import software.wings.beans.PageResponse;

import java.io.File;
import javax.validation.Valid;

public interface ArtifactService {
  public PageResponse<Artifact> list(PageRequest<Artifact> pageRequest);

  public Artifact create(@Valid Artifact artifact);

  public Artifact update(@Valid Artifact artifact);

  public File download(String appId, String artifactId);

  public Artifact get(String appId, String artifactId);
}
