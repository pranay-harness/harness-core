package software.wings.service.intfc;

import software.wings.beans.Infra;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;

/**
 * The Interface InfraService.
 */
public interface InfraService {
  /**
   * List.
   *
   * @param pageRequest the page request
   * @return the page response
   */
  PageResponse<Infra> list(PageRequest<Infra> pageRequest);

  /**
   * Save.
   *
   * @param infra the infra
   * @return the infra
   */
  public Infra save(Infra infra);

  /**
   * Delete.
   *
   * @param appId   the app id
   * @param envId   the env id
   * @param infraId the infra id
   */
  void delete(String appId, String envId, String infraId);

  /**
   * Delete by env.
   *
   * @param appId the app id
   * @param envId the env id
   */
  void deleteByEnv(String appId, String envId);

  /**
   * Create default infra for environment.
   *
   * @param appId the app id
   * @param envId the env id
   * @return the infra
   */
  Infra createDefaultInfraForEnvironment(String appId, String envId);

  /**
   * Gets infra id by env id.
   *
   * @param appId the app id
   * @param envId the env id
   * @return the infra id by env id
   */
  String getInfraIdByEnvId(String appId, String envId);
}
