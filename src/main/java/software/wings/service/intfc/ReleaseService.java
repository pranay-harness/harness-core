package software.wings.service.intfc;

import software.wings.beans.Application;
import software.wings.beans.ArtifactSource;
import software.wings.beans.PageRequest;
import software.wings.beans.PageResponse;
import software.wings.beans.Release;

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

  Release update(Release release);

  <T extends ArtifactSource> Release addArtifactSource(String uuid, @Valid T artifactSource);

  <T extends ArtifactSource> Release deleteArtifactSource(String uuid, @Valid T artifactSource);

  void delete(String appId);
}
