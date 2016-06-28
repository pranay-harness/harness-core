package software.wings.service.intfc;

import software.wings.beans.Environment;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;

// TODO: Auto-generated Javadoc

/**
 * Created by anubhaw on 4/1/16.
 */
public interface EnvironmentService {
  /**
   * List.
   *
   * @param request the request
   * @return the page response
   */
  PageResponse<Environment> list(PageRequest<Environment> request);

  /**
   * Gets the.
   *
   * @param appId the app id
   * @param envId the env id
   * @return the environment
   */
  Environment get(String appId, String envId);

  /**
   * Save.
   *
   * @param environment the environment
   * @return the environment
   */
  Environment save(Environment environment);

  /**
   * Update.
   *
   * @param environment the environment
   * @return the environment
   */
  Environment update(Environment environment);

  /**
   * Delete.
   *
   * @param appId
   * @param envId the env id
   */
  void delete(String appId, String envId);

  void deleteByAppId(String appId);
}
