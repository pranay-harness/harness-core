package migrations.all;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.lang.String.format;

import com.google.common.collect.Maps;
import com.google.inject.Inject;

import io.harness.exception.ExceptionUtils;
import io.harness.persistence.HIterator;
import lombok.extern.slf4j.Slf4j;
import migrations.Migration;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import software.wings.api.CloudProviderType;
import software.wings.api.DeploymentType;
import software.wings.beans.Account;
import software.wings.beans.AwsAmiInfrastructureMapping;
import software.wings.beans.AwsInfrastructureMapping;
import software.wings.beans.AwsInstanceFilter.AwsInstanceFilterKeys;
import software.wings.beans.AwsLambdaInfraStructureMapping;
import software.wings.beans.AzureInfrastructureMapping;
import software.wings.beans.AzureKubernetesInfrastructureMapping;
import software.wings.beans.BlueprintProperty;
import software.wings.beans.CodeDeployInfrastructureMapping;
import software.wings.beans.DirectKubernetesInfrastructureMapping;
import software.wings.beans.EcsInfrastructureMapping;
import software.wings.beans.GcpKubernetesInfrastructureMapping;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureMapping.InfrastructureMappingKeys;
import software.wings.beans.InfrastructureMappingBlueprint;
import software.wings.beans.InfrastructureProvisioner;
import software.wings.beans.InfrastructureProvisioner.InfrastructureProvisionerKeys;
import software.wings.beans.NameValuePair;
import software.wings.beans.PcfInfrastructureMapping;
import software.wings.beans.PhysicalInfrastructureMapping;
import software.wings.beans.PhysicalInfrastructureMappingWinRm;
import software.wings.dl.WingsPersistence;
import software.wings.infra.AwsAmiInfrastructure;
import software.wings.infra.AwsAmiInfrastructure.AwsAmiInfrastructureKeys;
import software.wings.infra.AwsEcsInfrastructure;
import software.wings.infra.AwsEcsInfrastructure.AwsEcsInfrastructureKeys;
import software.wings.infra.AwsInstanceInfrastructure;
import software.wings.infra.AwsInstanceInfrastructure.AwsInstanceInfrastructureKeys;
import software.wings.infra.AwsLambdaInfrastructure;
import software.wings.infra.AwsLambdaInfrastructure.AwsLambdaInfrastructureKeys;
import software.wings.infra.AzureInstanceInfrastructure;
import software.wings.infra.AzureKubernetesService;
import software.wings.infra.CodeDeployInfrastructure;
import software.wings.infra.DirectKubernetesInfrastructure;
import software.wings.infra.GoogleKubernetesEngine;
import software.wings.infra.InfraMappingInfrastructureProvider;
import software.wings.infra.InfrastructureDefinition;
import software.wings.infra.InfrastructureDefinition.InfrastructureDefinitionBuilder;
import software.wings.infra.PcfInfraStructure;
import software.wings.infra.PhysicalInfra;
import software.wings.infra.PhysicalInfraWinrm;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.InfrastructureDefinitionService;
import software.wings.service.intfc.InfrastructureMappingService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
public class InfraMappingToDefinitionMigration implements Migration {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private AccountService accountService;
  @Inject private AppService appService;
  @Inject private EnvironmentService environmentService;
  @Inject private InfrastructureDefinitionService infrastructureDefinitionService;
  @Inject private InfrastructureMappingService infrastructureMappingService;
  @Inject private SetInfraDefinitionTriggers setInfraDefinitionTriggers;
  @Inject private SetInfraDefinitionPipelines setInfraDefinitionPipelines;
  @Inject private SetInfraDefinitionWorkflows setInfraDefinitionWorkflows;

  private final String DEBUG_LINE = " INFRA_MAPPING_MIGRATION: ";
  private final String accountId = "zEaak-FLS425IEO7OLzMUg";

