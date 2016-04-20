package software.wings.service.intfc;

import software.wings.beans.PageRequest;
import software.wings.beans.PageResponse;
import software.wings.beans.Release;

/**
 * ReleaseService.
 *
 * @author Rishi
 */
public interface ReleaseService {
  public PageResponse<Release> list(PageRequest<Release> req);

  public Release create(Release release);

  public Release update(Release release);
}
