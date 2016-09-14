package software.wings.service.intfc;

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
   * @return the page response
   */
  PageResponse<Infrastructure> list(PageRequest<Infrastructure> pageRequest);

  /**
   * Save.
   *
   * @param infrastructure the infrastructure
   * @return the infrastructure
   */
  Infrastructure save(Infrastructure infrastructure);

  /**
   * Delete.
   *
   * @param infraId the infra id
   */
  void delete(String infraId);

  /**
   * Gets infra by env id.
   *
   * @param appId the app id
   * @param envId the env id
   * @return the infra by env id
   */
  Infrastructure getInfraByEnvId(String appId, String envId);

  /**
   * Create default infrastructure.
   */
  void createDefaultInfrastructure();

  /**
   * Gets default infrastructure id.
   *
   * @return the default infrastructure id
   */
  String getDefaultInfrastructureId();
}