  public void migrate() {
    logger.info(StringUtils.join(DEBUG_LINE, "Starting Infra Definition migration for accountId ", accountId));
    Account account = accountService.get(accountId);
    if (account == null) {
      logger.info(StringUtils.join(DEBUG_LINE, "Account does not exist, accountId ", accountId));
      return;
    }

    List<String> appIds = appService.getAppIdsByAccountId(accountId);

    Map<String, InfrastructureProvisioner> infrastructureProvisionerMap = new HashMap<>();

    for (String appId : appIds) {
      logger.info(StringUtils.join(DEBUG_LINE, "Starting migration for appId ", appId));

      List<String> envIds = environmentService.getEnvIdsByApp(appId);

      infrastructureProvisionerMap.clear();
      try (HIterator<InfrastructureProvisioner> infrastructureProvisionerHIterator =
               new HIterator<>(wingsPersistence.createQuery(InfrastructureProvisioner.class)
                                   .field(InfrastructureProvisionerKeys.appId)
                                   .equal(appId)
                                   .fetch())) {
        for (InfrastructureProvisioner provisioner : infrastructureProvisionerHIterator) {
          infrastructureProvisionerMap.put(provisioner.getUuid(), provisioner);
        }
      }
      for (String envId : envIds) {
        logger.info(StringUtils.join(DEBUG_LINE, "Starting migration for envId ", envId));

        try (HIterator<InfrastructureMapping> infrastructureMappingHIterator =
                 new HIterator<>(wingsPersistence.createQuery(InfrastructureMapping.class)
                                     .field(InfrastructureMappingKeys.appId)
                                     .equal(appId)
                                     .field(InfrastructureMappingKeys.envId)
                                     .equal(envId)
                                     .fetch())) {
          for (InfrastructureMapping infrastructureMapping : infrastructureMappingHIterator) {
            logger.info(StringUtils.join(
                DEBUG_LINE, "Starting migration for inframappingId ", infrastructureMapping.getUuid()));

            // If infradefinitionId is already set, then no need to migrate
            if (isEmpty(infrastructureMapping.getInfrastructureDefinitionId())) {
              Optional<InfrastructureDefinition> newInfraDefinition = createInfraDefinition(
                  infrastructureMapping, infrastructureProvisionerMap.get(infrastructureMapping.getProvisionerId()));

              newInfraDefinition.ifPresent(def -> {
                try {
                  InfrastructureDefinition savedDefinition = infrastructureDefinitionService.save(def, true);
                  setInfraDefinitionId(savedDefinition.getUuid(), infrastructureMapping);
                  logger.info(StringUtils.join(DEBUG_LINE,
                      format("Migrated infra mapping %s to infra definition %s", infrastructureMapping.getUuid(),
                          savedDefinition.getUuid())));
                } catch (Exception ex) {
                  logger.error(StringUtils.join(
                      DEBUG_LINE, ExceptionUtils.getMessage(ex), " inframapping ", infrastructureMapping.getUuid()));
                }
              });
            } else {
              logger.info(StringUtils.join(DEBUG_LINE, "skipping infra mapping ", infrastructureMapping.getUuid(),
                  " since infra definition is set to ", infrastructureMapping.getInfrastructureDefinitionId()));
            }
          }
        } catch (IllegalStateException ex) {
          logger.error(StringUtils.join(DEBUG_LINE,
              format(" Infra Mapping in env %s has more than 1 provisioners referenced ", envId), ex.getMessage()));
        } catch (Exception ex) {
          logger.error(
              StringUtils.join(DEBUG_LINE, format("Error migrating env %s of app %s", envId, appId), ex.getMessage()));
        }

        logger.info(StringUtils.join(DEBUG_LINE, "Finished migration for envId ", envId));
      }

      logger.info(StringUtils.join(DEBUG_LINE, "Finished migration for appId ", appId));
    }
    setInfraDefinitionTriggers.migrate(account);
    setInfraDefinitionPipelines.migrate(account);
    setInfraDefinitionWorkflows.migrate(account);

    logger.info(StringUtils.join(DEBUG_LINE, "Finished Infra mapping migration for accountId ", accountId));
  }

