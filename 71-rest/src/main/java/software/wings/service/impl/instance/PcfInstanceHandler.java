package software.wings.service.impl.instance;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.validation.Validator.notNullCheck;
import static java.util.function.Function.identity;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.WingsException;
import lombok.extern.slf4j.Slf4j;
import software.wings.api.DeploymentInfo;
import software.wings.api.DeploymentSummary;
import software.wings.api.PcfDeploymentInfo;
import software.wings.api.PhaseExecutionData;
import software.wings.api.PhaseStepExecutionData;
import software.wings.api.ondemandrollback.OnDemandRollbackInfo;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.PcfConfig;
import software.wings.beans.PcfInfrastructureMapping;
import software.wings.beans.SettingAttribute;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.infrastructure.instance.Instance;
import software.wings.beans.infrastructure.instance.Instance.InstanceBuilder;
import software.wings.beans.infrastructure.instance.info.InstanceInfo;
import software.wings.beans.infrastructure.instance.info.PcfInstanceInfo;
import software.wings.beans.infrastructure.instance.key.PcfInstanceKey;
import software.wings.beans.infrastructure.instance.key.deployment.DeploymentKey;
import software.wings.beans.infrastructure.instance.key.deployment.PcfDeploymentKey;
import software.wings.helpers.ext.pcf.PcfAppNotFoundException;
import software.wings.service.impl.PcfHelperService;
import software.wings.sm.PhaseStepExecutionSummary;
import software.wings.sm.StepExecutionSummary;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Singleton
@Slf4j
public class PcfInstanceHandler extends InstanceHandler {
  @Inject private PcfHelperService pcfHelperService;

  @Override
  public void syncInstances(String appId, String infraMappingId) {
    Multimap<String, Instance> pcfAppNameInstanceMap = ArrayListMultimap.create();
    syncInstancesInternal(appId, infraMappingId, pcfAppNameInstanceMap, null, false,
        OnDemandRollbackInfo.builder().onDemandRollback(false).build());
  }

