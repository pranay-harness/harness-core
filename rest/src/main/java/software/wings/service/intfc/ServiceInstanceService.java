package software.wings.service.intfc;

import org.hibernate.validator.constraints.NotEmpty;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.beans.Activity;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.ServiceInstance;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.infrastructure.Host;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.utils.validation.Create;

import java.util.List;
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
   * @param infraMapping the infra mapping
   * @param addedHosts   the added hosts
   * @param deletedPublicDnsNames the deleted public dns names
   */
  void updateInstanceMappings(@NotNull ServiceTemplate template, InfrastructureMapping infraMapping,
      List<Host> addedHosts, List<String> deletedPublicDnsNames);

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
   * Update activity.
   *
   * @param activity the activity
   */
  void updateActivity(@NotNull Activity activity);

  /**
   * Delete by infra mapping id.
   *
   * @param appId          the app id
   * @param infraMappingId the infra mapping id
   */
  void deleteByInfraMappingId(String appId, String infraMappingId);
}
