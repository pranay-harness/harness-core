package software.wings.service.intfc;

import org.hibernate.validator.constraints.NotEmpty;
import software.wings.beans.Activity;
import software.wings.beans.Artifact;
import software.wings.beans.Host;
import software.wings.beans.InstanceCountByEnv;
import software.wings.beans.Release;
import software.wings.beans.ServiceInstance;
import software.wings.beans.ServiceTemplate;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;

import java.util.List;
import java.util.Set;

/**
 * Created by anubhaw on 5/26/16.
 */
public interface ServiceInstanceService {
  /**
   * List page response.
   *
   * @param pageRequest the page request
   * @return the page response
   */
  PageResponse<ServiceInstance> list(PageRequest<ServiceInstance> pageRequest);

  /**
   * List.
   *
   * @param pageRequest the page request
   * @param appId       the app id
   * @param envId       the env id
   * @param serviceId   @return the page response
   * @return the page response
   */
  PageResponse<ServiceInstance> list(
      PageRequest<ServiceInstance> pageRequest, String appId, String envId, String serviceId);

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

  /**
   * Gets counts by env release and template.
   *
   * @param appId            the app id
   * @param release          the release
   * @param serviceTemplates the service templates
   * @return the counts by env release and template
   */
  Iterable<InstanceCountByEnv> getCountsByEnvReleaseAndTemplate(
      String appId, Release release, Set<ServiceTemplate> serviceTemplates);

  /**
   * Gets counts by env.
   *
   * @param appId            the app id
   * @param serviceTemplates the service templates
   * @return the counts by env
   */
  Iterable<InstanceCountByEnv> getCountsByEnv(String appId, Set<ServiceTemplate> serviceTemplates);

  /**
   * Update activity.
   *
   * @param activity the activity
   */
  void updateActivity(Activity activity);

  /**
   * Gets recent artifacts.
   *
   * @param appId             the app id
   * @param envId             the env id
   * @param serviceInstanceId the service instance id
   * @return the recent artifacts
   */
  List<Artifact> getRecentArtifacts(String appId, String envId, String serviceInstanceId);

  /**
   * Gets recent activities.
   *
   * @param appId             the app id
   * @param envId             the env id
   * @param serviceInstanceId the service instance id
   * @return the recent activities
   */
  List<Activity> getRecentActivities(String appId, String envId, String serviceInstanceId);
}
