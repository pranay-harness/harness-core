package software.wings.service.intfc;

import software.wings.beans.ArtifactSource;
import software.wings.beans.Release;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;

import javax.validation.Valid;

/**
 * ReleaseService.
 *
 * @author Rishi
 */
public interface ReleaseService {
  PageResponse<Release> list(PageRequest<Release> req);

  Release get(String id, String appId);

  Release create(@Valid Release release);

  Release update(@Valid Release release);

  <T extends ArtifactSource> Release addArtifactSource(String uuid, @Valid T artifactSource);

  <T extends ArtifactSource> Release deleteArtifactSource(String uuid, @Valid T artifactSource);

  void delete(String appId);
}
