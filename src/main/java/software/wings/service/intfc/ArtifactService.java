package software.wings.service.intfc;

import software.wings.beans.Artifact;
import software.wings.beans.ArtifactFile;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;

import java.io.File;
import java.util.List;
import javax.validation.Valid;

public interface ArtifactService {
  PageResponse<Artifact> list(PageRequest<Artifact> pageRequest);

  Artifact create(@Valid Artifact artifact);

  Artifact update(@Valid Artifact artifact);

  void updateStatus(String artifactId, String appId, Artifact.Status status);

  void addArtifactFile(String artifactId, String appId, List<ArtifactFile> artifactFiles);

  File download(String appId, String artifactId, String serviceId);

  Artifact get(String appId, String artifactId);
}
