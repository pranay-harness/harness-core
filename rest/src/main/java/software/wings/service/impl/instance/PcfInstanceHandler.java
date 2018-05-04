package software.wings.service.impl.instance;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.util.stream.Collectors.toList;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import software.wings.api.DeploymentInfo;
import software.wings.api.PcfDeploymentInfo;
import software.wings.api.PhaseExecutionData;
import software.wings.api.pcf.PcfDeployExecutionSummary;
import software.wings.api.pcf.PcfServiceData;
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
import software.wings.common.Constants;
import software.wings.exception.HarnessException;
import software.wings.exception.WingsException;
import software.wings.helpers.ext.pcf.PcfAppNotFoundException;
import software.wings.service.impl.PcfHelperService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.sm.PhaseExecutionSummary;
import software.wings.sm.PhaseStepExecutionSummary;
import software.wings.sm.StepExecutionSummary;
import software.wings.utils.Validator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Singleton
public class PcfInstanceHandler extends InstanceHandler {
  @Inject private InfrastructureMappingService infraMappingService;
  @Inject private PcfHelperService pcfHelperService;

  @Override
  public void syncInstances(String appId, String infraMappingId) throws HarnessException {
    Multimap<String, Instance> pcfAppNameInstanceMap = ArrayListMultimap.create();
    syncInstancesInternal(appId, infraMappingId, pcfAppNameInstanceMap, null);
  }

