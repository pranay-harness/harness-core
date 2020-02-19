package software.wings.service.intfc;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.delegate.task.aws.AwsElbListener;
import io.harness.delegate.task.aws.AwsLoadBalancerDetails;
import io.harness.delegate.task.spotinst.response.SpotinstElastigroupRunningCountData;
import io.harness.spotinst.model.ElastiGroup;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.api.DeploymentType;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.infrastructure.Host;
import software.wings.infra.InfraDefinitionDetail;
import software.wings.infra.InfrastructureDefinition;
import software.wings.service.impl.aws.model.AwsAsgGetRunningCountData;
import software.wings.service.impl.aws.model.AwsRoute53HostedZoneData;
import software.wings.service.impl.aws.model.AwsSecurityGroup;
import software.wings.service.impl.aws.model.AwsSubnet;
import software.wings.service.impl.aws.model.AwsVPC;
import software.wings.service.intfc.ownership.OwnedByEnvironment;
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.sm.ExecutionContext;

import java.util.List;
import java.util.Map;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

public interface InfrastructureDefinitionService extends OwnedByEnvironment {
  PageResponse<InfrastructureDefinition> list(PageRequest<InfrastructureDefinition> pageRequest);
  PageResponse<InfrastructureDefinition> list(@NotEmpty String appId, @NotEmpty String envId, String serviceId);
  PageResponse<InfraDefinitionDetail> listInfraDefinitionDetail(
      PageRequest<InfrastructureDefinition> pageRequest, @NotEmpty String appId, @NotEmpty String envId);
  InfrastructureDefinition save(@Valid InfrastructureDefinition infrastructureDefinition, boolean migration);
  InfrastructureDefinition save(
      @Valid InfrastructureDefinition infrastructureDefinition, boolean migration, boolean skipValidation);
  InfrastructureDefinition get(String appId, String infraDefinitionId);
  InfrastructureDefinition update(@Valid InfrastructureDefinition infrastructureDefinition);
  void delete(String appId, String infraDefinitionId);
  void deleteByYamlGit(String appid, String infraDefinitionId);

  Map<DeploymentType, List<SettingVariableTypes>> getDeploymentTypeCloudProviderOptions();

  InfrastructureMapping getInfraMapping(
      String appId, String serviceId, String infraDefinitionId, ExecutionContext context);

  InfrastructureMapping getInfrastructureMapping(String serviceId, InfrastructureDefinition infrastructureDefinition);

  boolean isDynamicInfrastructure(String appId, String infraDefinitionId);

  List<String> fetchCloudProviderIds(String appId, List<String> infraDefinitionIds);

  InfrastructureDefinition getInfraDefByName(String appId, String envId, String infraDefName);

  List<InfrastructureDefinition> getInfraStructureDefinitionByUuids(String appId, List<String> infraDefinitionIds);

  String cloudProviderNameForDefinition(InfrastructureDefinition infrastructureDefinition);

  String cloudProviderNameForDefinition(String appId, String infraDefinitionId);

  InfraDefinitionDetail getDetail(String appId, String infraDefinitionId);

  List<Host> getAutoScaleGroupNodes(String appId, String infraDefinitionId, String workflowExecutionId);

  List<String> listHostDisplayNames(String appId, String infraDefinition, String workflowExecutionId);

  Map<String, String> listAwsIamRoles(String appId, String infraDefinitionId);

  List<Host> listHosts(String appId, String infraDefinitionId);

  List<AwsElbListener> listListeners(String appId, String infraDefinitionId, String loadbalancerName);

  List<AwsRoute53HostedZoneData> listHostedZones(String appId, String infraDefinitionId);

  Map<String, String> listNetworkLoadBalancers(String appId, String infraDefinitionId);

  Map<String, String> listLoadBalancers(String appId, String infraDefinitionId);

  Map<String, String> listElasticLoadBalancers(String appId, String infraDefinitionId);

  Map<String, String> listTargetGroups(String appId, String infraDefinitionId, String loadbalancerName);

  String getContainerRunningInstances(
      String appId, String infraDefinitionId, String serviceId, String serviceNameExpression);

  void pruneDescendingEntities(@NotEmpty String appId, @NotEmpty String infraDefinitionId);

  void ensureSafeToDelete(@NotEmpty String appId, InfrastructureDefinition infrastructureDefinition);

  List<String> listNamesByProvisionerId(@NotEmpty String appId, @NotEmpty String provisionerId);

  List<String> listNamesByComputeProviderId(@NotEmpty String accountId, @NotEmpty String computeProviderId);

  List<String> listNamesByConnectionAttr(@NotEmpty String accountId, @NotEmpty String attributeId);

  List<String> listNamesByScopedService(String appId, String serviceName);

  void cloneInfrastructureDefinitions(String sourceAppID, String sourceEnvID, String targetAppID, String targetEnvID);

  Integer getPcfRunningInstances(String appId, String infraMappingId, String appNameExpression, String serviceId);

  List<InfrastructureDefinition> getNameAndIdForEnvironments(String appId, List<String> envIds);

  List<InfrastructureDefinition> getNameAndIdForEnvironment(String appId, String envId, int limit);
  Map<String, Integer> getCountForEnvironments(String appId, @NotNull List<String> envIds);

  List<InfrastructureDefinition> getDefinitionWithFieldsForEnvironments(
      String appId, List<String> envIds, List<String> projections);

  List<AwsLoadBalancerDetails> listElasticLoadBalancerDetails(String appId, String infraDefinitionId);
  List<AwsLoadBalancerDetails> listNetworkLoadBalancerDetails(String appId, String infraDefinitionId);

  List<ElastiGroup> listElastiGroups(String appId, String computeProviderId);
  String getElastigroupJson(String appId, String computeProviderId, String elastigroupId);
  SpotinstElastigroupRunningCountData getElastigroupRunningCountData(
      String appId, String infraDefinitionId, String elastigroupNameExpression, String serviceId, boolean blueGreen);

  AwsAsgGetRunningCountData getAmiCurrentlyRunningInstanceCount(
      String appId, String infraDefinitionId, String serviceId);

  List<String> listRoutesForPcf(String appId, String infraDefinitionId);

  List<AwsVPC> listVPC(String appId, String computeProviderId, String region);

  List<AwsSecurityGroup> listSecurityGroups(String appId, String computeProviderId, String region, List<String> vpcIds);

  List<AwsSubnet> listSubnets(String appId, String computeProviderId, String region, List<String> vpcIds);
}