  private Optional<InfrastructureDefinition> createInfraDefinition(
      InfrastructureMapping src, InfrastructureProvisioner prov) {
    try {
      InfrastructureDefinitionBuilder definitionBuilder =
          InfrastructureDefinition.builder()
              .createdAt(src.getCreatedAt())
              .createdBy(src.getCreatedBy())
              .lastUpdatedBy(src.getLastUpdatedBy())
              .lastUpdatedAt(src.getLastUpdatedAt())
              .name(src.getName())
              .appId(src.getAppId())
              .envId(src.getEnvId())
              .deploymentType(DeploymentType.valueOf(src.getDeploymentType()))
              .cloudProviderType(CloudProviderType.valueOf(src.getComputeProviderType()))
              .provisionerId(src.getProvisionerId());

      if (isNotEmpty(src.getServiceId())) {
        definitionBuilder.scopedToServices(Arrays.asList(src.getServiceId()));
      } else {
        logger.error(
            StringUtils.join(DEBUG_LINE, format("No service linked to Inframapping %s, continuing..", src.getUuid())));
      }

      List<InfrastructureMappingBlueprint> infrastructureMappingBlueprints =
          (prov == null) ? Collections.EMPTY_LIST : prov.getMappingBlueprints();

      if (prov != null) {
        logger.info(StringUtils.join(DEBUG_LINE,
            format("found %s mapping blueprints for infra provisioner %s", infrastructureMappingBlueprints.size(),
                prov.getUuid())));
      }

      Map<String, String> fieldNameChanges = Maps.newHashMap();
      InfraMappingInfrastructureProvider infrastructure;
      if (src instanceof AwsInfrastructureMapping) {
        AwsInfrastructureMapping awsSrc = (AwsInfrastructureMapping) src;

        fieldNameChanges.clear();
        fieldNameChanges.put("vpcs", AwsInstanceFilterKeys.vpcIds);
        fieldNameChanges.put("autoScalingGroup", AwsInstanceInfrastructureKeys.autoScalingGroupName);

        infrastructure =
            AwsInstanceInfrastructure.builder()
                .cloudProviderId(awsSrc.getComputeProviderSettingId())
                .useAutoScalingGroup(isNotEmpty(awsSrc.getAutoScalingGroupName()))
                .region(awsSrc.getRegion())
                .hostConnectionAttrs(awsSrc.getHostConnectionAttrs())
                .loadBalancerId(awsSrc.getLoadBalancerId())
                .loadBalancerName(awsSrc.getLoadBalancerName())
                .usePublicDns(awsSrc.isUsePublicDns())
                .awsInstanceFilter(awsSrc.getAwsInstanceFilter())
                .autoScalingGroupName(awsSrc.getAutoScalingGroupName())
                .desiredCapacity(awsSrc.getDesiredCapacity())
                .setDesiredCapacity(awsSrc.isSetDesiredCapacity())
                .hostNameConvention(awsSrc.getHostNameConvention())
                .provisionInstances(awsSrc.isProvisionInstances())
                .expressions(getExpressionsFromBluePrints(src, infrastructureMappingBlueprints, fieldNameChanges))
                .build();
      } else if (src instanceof AwsAmiInfrastructureMapping) {
        AwsAmiInfrastructureMapping awsAmiSrc = (AwsAmiInfrastructureMapping) src;

        fieldNameChanges.clear();
        fieldNameChanges.put("baseAsg", AwsAmiInfrastructureKeys.autoScalingGroupName);
        fieldNameChanges.put("classicLbs", AwsAmiInfrastructureKeys.classicLoadBalancers);
        fieldNameChanges.put("targetGroups", AwsAmiInfrastructureKeys.targetGroupArns);
        fieldNameChanges.put("stageClassicLbs", AwsAmiInfrastructureKeys.stageClassicLoadBalancers);
        fieldNameChanges.put("stageTargetGroups", AwsAmiInfrastructureKeys.stageTargetGroupArns);

        infrastructure =
            AwsAmiInfrastructure.builder()
                .cloudProviderId(awsAmiSrc.getComputeProviderSettingId())
                .region(awsAmiSrc.getRegion())
                .autoScalingGroupName(awsAmiSrc.getAutoScalingGroupName())
                .classicLoadBalancers(awsAmiSrc.getClassicLoadBalancers())
                .targetGroupArns(awsAmiSrc.getTargetGroupArns())
                .hostNameConvention(awsAmiSrc.getHostNameConvention())
                .stageClassicLoadBalancers(awsAmiSrc.getStageClassicLoadBalancers())
                .stageTargetGroupArns(awsAmiSrc.getStageTargetGroupArns())
                .expressions(getExpressionsFromBluePrints(src, infrastructureMappingBlueprints, fieldNameChanges))
                .build();
      } else if (src instanceof AwsLambdaInfraStructureMapping) {
        AwsLambdaInfraStructureMapping awsLambdaSrc = (AwsLambdaInfraStructureMapping) src;

        fieldNameChanges.clear();
        fieldNameChanges.put("securityGroups", AwsLambdaInfrastructureKeys.securityGroupIds);

        infrastructure =
            AwsLambdaInfrastructure.builder()
                .cloudProviderId(awsLambdaSrc.getComputeProviderSettingId())
                .region(awsLambdaSrc.getRegion())
                .vpcId(awsLambdaSrc.getVpcId())
                .subnetIds(awsLambdaSrc.getSubnetIds())
                .securityGroupIds(awsLambdaSrc.getSecurityGroupIds())
                .role(awsLambdaSrc.getRole())
                .expressions(getExpressionsFromBluePrints(src, infrastructureMappingBlueprints, fieldNameChanges))
                .build();
      } else if (src instanceof CodeDeployInfrastructureMapping) {
        CodeDeployInfrastructureMapping codeDeploySrc = (CodeDeployInfrastructureMapping) src;

        fieldNameChanges.clear();

        infrastructure = CodeDeployInfrastructure.builder()
                             .cloudProviderId(codeDeploySrc.getComputeProviderSettingId())
                             .region(codeDeploySrc.getRegion())
                             .applicationName(codeDeploySrc.getApplicationName())
                             .deploymentConfig(codeDeploySrc.getDeploymentConfig())
                             .deploymentGroup(codeDeploySrc.getDeploymentGroup())
                             .hostNameConvention(codeDeploySrc.getHostNameConvention())
                             .build();
      } else if (src instanceof EcsInfrastructureMapping) {
        EcsInfrastructureMapping ecsSrc = (EcsInfrastructureMapping) src;

        fieldNameChanges.clear();
        fieldNameChanges.put("ecsCluster", AwsEcsInfrastructureKeys.clusterName);
        fieldNameChanges.put("ecsVpc", AwsEcsInfrastructureKeys.vpcId);
        fieldNameChanges.put("ecsSubnets", AwsEcsInfrastructureKeys.subnetIds);
        fieldNameChanges.put("ecsSgs", AwsEcsInfrastructureKeys.securityGroupIds);

        infrastructure =
            AwsEcsInfrastructure.builder()
                .cloudProviderId(ecsSrc.getComputeProviderSettingId())
                .region(ecsSrc.getRegion())
                .vpcId(ecsSrc.getVpcId())
                .subnetIds(ecsSrc.getSubnetIds())
                .securityGroupIds(ecsSrc.getSecurityGroupIds())
                .assignPublicIp(ecsSrc.isAssignPublicIp())
                .executionRole(ecsSrc.getExecutionRole())
                .launchType(ecsSrc.getLaunchType())
                .clusterName(ecsSrc.getClusterName())
                .expressions(getExpressionsFromBluePrints(src, infrastructureMappingBlueprints, fieldNameChanges))
                .build();
      } else if (src instanceof GcpKubernetesInfrastructureMapping) {
        GcpKubernetesInfrastructureMapping gcpSrc = (GcpKubernetesInfrastructureMapping) src;

        fieldNameChanges.clear();

        infrastructure =
            GoogleKubernetesEngine.builder()
                .cloudProviderId(gcpSrc.getComputeProviderSettingId())
                .clusterName(gcpSrc.getClusterName())
                .namespace(gcpSrc.getNamespace())
                .releaseName(gcpSrc.getReleaseName())
                .expressions(getExpressionsFromBluePrints(src, infrastructureMappingBlueprints, fieldNameChanges))
                .build();
      } else if (src instanceof DirectKubernetesInfrastructureMapping) {
        DirectKubernetesInfrastructureMapping directK8sSrc = (DirectKubernetesInfrastructureMapping) src;

        fieldNameChanges.clear();

        infrastructure = DirectKubernetesInfrastructure.builder()
                             .cloudProviderId(directK8sSrc.getComputeProviderSettingId())
                             .clusterName(directK8sSrc.getClusterName())
                             .namespace(directK8sSrc.getNamespace())
                             .releaseName(directK8sSrc.getReleaseName())
                             .build();
      } else if (src instanceof AzureInfrastructureMapping) {
        AzureInfrastructureMapping azureSrc = (AzureInfrastructureMapping) src;

        fieldNameChanges.clear();

        infrastructure = AzureInstanceInfrastructure.builder()
                             .cloudProviderId(azureSrc.getComputeProviderSettingId())
                             .subscriptionId(azureSrc.getSubscriptionId())
                             .resourceGroup(azureSrc.getResourceGroup())
                             .tags(azureSrc.getTags())
                             .hostConnectionAttrs(azureSrc.getHostConnectionAttrs())
                             .winRmConnectionAttributes(azureSrc.getWinRmConnectionAttributes())
                             .usePublicDns(azureSrc.isUsePublicDns())
                             .build();
      } else if (src instanceof AzureKubernetesInfrastructureMapping) {
        AzureKubernetesInfrastructureMapping azureK8sSrc = (AzureKubernetesInfrastructureMapping) src;

        fieldNameChanges.clear();

        infrastructure = AzureKubernetesService.builder()
                             .cloudProviderId(azureK8sSrc.getComputeProviderSettingId())
                             .clusterName(azureK8sSrc.getClusterName())
                             .namespace(azureK8sSrc.getNamespace())
                             .subscriptionId(azureK8sSrc.getSubscriptionId())
                             .resourceGroup(azureK8sSrc.getResourceGroup())
                             .build();
      } else if (src instanceof PcfInfrastructureMapping) {
        PcfInfrastructureMapping pcfSrc = (PcfInfrastructureMapping) src;

        fieldNameChanges.clear();

        infrastructure = PcfInfraStructure.builder()
                             .cloudProviderId(pcfSrc.getComputeProviderSettingId())
                             .organization(pcfSrc.getOrganization())
                             .space(pcfSrc.getSpace())
                             .tempRouteMap(pcfSrc.getTempRouteMap())
                             .routeMaps(pcfSrc.getRouteMaps())
                             .build();
      } else if (src instanceof PhysicalInfrastructureMapping) {
        PhysicalInfrastructureMapping physicalSrc = (PhysicalInfrastructureMapping) src;

        fieldNameChanges.clear();
        fieldNameChanges.put("Hostname", PhysicalInfra.hostname);

        infrastructure =
            PhysicalInfra.builder()
                .cloudProviderId(physicalSrc.getComputeProviderSettingId())
                .hostNames(physicalSrc.getHostNames())
                .hosts(physicalSrc.hosts())
                .loadBalancerId(physicalSrc.getLoadBalancerId())
                .loadBalancerName(physicalSrc.getLoadBalancerName())
                .hostConnectionAttrs(physicalSrc.getHostConnectionAttrs())
                .expressions(getExpressionsFromBluePrints(src, infrastructureMappingBlueprints, fieldNameChanges))
                .build();
      } else if (src instanceof PhysicalInfrastructureMappingWinRm) {
        PhysicalInfrastructureMappingWinRm physicalWinRmSrc = (PhysicalInfrastructureMappingWinRm) src;

        fieldNameChanges.clear();

        infrastructure = PhysicalInfraWinrm.builder()
                             .cloudProviderId(physicalWinRmSrc.getComputeProviderSettingId())
                             .hostNames(physicalWinRmSrc.getHostNames())
                             .hosts(physicalWinRmSrc.hosts())
                             .loadBalancerId(physicalWinRmSrc.getLoadBalancerId())
                             .loadBalancerName(physicalWinRmSrc.getLoadBalancerName())
                             .winRmConnectionAttributes(physicalWinRmSrc.getWinRmConnectionAttributes())
                             .build();
      } else {
        infrastructure = null;
        logger.error(StringUtils.join(DEBUG_LINE, " Unknown type for infra mapping %s ", src.getUuid()));
      }
      return Optional.of(definitionBuilder.infrastructure(infrastructure).build());
    } catch (Exception ex) {
      logger.error(StringUtils.join(DEBUG_LINE, ExceptionUtils.getMessage(ex),
          " Could not create infradefinition for inframapping ", src.getUuid()));
      return Optional.empty();
    }
  }

