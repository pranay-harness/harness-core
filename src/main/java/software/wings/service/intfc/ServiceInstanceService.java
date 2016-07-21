package software.wings.service.intfc;

import org.hibernate.validator.constraints.NotEmpty;
import software.wings.beans.Host;
import software.wings.beans.Release;
import software.wings.beans.ServiceInstance;
import software.wings.beans.ServiceTemplate;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.beans.InstanceCountByEnv;

import java.util.List;
import java.util.Set;

/**
 * Created by anubhaw on 5/26/16.
 */
public interface ServiceInstanceService {
  /**
   * List.
   *
   * @param pageRequest the page request
   * @return the page response
   */
  PageResponse<ServiceInstance> list(PageRequest<ServiceInstance> pageRequest);

  /**
   * Save.
   *
   * @param serviceInstance the service instance
   * @return the service instance
   */
  ServiceInstance save(ServiceInstance serviceInstance);

  /**
   * Update.
   *
   * @param serviceInstance the service instance
   * @return the service instance
   */
  ServiceInstance update(ServiceInstance serviceInstance);

  /**
   * Delete.
   *
   * @param appId      the app id
   * @param envId      the env id
   * @param instanceId the instance id
   */
  void delete(String appId, String envId, String instanceId);

  /**
   * Gets the.
   *
   * @param appId      the app id
   * @param envId      the env id
   * @param instanceId the instance id
   * @return the service instance
   */
  ServiceInstance get(@NotEmpty String appId, @NotEmpty String envId, @NotEmpty String instanceId);

  /**
   * Update host mappings.
   *
   * @param template     the template
   * @param addedHosts   the added hosts
   * @param deletedHosts the deleted hosts
   */
  void updateInstanceMappings(ServiceTemplate template, List<Host> addedHosts, List<Host> deletedHosts);

  /**
   * Delete by env.
   *
   * @param appId the app id
   * @param envId the env id
   */
  void deleteByEnv(String appId, String envId);

  /**
   * Delete by service template.
   *
   * @param appId      the app id
   * @param envId      the env id
   * @param templateId the template id
   */
  void deleteByServiceTemplate(String appId, String envId, String templateId);

  Iterable<InstanceCountByEnv> getCountsByEnvReleaseAndTemplate(
      String appId, Release release, Set<ServiceTemplate> serviceTemplates);

  Iterable<InstanceCountByEnv> getCountsByEnv(String appId, Set<ServiceTemplate> serviceTemplates);
}
