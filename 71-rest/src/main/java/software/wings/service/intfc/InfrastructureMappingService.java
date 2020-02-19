package software.wings.service.intfc;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.delegate.task.aws.AwsElbListener;
import io.harness.persistence.HQuery.QueryChecks;
import io.harness.validation.Create;
import io.harness.validation.Update;
import org.hibernate.validator.constraints.NotEmpty;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.api.DeploymentType;
import software.wings.api.PhaseElement;
import software.wings.beans.HostValidationRequest;
import software.wings.beans.HostValidationResponse;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.ServiceInstance;
import software.wings.beans.ServiceInstanceSelectionParams;
import software.wings.beans.infrastructure.Host;
import software.wings.service.impl.aws.model.AwsAsgGetRunningCountData;
import software.wings.service.impl.aws.model.AwsRoute53HostedZoneData;
import software.wings.service.impl.aws.model.AwsSecurityGroup;
import software.wings.service.impl.aws.model.AwsSubnet;
import software.wings.service.impl.aws.model.AwsVPC;
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

  void validateInfraMapping(@Valid InfrastructureMapping infraMapping, boolean skipValidation);

  @ValidationGroups(Create.class) InfrastructureMapping save(@Valid InfrastructureMapping infrastructureMapping);

  @ValidationGroups(Create.class)
  InfrastructureMapping save(@Valid InfrastructureMapping infrastructureMapping, boolean skipValidation);

  InfrastructureMapping get(String appId, String infraMappingId);

  @ValidationGroups(Update.class) InfrastructureMapping update(@Valid InfrastructureMapping infrastructureMapping);

  @ValidationGroups(Update.class)
  InfrastructureMapping update(@Valid InfrastructureMapping infrastructureMapping, boolean skipValidation);

  void ensureSafeToDelete(@NotEmpty String appId, @NotEmpty String infraMappingId);

  void delete(String appId, String infraMappingId);

  void pruneDescendingEntities(@NotEmpty String appId, @NotEmpty String infraMappingId);

  void deleteByServiceTemplate(String appId, String serviceTemplateId);

  Map<String, Object> getInfraMappingStencils(String appId);

  List<String> listComputeProviderHostDisplayNames(
      String appId, String envId, String serviceId, String computeProviderId);

  InfrastructureMapping getInfraMappingByName(String appId, String envId, String name);

  List<InfrastructureMapping> getInfraMappingLinkedToInfraDefinition(String appId, String infraDefinitionId);

  // TODO => Remove this comment once fully migrated
  // Remove all NeedsMigration Conmmecnt after migration

  // NeedsMigration
  // Migrated
  List<Host> getAutoScaleGroupNodes(String appId, String infraMappingId, String workflowExecutionId);

  Map<DeploymentType, List<SettingVariableTypes>> listInfraTypes(String appId, String envId, String serviceId);

  // TODO check What the Function id Doing. DOes it require to be migrated
  List<ServiceInstance> selectServiceInstances(
      String appId, String infraMappingId, String workflowExecutionId, ServiceInstanceSelectionParams selectionParams);

  List<String> listClusters(String appId, String deploymentType, String computeProviderId, String region);

  List<String> listRegions(String appId, String deploymentType, String computeProviderId);

  List<String> listInstanceTypes(String appId, String deploymentType, String computeProviderId);

  List<String> listInstanceRoles(String appId, String deploymentType, String computeProviderId);

  Map<String, String> listAllRoles(String appId, String computeProviderId);

  List<AwsVPC> listVPC(@NotEmpty String appId, @NotEmpty String computeProviderId, @NotEmpty String region);

  List<AwsSecurityGroup> listSecurityGroups(@NotEmpty String appId, @NotEmpty String computeProviderId,
      @NotEmpty String region, @NotNull List<String> vpcIds);

  List<AwsSubnet> listSubnets(@NotEmpty String appId, @NotEmpty String computeProviderId, @NotEmpty String region,
      @NotNull List<String> vpcIds);

  Map<String, String> listLoadBalancers(String appId, String deploymentType, String computeProviderId);

  List<String> listClassicLoadBalancers(String appId, String computeProviderId, String region);

  Map<String, String> listTargetGroups(
      String appId, String deploymentType, String computeProviderId, String loadBalancerName);

  List<HostValidationResponse> validateHost(@Valid HostValidationRequest validationRequest);

  List<String> listElasticLoadBalancer(
      @NotNull String accessKey, @NotNull char[] secretKey, @NotNull String region, @NotEmpty String accountId);

  // NeedsMigration
  // Migrated
  Map<String, String> listNetworkLoadBalancers(String appId, String infraMappingId);

  List<String> listCodeDeployApplicationNames(String computeProviderId, String region, String appId);

  List<String> listCodeDeployDeploymentGroups(
      String computeProviderId, String region, String applicationName, String appId);

  List<String> listCodeDeployDeploymentConfigs(String computeProviderId, String region, String appId);

  // NeedsMigration
  // Migrated
  Map<String, String> listLoadBalancers(String appId, String infraMappingId);

  // NeedsMigration
  // Migrated
  Map<String, String> listElasticLoadBalancers(String appId, String infraMappingId);

  // NeedsMigration
  // Migrated
  Map<String, String> listTargetGroups(String appId, String infraMappingId, String loadbalancerName);

  // NeedsMigration
  // Migrated
  List<String> listHostDisplayNames(String appId, String infraMappingId, String workflowExecutionId);

  // Needs Migration
  // Migrated
  String getContainerRunningInstances(String appId, String infraMappingId, String serviceNameExpression);

  // NeedsMigration
  // Migrated
  Map<String, String> listAwsIamRoles(String appId, String infraMappingId);

  Set<String> listTags(String appId, String computeProviderId, String region);

  Set<String> listAzureTags(String appId, String computeProviderId, String subscriptionId);

  Set<String> listAzureResourceGroups(String appId, String computeProviderId, String subscriptionId);

  List<String> listAutoScalingGroups(String appId, String computeProviderId, String region);

  Map<String, String> listAlbTargetGroups(String appId, String computeProviderId, String region);

  // NeedsMigration
  List<Host> listHosts(String appId, String infrastructureMappingId);

  List<InfrastructureMapping> getInfraStructureMappingsByUuids(String appId, List<String> infraMappingIds);

  List<String> listOrganizationsForPcf(String appId, String computeProviderId);

  List<String> listSpacesForPcf(String appId, String computeProviderId, String organization);

  List<String> lisRouteMapsForPcf(String appId, String computeProviderId, String organization, String spaces);

  void deleteByYamlGit(String appId, String infraMappingId, boolean syncFromGit);

  List<InfrastructureMapping> listByComputeProviderId(String accountId, String computeProviderId);

  // NeedsMigration
  // Migrated
  List<AwsElbListener> listListeners(String appId, String infraMappingId, String loadbalancerName);

  String createRoute(String appId, String computeProviderId, String organization, String spaces, String host,
      String domain, String path, boolean tcpRoute, boolean useRandomPort, String port);

  // NeedsMigration
  // Migrated
  List<AwsRoute53HostedZoneData> listHostedZones(String appId, String infraMappingId);

  Integer getPcfRunningInstances(String appId, String infraMappingId, String appNameExpression);

  AwsAsgGetRunningCountData getAmiCurrentlyRunningInstanceCount(String infraMappingId, String appId);

  List<String> fetchCloudProviderIds(String appId, List<String> infraMappingIds);

  List<InfrastructureMapping> listInfraMappings(@NotEmpty String appId, @NotEmpty String envId);

  void saveInfrastructureMappingToSweepingOutput(
      String appId, String workflowExecutionId, PhaseElement phaseElement, String infraStructureMappingId);

  List<String> getVPCIdStrList(String appId, String computeProviderId, String region);

  List<String> getSGIdStrList(String appId, String computeProviderId, String region, List<String> vpcIds);

  List<String> getSubnetIdStrList(String appId, String computeProviderId, String region, List<String> vpcIds);
}
