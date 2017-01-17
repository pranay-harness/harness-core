package software.wings.service.intfc;

import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.ServiceInstance;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.utils.validation.Create;
import software.wings.utils.validation.Update;

import java.util.List;
import java.util.Map;
import javax.validation.Valid;

/**
 * Created by anubhaw on 1/10/17.
 */
public interface InfrastructureMappingService {
  /**
   * List page response.
   *
   * @param pageRequest the page request
   * @return the page response
   */
  PageResponse<InfrastructureMapping> list(PageRequest<InfrastructureMapping> pageRequest);

  /**
   * Save infrastructure mapping.
   *
   * @param infrastructureMapping the infrastructure mapping
   * @return the infrastructure mapping
   */
  @ValidationGroups(Create.class) InfrastructureMapping save(@Valid InfrastructureMapping infrastructureMapping);

  /**
   * Get infrastructure mapping.
   *
   * @param appId          the app id
   * @param envId          the env id
   * @param infraMappingId the infra mapping id  @return the infrastructure mapping
   * @return the infrastructure mapping
   */
  InfrastructureMapping get(String appId, String envId, String infraMappingId);

  /**
   * Update.
   *
   * @param infrastructureMapping the infrastructure mapping
   * @return the infrastructure mapping
   */
  @ValidationGroups(Update.class) InfrastructureMapping update(@Valid InfrastructureMapping infrastructureMapping);

  /**
   * Delete.
   *
   * @param appId          the app id
   * @param envId          the env id
   * @param infraMappingId the infra mapping id
   */
  void delete(String appId, String envId, String infraMappingId);

  /**
   * Gets infra mapping stencils.
   *
   * @param appId the app id
   * @return the infra mapping stencils
   */
  Map<String, Map<String, Object>> getInfraMappingStencils(String appId);

  /**
   * List service instances list.
   *
   * @param appId             the app id
   * @param serviceId         the service id
   * @param envId             the env id
   * @param computeProviderId the compute provider id
   * @param selectionParams   the selection params   @return the list
   * @return the list
   */
  List<ServiceInstance> selectServiceInstances(
      String appId, String serviceId, String envId, String computeProviderId, Map<String, Object> selectionParams);

  /**
   * List compute provider hosts list.
   *
   * @param appId             the app id
   * @param envId             the env id
   * @param serviceId         the service id
   * @param computeProviderId the compute provider id
   * @return the list
   */
  List<String> listComputeProviderHosts(String appId, String envId, String serviceId, String computeProviderId);
}