  /**
   *
   * @param appId
   * @param infraMappingId
   * @param pcfAppNameInstanceMap  key - pcfAppName     value - Instances
   * @throws HarnessException
   */
  private void syncInstancesInternal(String appId, String infraMappingId,
      Multimap<String, Instance> pcfAppNameInstanceMap, DeploymentInfo newDeploymentInfo) throws HarnessException {
    logger.info("# Performing PCF Instance sync");
    InfrastructureMapping infrastructureMapping = infraMappingService.get(appId, infraMappingId);
    Validator.notNullCheck("Infra mapping is null for id:" + infraMappingId, infrastructureMapping);

    if (!(infrastructureMapping instanceof PcfInfrastructureMapping)) {
      String msg =
          "Incompatible infra mapping type. Expecting PCF type. Found:" + infrastructureMapping.getInfraMappingType();
      logger.error(msg);
      throw new HarnessException(msg);
    }

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
          Validator.notNullCheck("latestpcfInstanceInfoList", latestpcfInstanceInfoList);
          logger.info("Received Instance details for Instance count: " + latestpcfInstanceInfoList.size());

          Map<String, PcfInstanceInfo> latestPcfInstanceInfoMap = latestpcfInstanceInfoList.stream().collect(
              Collectors.toMap(PcfInstanceInfo::getId, pcfInstanceInfo -> pcfInstanceInfo));

          Collection<Instance> instancesInDB = pcfAppNameInstanceMap.get(pcfApplicationName);

          Map<String, Instance> instancesInDBMap = Maps.newHashMap();

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

          Set<String> instanceIdsToBeDeleted = new HashSet<>();
          instancesToBeDeleted.stream().forEach(id -> {
            Instance instance = instancesInDBMap.get(id);
            if (instance != null) {
              instanceIdsToBeDeleted.add(instance.getUuid());
            }
          });

          logger.info("Total no of instances found in DB for InfraMappingId: {} and AppId: {}, "
                  + "No of instances in DB: {}, No of Running instances: {}, "
                  + "No of instances to be Added: {}, No of instances to be deleted: {}",
              infraMappingId, appId, instancesInDB.size(), latestPcfInstanceInfoMap.keySet().size(),
              instancesToBeAdded.size(), instanceIdsToBeDeleted.size());
          if (isNotEmpty(instanceIdsToBeDeleted)) {
            instanceService.delete(instanceIdsToBeDeleted);
          }

          DeploymentInfo deploymentInfo;
          if (isNotEmpty(instancesToBeAdded)) {
            // newDeploymentInfo would be null in case of sync job.
            if (newDeploymentInfo == null && isNotEmpty(instancesInDB)) {
              Optional<Instance> instanceWithExecutionInfoOptional = getInstanceWithExecutionInfo(instancesInDB);
              if (!instanceWithExecutionInfoOptional.isPresent()) {
                logger.error("Couldn't find an instance from a previous deployment for inframapping {}",
                    infrastructureMapping.getUuid());
                return;
              }

              DeploymentInfo deploymentInfoFromPrevious = PcfDeploymentInfo.builder().build();
              generateDeploymentInfoFromInstance(instanceWithExecutionInfoOptional.get(), deploymentInfoFromPrevious);
              deploymentInfo = deploymentInfoFromPrevious;
            } else {
              deploymentInfo = newDeploymentInfo;
            }

            instancesToBeAdded.forEach(containerId -> {
              PcfInstanceInfo pcfInstanceInfo = latestPcfInstanceInfoMap.get(containerId);
              Instance instance = buildInstanceFromPCFInfo(pcfInfrastructureMapping, pcfInstanceInfo, deploymentInfo);
              instanceService.saveOrUpdate(instance);
            });
          }
        }
      });
    }
  }

  // application GUIDs should match, means belong to same pcf app
  private boolean matchPcfApplicationGuid(Instance instance, PcfInstanceInfo pcfInstanceInfo) {
    return pcfInstanceInfo.getPcfApplicationGuid().equals(
        ((PcfInstanceInfo) instance.getInstanceInfo()).getPcfApplicationGuid());
  }

  private Instance buildInstanceFromPCFInfo(
      InfrastructureMapping infrastructureMapping, PcfInstanceInfo pcfInstanceInfo, DeploymentInfo newDeploymentInfo) {
    InstanceBuilder builder = buildInstanceBase(null, infrastructureMapping, newDeploymentInfo);
    builder.pcfInstanceKey(PcfInstanceKey.builder().id(pcfInstanceInfo.getId()).build());
    builder.instanceInfo(pcfInstanceInfo);

    return builder.build();
  }

  private void loadPcfAppNameInstanceMap(String appId, String infraMappingId,
      Multimap<String, Instance> pcfApplicationNameInstanceMap) throws HarnessException {
    List<Instance> instanceListInDBForInfraMapping = getInstances(appId, infraMappingId);
    for (Instance instance : instanceListInDBForInfraMapping) {
      InstanceInfo instanceInfo = instance.getInstanceInfo();
      if (instanceInfo instanceof PcfInstanceInfo) {
        PcfInstanceInfo pcfInstanceInfo = (PcfInstanceInfo) instanceInfo;
        String pcfAppName = pcfInstanceInfo.getPcfApplicationName();
        pcfApplicationNameInstanceMap.put(pcfAppName, instance);
      } else {
        throw new HarnessException("UnSupported instance deploymentInfo type" + instance.getInstanceType().name());
      }
    }
  }

  @Override
  public void handleNewDeployment(DeploymentInfo deploymentInfo) throws HarnessException {
    PcfDeploymentInfo pcfDeploymentInfo = (PcfDeploymentInfo) deploymentInfo;
    Multimap<String, Instance> pcfApplicationNameInstanceMap = ArrayListMultimap.create();
    pcfDeploymentInfo.getPcfApplicationNameSet().stream().forEach(
        pcfApplicationName -> pcfApplicationNameInstanceMap.put(pcfApplicationName, null));

    syncInstancesInternal(
        deploymentInfo.getAppId(), deploymentInfo.getInfraMappingId(), pcfApplicationNameInstanceMap, deploymentInfo);
  }

  @Override
  public Optional<DeploymentInfo> getDeploymentInfo(PhaseExecutionData phaseExecutionData,
      WorkflowExecution workflowExecution, InfrastructureMapping infrastructureMapping, String stateExecutionInstanceId,
      Artifact artifact) throws HarnessException {
    PhaseExecutionSummary phaseExecutionSummary = phaseExecutionData.getPhaseExecutionSummary();
    if (phaseExecutionSummary != null) {
      Map<String, PhaseStepExecutionSummary> phaseStepExecutionSummaryMap =
          phaseExecutionSummary.getPhaseStepExecutionSummaryMap();
      if (phaseStepExecutionSummaryMap != null) {
        PhaseStepExecutionSummary phaseStepExecutionSummary = phaseStepExecutionSummaryMap.get(Constants.DEPLOY);
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
          if (stepExecutionSummary != null && stepExecutionSummary instanceof PcfDeployExecutionSummary) {
            PcfDeployExecutionSummary pcfDeployExecutionSummary = (PcfDeployExecutionSummary) stepExecutionSummary;

            Set<String> pcfSvcNameSet = Sets.newHashSet();

            if (pcfDeployExecutionSummary.getInstaceData() != null) {
              pcfSvcNameSet.addAll(
                  pcfDeployExecutionSummary.getInstaceData().stream().map(PcfServiceData::getName).collect(toList()));
            }

            if (pcfSvcNameSet.isEmpty()) {
              logger.warn(
                  "Both old and new app resize details are empty. Cannot proceed for phase step for state execution instance: {}",
                  stateExecutionInstanceId);
              return Optional.empty();
            }

            Validator.notNullCheck(
                "pcfSvcNameSet is null for workflow execution: " + workflowExecution.getUuid(), pcfSvcNameSet);

            if (pcfSvcNameSet.isEmpty()) {
              String msg = "No pcf service names processed by the event for workflow execution {}";
              logger.error(msg, workflowExecution.getUuid());
              throw new WingsException(msg);
            }

            PcfDeploymentInfo pcfDeploymentInfo =
                PcfDeploymentInfo.builder().pcfApplicationNameSet(pcfSvcNameSet).build();
            return Optional.of(pcfDeploymentInfo);
          }
        }
      }
    }

    return Optional.empty();
  }
}
