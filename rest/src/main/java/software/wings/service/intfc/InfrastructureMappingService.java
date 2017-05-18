package software.wings.service.intfc;

import com.amazonaws.services.autoscaling.model.LaunchConfiguration;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.beans.HostValidationResponse;
import software.wings.beans.HostValidationRequest;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.ServiceInstance;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.utils.validation.Create;
import software.wings.utils.validation.Update;

import java.util.List;
import java.util.Map;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

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
   * @param infraMappingId the infra mapping id  @return the infrastructure mapping
   * @return the infrastructure mapping
   */
  InfrastructureMapping get(String appId, String infraMappingId);

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
  Map<String, Object> getInfraMappingStencils(String appId);

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

  /**
   * List launch configs list.
   *
   * @param appId             the app id
   * @param envId             the env id
   * @param serviceId         the service id
   * @param computeProviderId the compute provider id
   * @return the list
   */
  List<LaunchConfiguration> listLaunchConfigs(String appId, String envId, String serviceId, String computeProviderId);

  /**
   * Gets infra mapping by compute provider and service id.
   *
   * @param appId             the app id
   * @param envId             the env id
   * @param serviceId         the service id
   * @param computeProviderId the computer provider id
   * @return the infra mapping by compute provider and service id
   */
  InfrastructureMapping getInfraMappingByComputeProviderAndServiceId(
      String appId, String envId, String serviceId, String computeProviderId);

  /**
   * Provision nodes list.
   *
   * @param appId              the app id
   * @param envId              the env id
   * @param infraMappingId     the infra mapping id
   * @param launcherConfigName the launcher config name
   * @param instanceCount      the instance count
   * @return the list
   */
  List<ServiceInstance> provisionNodes(
      String appId, String envId, String infraMappingId, String launcherConfigName, int instanceCount);

  /**
   * De provision nodes.
   *
   * @param appId             the app id
   * @param serviceId         the service id
   * @param envId             the env id
   * @param computeProviderId the compute provider id
   * @param hostNames         the host names
   */
  void deProvisionNodes(String appId, String serviceId, String envId, String computeProviderId, List<String> hostNames);

  /**
   * List types map.
   *
   * @param appId     the app id
   * @param envId     the env id
   * @param serviceId the service id
   * @return the map
   */
  Map<String, Map<String, String>> listInfraTypes(String appId, String envId, String serviceId);

  /**
   * Select service instances list.
   *
   * @param appId           the app id
   * @param envId           the env id
   * @param infraMappingId  the infra mapping id
   * @param selectionParams the selection params
   * @return the list
   */
  List<ServiceInstance> selectServiceInstances(
      String appId, String envId, String infraMappingId, Map<String, Object> selectionParams);

  /**
   * List clusters list.
   *
   * @param appId             the app id
   * @param deploymentType    the deployment type
   * @param computeProviderId the compute provider id
   * @param region            the region
   * @return the list
   */
  List<String> listClusters(String appId, String deploymentType, String computeProviderId, String region);

  /**
   * List images list.
   *
   * @param appId             the app id
   * @param deploymentType    the deployment type
   * @param computeProviderId the compute provider id
   * @param region            the region
   * @return the list
   */
  List<String> listImages(String appId, String deploymentType, String computeProviderId, String region);

  /**
   * List regions list.
   *
   * @param appId             the app id
   * @param deploymentType    the deployment type
   * @param computeProviderId the compute provider id
   * @return the list
   */
  List<String> listRegions(String appId, String deploymentType, String computeProviderId);

  /**
   * List instance types list.
   *
   * @param appId             the app id
   * @param deploymentType    the deployment type
   * @param computeProviderId the compute provider id
   * @return the list
   */
  List<String> listInstanceTypes(String appId, String deploymentType, String computeProviderId);

  /**
   * List instance roles list.
   *
   * @param appId             the app id
   * @param deploymentType    the deployment type
   * @param computeProviderId the compute provider id
   * @return the list
   */
  List<String> listInstanceRoles(String appId, String deploymentType, String computeProviderId);

  /**
   * List all roles map.
   *
   * @param appId             the app id
   * @param deploymentType    the deployment type
   * @param computeProviderId the compute provider id
   * @return the map
   */
  Map<String, String> listAllRoles(String appId, String deploymentType, String computeProviderId);

  /**
   * List networks list.
   *
   * @param appId             the app id
   * @param deploymentType    the deployment type
   * @param computeProviderId the compute provider id
   * @param region            the region
   * @return the list
   */
  List<String> listNetworks(String appId, String deploymentType, String computeProviderId, String region);

  /**
   * List load balancers map.
   *
   * @param appId             the app id
   * @param deploymentType    the deployment type
   * @param computeProviderId the compute provider id
   * @return the map
   */
  Map<String, String> listLoadBalancers(String appId, String deploymentType, String computeProviderId);

  /**
   * List classic load balancers list.
   *
   * @param appId             the app id
   * @param computeProviderId the compute provider id
   * @param region            the region
   * @return the list
   */
  List<String> listClassicLoadBalancers(String appId, String computeProviderId, String region);

  /**
   * List target groups map.
   *
   * @param appId             the app id
   * @param deploymentType    the deployment type
   * @param computeProviderId the compute provider id
   * @param loadBalancerName  the load balancer name
   * @return the map
   */
  Map<String, String> listTargetGroups(
      String appId, String deploymentType, String computeProviderId, String loadBalancerName);

  /**
   * Validate host host name validation response.
   *
   * @param validationRequest the validation request
   * @return the host name validation response
   */
  List<HostValidationResponse> validateHost(@Valid HostValidationRequest validationRequest);

  /**
   * List elastic load balancer list.
   *
   * @param accessKey the access key
   * @param secretKey the secret key
   * @param region    the region
   * @return the list
   */
  List<String> listElasticLoadBalancer(@NotNull String accessKey, @NotNull char[] secretKey, @NotNull String region);
}