  /**
   *
   * @param appId
   * @param infraMappingId
   * @param pcfAppNameInstanceMap  key - pcfAppName     value - Instances
   */
  private void syncInstancesInternal(String appId, String infraMappingId,
      Multimap<String, Instance> pcfAppNameInstanceMap, List<DeploymentSummary> newDeploymentSummaries,
      boolean rollback, OnDemandRollbackInfo onDemandRollbackInfo) {
    logger.info("# Performing PCF Instance sync");
    InfrastructureMapping infrastructureMapping = infraMappingService.get(appId, infraMappingId);
    notNullCheck("Infra mapping is null for id:" + infraMappingId, infrastructureMapping);

    if (!(infrastructureMapping instanceof PcfInfrastructureMapping)) {
      String msg =
          "Incompatible infra mapping type. Expecting PCF type. Found:" + infrastructureMapping.getInfraMappingType();
      logger.error(msg);
      throw WingsException.builder().message(msg).build();
    }

    Map<String, DeploymentSummary> pcfAppNamesNewDeploymentSummaryMap = getDeploymentSummaryMap(newDeploymentSummaries);

    loadPcfAppNameInstanceMap(appId, infraMappingId, pcfAppNameInstanceMap);

    PcfInfrastructureMapping pcfInfrastructureMapping = (PcfInfrastructureMapping) infrastructureMapping;
    SettingAttribute settingAttribute = settingsService.get(pcfInfrastructureMapping.getComputeProviderSettingId());
    PcfConfig pcfConfig = (PcfConfig) settingAttribute.getValue();

    // This is to handle the case of the instances stored in the new schema.
    if (pcfAppNameInstanceMap.size() > 0) {
      pcfAppNameInstanceMap.keySet().forEach(pcfApplicationName -> {
        logger.info("Performing sync for PCFApplicationName: " + pcfApplicationName + "HarnessAppId: " + appId);
        // Get all the instances for the given containerSvcName (In kubernetes, this is replication Controller and in
        // ECS it is taskDefinition)
        boolean failedToRetrieveData = false;
        List<PcfInstanceInfo> latestpcfInstanceInfoList = new ArrayList<>();
        try {
          latestpcfInstanceInfoList = pcfHelperService.getApplicationDetails(pcfApplicationName,
              pcfInfrastructureMapping.getOrganization(), pcfInfrastructureMapping.getSpace(), pcfConfig);
        } catch (Exception e) {
          logger.warn("Error while fetching application details for PCFApplication", e);

          if (e instanceof PcfAppNotFoundException) {
            latestpcfInstanceInfoList = new ArrayList<>();
          } else {
            // skip processing this time, as app exists but we could not fetch data
            failedToRetrieveData = true;
          }
        }

        if (!failedToRetrieveData) {
          notNullCheck("latestpcfInstanceInfoList", latestpcfInstanceInfoList);
          logger.info("Received Instance details for Instance count: " + latestpcfInstanceInfoList.size());

          Map<String, PcfInstanceInfo> latestPcfInstanceInfoMap =
              latestpcfInstanceInfoList.stream().collect(Collectors.toMap(PcfInstanceInfo::getId, identity()));

          Collection<Instance> instancesInDB = pcfAppNameInstanceMap.get(pcfApplicationName);

          Map<String, Instance> instancesInDBMap = new HashMap<>();

          // If there are prior instances in db already
          if (isNotEmpty(instancesInDB)) {
            instancesInDB.forEach(instance -> {
              if (instance != null) {
                instancesInDBMap.put(instance.getPcfInstanceKey().getId(), instance);
              }
            });
          }

          // Find the instances that were yet to be added to db
          SetView<String> instancesToBeAdded =
              Sets.difference(latestPcfInstanceInfoMap.keySet(), instancesInDBMap.keySet());

          SetView<String> instancesToBeDeleted =
              Sets.difference(instancesInDBMap.keySet(), latestPcfInstanceInfoMap.keySet());

          if (onDemandRollbackInfo.isOnDemandRollback()) {
            handleOnDemandRollbackDeployment(
                instancesInDBMap, latestPcfInstanceInfoMap, onDemandRollbackInfo.getRollbackExecutionId());
          }

          Set<String> instanceIdsToBeDeleted = new HashSet<>();
          instancesToBeDeleted.forEach(id -> {
            Instance instance = instancesInDBMap.get(id);
            if (instance != null) {
              instanceIdsToBeDeleted.add(instance.getUuid());
            }
          });
          logger.info("Instances to be deleted {}", instanceIdsToBeDeleted.size());

          logger.info("Total no of instances found in DB for AppId: {}, "
                  + "No of instances in DB: {}, No of Running instances: {}, "
                  + "No of instances to be Added: {}, No of instances to be deleted: {}",
              appId, instancesInDB.size(), latestPcfInstanceInfoMap.keySet().size(), instancesToBeAdded.size(),
              instanceIdsToBeDeleted.size());
          if (isNotEmpty(instanceIdsToBeDeleted)) {
            instanceService.delete(instanceIdsToBeDeleted);
          }

          DeploymentSummary deploymentSummary;
          if (isNotEmpty(instancesToBeAdded)) {
            // newDeploymentInfo would be null in case of sync job.
            if ((newDeploymentSummaries == null || !pcfAppNamesNewDeploymentSummaryMap.containsKey(pcfApplicationName))
                && isNotEmpty(instancesInDB)) {
              Optional<Instance> instanceWithExecutionInfoOptional = getInstanceWithExecutionInfo(instancesInDB);
              if (!instanceWithExecutionInfoOptional.isPresent()) {
                logger.warn("Couldn't find an instance from a previous deployment for infra mapping");
                return;
              }
              DeploymentSummary deploymentSummaryFromPrevious =
                  DeploymentSummary.builder().deploymentInfo(PcfDeploymentInfo.builder().build()).build();
              generateDeploymentSummaryFromInstance(
                  instanceWithExecutionInfoOptional.get(), deploymentSummaryFromPrevious);
              deploymentSummary = deploymentSummaryFromPrevious;
            } else {
              deploymentSummary = getDeploymentSummaryForInstanceCreation(
                  pcfAppNamesNewDeploymentSummaryMap.get(pcfApplicationName), rollback);
            }

            instancesToBeAdded.forEach(containerId -> {
              PcfInstanceInfo pcfInstanceInfo = latestPcfInstanceInfoMap.get(containerId);
              // For rollback fetch existing data from deployment summary table
              Instance instance =
                  buildInstanceFromPCFInfo(pcfInfrastructureMapping, pcfInstanceInfo, deploymentSummary);
              instanceService.save(instance);
            });
            logger.info("Instances to be added {}", instancesToBeAdded.size());
          }
        }
      });
    }
  }

  private void handleOnDemandRollbackDeployment(Map<String, Instance> instancesInDBMap,
      Map<String, PcfInstanceInfo> latestPcfInstanceInfoMap, String rollbackExecutionId) {
    SetView<String> instancesToBeUpdated =
        Sets.intersection(instancesInDBMap.keySet(), latestPcfInstanceInfoMap.keySet());
    instancesToBeUpdated.forEach(id -> {
      Instance instance = instancesInDBMap.get(id);
      if (instance != null && rollbackExecutionId != null) {
        instance.setLastWorkflowExecutionId(rollbackExecutionId);
        instance.setLastDeployedAt(System.currentTimeMillis());
        instanceService.saveOrUpdate(instance);
      }
    });
  }

  private Map<String, DeploymentSummary> getDeploymentSummaryMap(List<DeploymentSummary> newDeploymentSummaries) {
    if (EmptyPredicate.isEmpty(newDeploymentSummaries)) {
      return Collections.emptyMap();
    }

    Map<String, DeploymentSummary> deploymentSummaryMap = new HashMap<>();
    newDeploymentSummaries.forEach(deploymentSummary
        -> deploymentSummaryMap.put(
            ((PcfDeploymentInfo) deploymentSummary.getDeploymentInfo()).getApplicationName(), deploymentSummary));

    return deploymentSummaryMap;
  }