  private Map<String, String> getExpressionsFromBluePrints(InfrastructureMapping infrastructureMapping,
      List<InfrastructureMappingBlueprint> blueprints, Map<String, String> fieldNameChanges) {
    if (CollectionUtils.isEmpty(blueprints)) {
      return Collections.EMPTY_MAP;
    }

    final List<List<BlueprintProperty>> bluePrintPropertiesList =
        blueprints.stream()
            .filter(blueprint -> isNotEmpty(blueprint.getProperties()))
            .filter(blueprint -> blueprint.getServiceId().equals(infrastructureMapping.getServiceId()))
            .filter(blueprint
                -> blueprint.infrastructureMappingType().name().equals(infrastructureMapping.getInfraMappingType()))
            .map(InfrastructureMappingBlueprint::getProperties)
            .collect(Collectors.toList());

    if (bluePrintPropertiesList.isEmpty()) {
      logger.info(StringUtils.join(DEBUG_LINE,
          format("No service mapping for serviceId %s found "
                  + "with provisioner %s linked to infraMapping",
              infrastructureMapping.getServiceId(), infrastructureMapping.getProvisionerId(),
              infrastructureMapping.getUuid())));
    } else if (bluePrintPropertiesList.size() > 1) {
      logger.error(StringUtils.join(DEBUG_LINE,
          format("Provisioner %s has more than 1 service "
                  + "mappings "
                  + "for 1 service %s",
              infrastructureMapping.getProvisionerId(), infrastructureMapping.getServiceId())));
    } else {
      List<BlueprintProperty> blueprintProperties = bluePrintPropertiesList.get(0);

      return blueprintProperties.stream()
          .map(blueprintProperty -> {
            List<NameValuePair> fields =
                isEmpty(blueprintProperty.getFields()) ? new ArrayList<>() : blueprintProperty.getFields();
            String name = fieldNameChanges.getOrDefault(blueprintProperty.getName(), blueprintProperty.getName());
            fields.add(NameValuePair.builder().name(name).value(blueprintProperty.getValue()).build());
            fields.forEach(nameValuePair
                -> nameValuePair.setName(
                    fieldNameChanges.getOrDefault(nameValuePair.getName(), nameValuePair.getName())));
            return fields;
          })
          .flatMap(Collection::stream)
          .filter(nameValuePair -> isNotEmpty(nameValuePair.getValue()) && isNotEmpty(nameValuePair.getName()))
          .collect(Collectors.toMap(NameValuePair::getName, NameValuePair::getValue, (value1, value2) -> {
            logger.info(StringUtils.join(DEBUG_LINE,
                " Found duplicate value for keys in "
                    + "provisioner for infra mapping ",
                infrastructureMapping.getProvisionerId()));
            return value1;
          }));
    }
    return Collections.EMPTY_MAP;
  }

  private void setInfraDefinitionId(String infraDefinitionId, InfrastructureMapping infrastructureMapping) {
    Map<String, Object> toUpdate = new HashMap<>();
    toUpdate.put(InfrastructureMappingKeys.infrastructureDefinitionId, infraDefinitionId);
    try {
      wingsPersistence.updateFields(InfrastructureMapping.class, infrastructureMapping.getUuid(), toUpdate);
    } catch (Exception ex) {
      logger.error(StringUtils.join(DEBUG_LINE,
          "Could not set infradefinition Id for infra "
              + "mapping ",
          infrastructureMapping.getUuid()));
    }
  }
}
