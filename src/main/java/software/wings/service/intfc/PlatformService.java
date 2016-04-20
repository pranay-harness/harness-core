package software.wings.service.intfc;

import software.wings.beans.PageRequest;
import software.wings.beans.PageResponse;
import software.wings.beans.PlatformSoftware;

/**
 * PlatformService.
 *
 * @author Rishi
 */
public interface PlatformService {
  public PageResponse<PlatformSoftware> list(PageRequest<PlatformSoftware> req);

  public PlatformSoftware create(PlatformSoftware platform);

  public PlatformSoftware update(PlatformSoftware platform);
}
