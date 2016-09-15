package software.wings.service.intfc;

import org.hibernate.validator.constraints.NotEmpty;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.beans.Activity;
import software.wings.beans.ApplicationHost;
import software.wings.beans.InstanceCountByEnv;
import software.wings.beans.Release;
import software.wings.beans.ServiceInstance;
import software.wings.beans.ServiceTemplate;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.utils.validation.Create;

import java.util.List;
import java.util.Set;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

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
      PageRequest<ServiceInstance> pageRequest, @NotEmpty String appId, @NotEmpty String envId, String serviceId);

  /**
   * Save.
   *
   * @param serviceInstance the service instance
   * @return the service instance
   */
  @ValidationGroups(Create.class) ServiceInstance save(@Valid ServiceInstance serviceInstance);

  /**
   * Delete.
   *
   * @param appId      the app id
   * @param envId      the env id
   * @param instanceId the instance id
   */
  void delete(@NotEmpty String appId, @NotEmpty String envId, @NotEmpty String instanceId);

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
  void updateInstanceMappings(
      @NotNull ServiceTemplate template, List<ApplicationHost> addedHosts, List<ApplicationHost> deletedHosts);

  /**
   * Delete by env.
   *
   * @param appId the app id
   * @param envId the env id
   */
  void deleteByEnv(@NotEmpty String appId, @NotEmpty String envId);

  /**
   * Delete by service template.
   *
   * @param appId      the app id
   * @param envId      the env id
   * @param templateId the template id
   */
  void deleteByServiceTemplate(@NotEmpty String appId, @NotEmpty String envId, @NotEmpty String templateId);

  /**
   * Gets counts by env release and template.
   *
   * @param appId            the app id
   * @param release          the release
   * @param serviceTemplates the service templates
   * @return the counts by env release and template
   */
  Iterable<InstanceCountByEnv> getCountsByEnvReleaseAndTemplate(
      @NotEmpty String appId, @NotNull Release release, Set<ServiceTemplate> serviceTemplates);

  /**
   * Gets counts by env.
   *
   * @param appId            the app id
   * @param serviceTemplates the service templates
   * @return the counts by env
   */
  Iterable<InstanceCountByEnv> getCountsByEnv(@NotEmpty String appId, Set<ServiceTemplate> serviceTemplates);

  /**
   * Update activity.
   *
   * @param activity the activity
   */
  void updateActivity(@NotNull Activity activity);
}
