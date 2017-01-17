package software.wings.service.intfc;

import software.wings.beans.infrastructure.Host;
import software.wings.beans.infrastructure.Infrastructure;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;

/**
 * The Interface InfrastructureService.
 */
public interface InfrastructureService {
  /**
   * List.
   *
   * @param pageRequest the page request
   * @param overview    the overview
   * @return the page response
   */
  PageResponse<Infrastructure> list(PageRequest<Infrastructure> pageRequest, boolean overview);

  /**
   * Get infrastructure.
   *
   * @param infraId the infra id
   * @return the infrastructure
   */
  Infrastructure get(String infraId);
}
