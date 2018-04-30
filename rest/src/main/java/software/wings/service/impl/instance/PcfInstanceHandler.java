package software.wings.service.impl.instance;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import software.wings.api.DeploymentInfo;
import software.wings.api.PcfDeploymentInfo;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.PcfConfig;
import software.wings.beans.PcfInfrastructureMapping;
import software.wings.beans.SettingAttribute;
import software.wings.beans.infrastructure.instance.Instance;
import software.wings.beans.infrastructure.instance.Instance.InstanceBuilder;
import software.wings.beans.infrastructure.instance.info.InstanceInfo;
import software.wings.beans.infrastructure.instance.info.PcfInstanceInfo;
import software.wings.beans.infrastructure.instance.key.PcfInstanceKey;
import software.wings.exception.HarnessException;
import software.wings.service.impl.PcfHelperService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.utils.Validator;

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
        List<PcfInstanceInfo> latestpcfInstanceInfoList = pcfHelperService.getApplicationDetails(pcfApplicationName,
            pcfInfrastructureMapping.getOrganization(), pcfInfrastructureMapping.getSpace(), pcfConfig);

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

        SetView<String> instancesToBeUpdated =
            Sets.intersection(latestPcfInstanceInfoMap.keySet(), instancesInDBMap.keySet());

        // Find the instances that were yet to be added to db
        SetView<String> instancesToBeAdded =
            Sets.difference(latestPcfInstanceInfoMap.keySet(), instancesInDBMap.keySet());

        SetView<String> instancesToBeDeleted =
            Sets.difference(instancesInDBMap.keySet(), latestPcfInstanceInfoMap.keySet());

        //        instancesToBeUpdated.stream().forEach(id -> {
        //          PcfInstanceInfo pcfInstanceInfo = latestPcfInstanceInfoMap.get(id);
        //          Instance instance = buildInstanceFromPCFInfo(pcfInfrastructureMapping, pcfInstanceInfo,
        //          newDeploymentInfo); logger.info("Updating Instance: " + pcfInstanceInfo.getId() + ", for
        //          PcfApplication:- " + pcfApplicationName); instanceService.saveOrUpdate(instance);
        //        });

        Set<String> instanceIdsToBeDeleted = new HashSet<>();
        instancesToBeDeleted.stream().forEach(id -> {
          Instance instance = instancesInDBMap.get(id);
          if (instance != null) {
            instanceIdsToBeDeleted.add(instance.getUuid());
          }
        });

        logger.info("Total no of instances found in DB for InfraMappingId: {} and AppId: {}, "
                + "No of instances in DB: {}, No of Running instances: {}, No of instances updated: {}, "
                + "No of instances to be Added: {}, No of instances to be deleted: {}",
            infraMappingId, appId, instancesInDB.size(), latestPcfInstanceInfoMap.keySet().size(),
            instancesToBeUpdated.size(), instancesToBeAdded.size(), instanceIdsToBeDeleted.size());
        if (isNotEmpty(instanceIdsToBeDeleted)) {
          instanceService.delete(instanceIdsToBeDeleted);
        }

        if (isNotEmpty(instancesToBeAdded)) {
          instancesToBeAdded.forEach(id -> {
            DeploymentInfo deploymentInfo = null;

            PcfInstanceInfo pcfInstanceInfo = latestPcfInstanceInfoMap.get(id);
            logger.info("Adding Instance: " + pcfInstanceInfo.getId() + ", for PcfApplication:- " + pcfApplicationName);
            /**
             * If coming from Sync_Job, newDeploymentInfo will be null, so fetch previous instance
             */
            if (newDeploymentInfo == null) {
              Optional<Instance> optional = instancesInDB.stream()
                                                .filter(instance -> matchPcfApplicationGuid(instance, pcfInstanceInfo))
                                                .findFirst();
              deploymentInfo =
                  optional.isPresent() ? generateDeploymentInfoFromEarlierDeployment(optional.get()) : null;
            } else {
              deploymentInfo = newDeploymentInfo;
            }

            Instance instance = buildInstanceFromPCFInfo(pcfInfrastructureMapping, pcfInstanceInfo, deploymentInfo);
            instanceService.saveOrUpdate(instance);
          });
        }
      });
    }
  }

  // application GUIDs should match, means belong to same pcf app
  private boolean matchPcfApplicationGuid(Instance instance, PcfInstanceInfo pcfInstanceInfo) {
    return pcfInstanceInfo.getPcfApplicationGuid().equals(
        ((PcfInstanceInfo) instance.getInstanceInfo()).getPcfApplicationGuid());
  }

  private DeploymentInfo generateDeploymentInfoFromEarlierDeployment(Instance instance) {
    return PcfDeploymentInfo.builder()
        .appId(instance.getAppId())
        .accountId(instance.getAccountId())
        .infraMappingId(instance.getInfraMappingId())
        .workflowExecutionId(instance.getLastWorkflowExecutionId())
        .workflowExecutionName(instance.getLastWorkflowExecutionName())
        .workflowId(instance.getLastWorkflowExecutionId())

        .artifactId(instance.getLastArtifactId())
        .artifactName(instance.getLastArtifactName())
        .artifactStreamId(instance.getLastArtifactStreamId())
        .artifactSourceName(instance.getLastArtifactSourceName())
        .artifactBuildNum(instance.getLastArtifactBuildNum())

        .pipelineExecutionId(instance.getLastPipelineExecutionId())
        .pipelineExecutionName(instance.getLastPipelineExecutionName())

        // Commented this out, so we can distinguish between autoscales instances and instances we deployed
        .deployedById(AUTO_SCALE)
        .deployedByName(AUTO_SCALE)
        .deployedAt(System.currentTimeMillis())
        .artifactBuildNum(instance.getLastArtifactBuildNum())
        .build();
  }

  private Instance buildInstanceFromPCFInfo(
      InfrastructureMapping infrastructureMapping, PcfInstanceInfo pcfInstanceInfo, DeploymentInfo newDeploymentInfo) {
    InstanceBuilder builder = instanceHelper.buildInstanceBase(null, infrastructureMapping, newDeploymentInfo);
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
}
