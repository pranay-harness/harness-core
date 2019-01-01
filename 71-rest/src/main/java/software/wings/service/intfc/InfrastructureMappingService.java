package software.wings.service.intfc;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.delegate.task.protocol.AwsElbListener;
import io.harness.exception.WingsException;
import io.harness.persistence.HQuery.QueryChecks;
import io.harness.validation.Create;
import io.harness.validation.Update;
import org.hibernate.validator.constraints.NotEmpty;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.api.DeploymentType;
import software.wings.beans.HostValidationRequest;
import software.wings.beans.HostValidationResponse;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.ServiceInstance;
import software.wings.beans.ServiceInstanceSelectionParams;
import software.wings.beans.infrastructure.Host;
import software.wings.service.intfc.ownership.OwnedByEnvironment;
import software.wings.service.intfc.ownership.OwnedByInfrastructureProvisioner;
import software.wings.settings.SettingValue.SettingVariableTypes;

import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/**
 * Created by anubhaw on 1/10/17.
 */
public interface InfrastructureMappingService extends OwnedByEnvironment, OwnedByInfrastructureProvisioner {
  PageResponse<InfrastructureMapping> list(PageRequest<InfrastructureMapping> pageRequest);

  PageResponse<InfrastructureMapping> list(
      PageRequest<InfrastructureMapping> pageRequest, Set<QueryChecks> queryChecks);

  @ValidationGroups(Create.class) InfrastructureMapping save(@Valid InfrastructureMapping infrastructureMapping);
  @ValidationGroups(Create.class)
  InfrastructureMapping save(@Valid InfrastructureMapping infrastructureMapping, boolean fromYaml);

  InfrastructureMapping get(String appId, String infraMappingId);

  @ValidationGroups(Update.class) InfrastructureMapping update(@Valid InfrastructureMapping infrastructureMapping);
  @ValidationGroups(Update.class)
  InfrastructureMapping update(@Valid InfrastructureMapping infrastructureMapping, boolean fromYaml);

  void ensureSafeToDelete(@NotEmpty String appId, @NotEmpty String infraMappingId);

  void delete(String appId, String infraMappingId);

  void pruneDescendingEntities(@NotEmpty String appId, @NotEmpty String infraMappingId);

  void deleteByServiceTemplate(String appId, String serviceTemplateId);

  Map<String, Object> getInfraMappingStencils(String appId);

  List<String> listComputeProviderHostDisplayNames(
      String appId, String envId, String serviceId, String computeProviderId);

  InfrastructureMapping getInfraMappingByName(String appId, String envId, String name);

  List<Host> getAutoScaleGroupNodes(String appId, String infraMappingId, String workflowExecutionId);

  Map<DeploymentType, List<SettingVariableTypes>> listInfraTypes(String appId, String envId, String serviceId);

  List<ServiceInstance> selectServiceInstances(
      String appId, String infraMappingId, String workflowExecutionId, ServiceInstanceSelectionParams selectionParams);

  List<String> listClusters(String appId, String deploymentType, String computeProviderId, String region);

  List<String> listRegions(String appId, String deploymentType, String computeProviderId);

  List<String> listInstanceTypes(String appId, String deploymentType, String computeProviderId);

  List<String> listInstanceRoles(String appId, String deploymentType, String computeProviderId);

  Map<String, String> listAllRoles(String appId, String computeProviderId);

  List<String> listVPC(@NotEmpty String appId, @NotEmpty String computeProviderId, @NotEmpty String region);

  List<String> listSecurityGroups(@NotEmpty String appId, @NotEmpty String computeProviderId, @NotEmpty String region,
      @NotNull List<String> vpcIds);

  List<String> listSubnets(@NotEmpty String appId, @NotEmpty String computeProviderId, @NotEmpty String region,
      @NotNull List<String> vpcIds);

  Map<String, String> listLoadBalancers(String appId, String deploymentType, String computeProviderId);

  List<String> listClassicLoadBalancers(String appId, String computeProviderId, String region);

  Map<String, String> listTargetGroups(
      String appId, String deploymentType, String computeProviderId, String loadBalancerName);

  List<HostValidationResponse> validateHost(@Valid HostValidationRequest validationRequest);

  List<String> listElasticLoadBalancer(
      @NotNull String accessKey, @NotNull char[] secretKey, @NotNull String region, @NotEmpty String accountId);

  Map<String, String> listNetworkLoadBalancers(String appId, String infraMappingId);

  List<String> listCodeDeployApplicationNames(String computeProviderId, String region, String appId);

  List<String> listCodeDeployDeploymentGroups(
      String computeProviderId, String region, String applicationName, String appId);

  List<String> listCodeDeployDeploymentConfigs(String computeProviderId, String region, String appId);

  Map<String, String> listLoadBalancers(String appId, String infraMappingId);

  Map<String, String> listElasticLoadBalancers(String appId, String infraMappingId);

  Map<String, String> listTargetGroups(String appId, String infraMappingId, String loadbalancerName);

  List<String> listHostDisplayNames(String appId, String infraMappingId, String workflowExecutionId);

  String getContainerRunningInstances(String appId, String infraMappingId, String serviceNameExpression);

  Map<String, String> listAwsIamRoles(String appId, String infraMappingId);

  Set<String> listTags(String appId, String computeProviderId, String region);
  Set<String> listAzureTags(String appId, String computeProviderId, String subscriptionId);
  Set<String> listAzureResourceGroups(String appId, String computeProviderId, String subscriptionId);

  List<String> listAutoScalingGroups(String appId, String computeProviderId, String region);

  Map<String, String> listAlbTargetGroups(String appId, String computeProviderId, String region);

  List<Host> listHosts(String appId, String infrastructureMappingId);

  List<InfrastructureMapping> getInfraStructureMappingsByUuids(String appId, List<String> infraMappingIds);
  List<String> listOrganizationsForPcf(String appId, String computeProviderId) throws WingsException;

  List<String> listSpacesForPcf(String appId, String computeProviderId, String organization) throws WingsException;

  List<String> lisRouteMapsForPcf(String appId, String computeProviderId, String organization, String spaces)
      throws WingsException;

  void deleteByYamlGit(String appId, String infraMappingId, boolean syncFromGit);

  List<InfrastructureMapping> listByComputeProviderId(String accountId, String computeProviderId);

  List<AwsElbListener> listListeners(String appId, String infraMappingId, String loadbalancerName);
}