  // application GUIDs should match, means belong to same pcf app
  private boolean matchPcfApplicationGuid(Instance instance, PcfInstanceInfo pcfInstanceInfo) {
    return pcfInstanceInfo.getPcfApplicationGuid().equals(
        ((PcfInstanceInfo) instance.getInstanceInfo()).getPcfApplicationGuid());
  }

  private Instance buildInstanceFromPCFInfo(InfrastructureMapping infrastructureMapping,
      PcfInstanceInfo pcfInstanceInfo, DeploymentSummary deploymentSummary) {
    InstanceBuilder builder = buildInstanceBase(null, infrastructureMapping, deploymentSummary);
    builder.pcfInstanceKey(PcfInstanceKey.builder().id(pcfInstanceInfo.getId()).build());
    builder.instanceInfo(pcfInstanceInfo);

    return builder.build();
  }

  private void loadPcfAppNameInstanceMap(
      String appId, String infraMappingId, Multimap<String, Instance> pcfApplicationNameInstanceMap) {
    List<Instance> instanceListInDBForInfraMapping = getInstances(appId, infraMappingId);
    for (Instance instance : instanceListInDBForInfraMapping) {
      InstanceInfo instanceInfo = instance.getInstanceInfo();
      if (instanceInfo instanceof PcfInstanceInfo) {
        PcfInstanceInfo pcfInstanceInfo = (PcfInstanceInfo) instanceInfo;
        String pcfAppName = pcfInstanceInfo.getPcfApplicationName();
        pcfApplicationNameInstanceMap.put(pcfAppName, instance);
      } else {
        throw WingsException.builder()
            .message("UnSupported instance deploymentInfo type" + instance.getInstanceType().name())
            .build();
      }
    }
  }

  @Override
  public void handleNewDeployment(
      List<DeploymentSummary> deploymentSummaries, boolean rollback, OnDemandRollbackInfo onDemandRollbackInfo) {
    Multimap<String, Instance> pcfApplicationNameInstanceMap = ArrayListMultimap.create();
    deploymentSummaries.forEach(deploymentSummary -> {
      PcfDeploymentInfo pcfDeploymentInfo = (PcfDeploymentInfo) deploymentSummary.getDeploymentInfo();
      pcfApplicationNameInstanceMap.put(pcfDeploymentInfo.getApplicationName(), null);
    });

    syncInstancesInternal(deploymentSummaries.iterator().next().getAppId(),
        deploymentSummaries.iterator().next().getInfraMappingId(), pcfApplicationNameInstanceMap, deploymentSummaries,
        rollback, onDemandRollbackInfo);
  }

  @Override
  public Optional<List<DeploymentInfo>> getDeploymentInfo(PhaseExecutionData phaseExecutionData,
      PhaseStepExecutionData phaseStepExecutionData, WorkflowExecution workflowExecution,
      InfrastructureMapping infrastructureMapping, String stateExecutionInstanceId, Artifact artifact) {
    PhaseStepExecutionSummary phaseStepExecutionSummary = phaseStepExecutionData.getPhaseStepExecutionSummary();
    if (phaseStepExecutionSummary == null) {
      if (logger.isDebugEnabled()) {
        logger.debug("PhaseStepExecutionSummary is null for stateExecutionInstanceId: " + stateExecutionInstanceId);
      }
      return Optional.empty();
    }

    List<StepExecutionSummary> stepExecutionSummaryList = phaseStepExecutionSummary.getStepExecutionSummaryList();
    // This was observed when the "deploy containers" step was executed in rollback and no commands were
    // executed since setup failed.
    if (stepExecutionSummaryList == null) {
      if (logger.isDebugEnabled()) {
        logger.debug("StepExecutionSummaryList is null for stateExecutionInstanceId: " + stateExecutionInstanceId);
      }
      return Optional.empty();
    }

    for (StepExecutionSummary stepExecutionSummary : stepExecutionSummaryList) {
      if (stepExecutionSummary instanceof DeploymentInfoExtractor) {
        return ((DeploymentInfoExtractor) stepExecutionSummary).extractDeploymentInfo();
      }
    }
    return Optional.empty();
  }

  @Override
  public DeploymentKey generateDeploymentKey(DeploymentInfo deploymentInfo) {
    return PcfDeploymentKey.builder()
        .applicationName(((PcfDeploymentInfo) deploymentInfo).getApplicationName())
        .build();
  }

  @Override
  protected void setDeploymentKey(DeploymentSummary deploymentSummary, DeploymentKey deploymentKey) {
    if (deploymentKey instanceof PcfDeploymentKey) {
      deploymentSummary.setPcfDeploymentKey((PcfDeploymentKey) deploymentKey);
    } else {
      throw WingsException.builder()
          .message("Invalid deploymentKey passed for PcfDeploymentKey" + deploymentKey)
          .build();
    }
  }
}
