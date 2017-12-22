package software.wings.service.intfc;

import org.hibernate.validator.constraints.NotEmpty;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.api.DeploymentType;
import software.wings.beans.HostValidationRequest;
import software.wings.beans.HostValidationResponse;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.ServiceInstance;
import software.wings.beans.ServiceInstanceSelectionParams;
import software.wings.beans.infrastructure.Host;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.utils.validation.Create;
import software.wings.utils.validation.Update;

import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/**
 * Created by anubhaw on 1/10/17.
 */
public interface InfrastructureMappingService {
  PageResponse<InfrastructureMapping> list(PageRequest<InfrastructureMapping> pageRequest);

  PageResponse<InfrastructureMapping> list(PageRequest<InfrastructureMapping> pageRequest, boolean disableValidation);

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
   * @param infraMappingId the infra mapping id
   */
  void delete(String appId, String infraMappingId);

  /**
   * Prune owned from the app objects.
   *
   * @param appId the app id
   * @param infraMappingId the infra mapping id
   */
  void pruneDescendingEntities(@NotEmpty String appId, @NotEmpty String infraMappingId);

  /**
   * Delete by service template.
   *
   * @param appId             the app id
   * @param serviceTemplateId the service template id
   */
  void deleteByServiceTemplate(String appId, String serviceTemplateId);

  /**
   * Gets infra mapping stencils.
   *
   * @param appId the app id
   * @return the infra mapping stencils
   */
  Map<String, Object> getInfraMappingStencils(String appId);

  /**
   * List compute provider hosts list.
   *
   * @param appId             the app id
   * @param envId             the env id
   * @param serviceId         the service id
   * @param computeProviderId the compute provider id
   * @return the list
   */
  List<String> listComputeProviderHostDisplayNames(
      String appId, String envId, String serviceId, String computeProviderId);

  InfrastructureMapping getInfraMappingByName(String appId, String envId, String name);

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
   * @param infraMappingId     the infra mapping id
   * @param workflowId
   * @return the list
   */
  List<Host> getAutoScaleGroupNodes(String appId, String infraMappingId, String workflowExecutionId);

  /**
   * List types map.
   *
   * @param appId     the app id
   * @param envId     the env id
   * @param serviceId the service id
   * @return the map
   */
  Map<DeploymentType, List<SettingVariableTypes>> listInfraTypes(String appId, String envId, String serviceId);

  /**
   * Select service instances list.
   *
   * @param appId                          the app id
   * @param infraMappingId                 the infra mapping id
   *@param selectionParams the service instance selection params  @return the list
   */
  List<ServiceInstance> selectServiceInstances(
      String appId, String infraMappingId, String workflowExecutionId, ServiceInstanceSelectionParams selectionParams);

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
   * @param computeProviderId the compute provider id
   * @return the map
   */
  Map<String, String> listAllRoles(String appId, String computeProviderId);

  /**
   * List networks list.
   *
   * @param appId             the app id
   * @param computeProviderId the compute provider id
   * @param region            the region
   * @return the list
   */
  List<String> listVPC(@NotEmpty String appId, @NotEmpty String computeProviderId, @NotEmpty String region);

  /**
   * List security groups list.
   *
   * @param appId             the app id
   * @param computeProviderId the compute provider id
   * @param region            the region
   * @param vpcIds            the vpc ids
   * @return the list
   */
  List<String> listSecurityGroups(@NotEmpty String appId, @NotEmpty String computeProviderId, @NotEmpty String region,
      @NotNull List<String> vpcIds);

  /**
   * List subnets list.
   *
   * @param appId             the app id
   * @param computeProviderId the compute provider id
   * @param region            the region
   * @param vpcIds            the vpc ids
   * @return the list
   */
  List<String> listSubnets(@NotEmpty String appId, @NotEmpty String computeProviderId, @NotEmpty String region,
      @NotNull List<String> vpcIds);

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

  /**
   * List code deploy application names list.
   *
   * @param computeProviderId the compute provider id
   * @param region            the region
   * @return the list
   */
  List<String> listCodeDeployApplicationNames(String computeProviderId, String region);

  /**
   * List code deploy deployment groups list.
   *
   * @param computeProviderId the compute provider id
   * @param region            the region
   * @param applicationName   the application name
   * @return the list
   */
  List<String> listCodeDeployDeploymentGroups(String computeProviderId, String region, String applicationName);

  /**
   * List code deploy deployment configs list.
   *
   * @param computeProviderId the compute provider id
   * @param region            the region
   * @return the list
   */
  List<String> listCodeDeployDeploymentConfigs(String computeProviderId, String region);

  /**
   * List load balancers map.
   *
   * @param appId          the app id
   * @param infraMappingId the infra mapping id
   * @return the map
   */
  Map<String, String> listLoadBalancers(String appId, String infraMappingId);

  /**
   * List target groups map.
   *
   * @param appId            the app id
   * @param infraMappingId   the infra mapping id
   * @param loadbalancerName the loadbalancer name
   * @return the map
   */
  Map<String, String> listTargetGroups(String appId, String infraMappingId, String loadbalancerName);

  /**
   * List hosts list.
   *
   * @param appId          the app id
   * @param infraMappingId the infra mapping id
   * @return the list
   */
  List<String> listHostDisplayNames(String appId, String infraMappingId, String workflowExecutionId);

  /**
   * List aws iam roles map.
   *
   * @param appId          the app id
   * @param infraMappingId the infra mapping id
   * @return the map
   */
  Map<String, String> listAwsIamRoles(String appId, String infraMappingId);

  /**
   * List tags list.
   *
   * @param appId             the app id
   * @param computeProviderId the compute provider id
   * @param region            the region
   * @return the list
   */
  Set<String> listTags(String appId, String computeProviderId, String region);

  /**
   * List auto scaling groups list.
   *
   * @param appId             the app id
   * @param computeProviderId the compute provider id
   * @param region            the region
   * @return the list
   */
  List<String> listAutoScalingGroups(String appId, String computeProviderId, String region);
}
