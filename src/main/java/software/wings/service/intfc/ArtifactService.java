package software.wings.service.intfc;

import software.wings.beans.Artifact;
import software.wings.beans.PageRequest;
import software.wings.beans.PageResponse;

import java.io.File;
import javax.validation.Valid;

public interface ArtifactService {
  PageResponse<Artifact> list(PageRequest<Artifact> pageRequest);

  Artifact create(@Valid Artifact artifact);

  Artifact update(@Valid Artifact artifact);

  File download(String appId, String artifactId, String serviceId);

  Artifact get(String appId, String artifactId);
}
