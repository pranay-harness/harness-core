package software.wings.service;

import static software.wings.beans.FeatureName.MOVE_PCF_INSTANCE_SYNC_TO_PERP_TASK;
import static software.wings.beans.FeatureName.STOP_PROCESSING_INSTANCE_SYNC_FROM_ITERATOR_PCF_DEPLOYMENTS;
import static software.wings.beans.InfrastructureMapping.APP_ID_KEY;
import static software.wings.beans.InfrastructureMapping.INFRA_MAPPING_TYPE_KEY;
import static software.wings.beans.InfrastructureMappingType.PCF_PCF;
import static software.wings.service.InstanceSyncController.InstanceSyncFlow.ITERATOR_INSTANCE_SYNC;
import static software.wings.service.InstanceSyncController.InstanceSyncFlow.NEW_DEPLOYMENT;
import static software.wings.service.InstanceSyncController.InstanceSyncFlow.PERPETUAL_TASK;

import com.google.inject.Inject;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.beans.SearchFilter;
import io.harness.perpetualtask.instanceSync.PcfInstanceSyncPerpTaskClient;
import io.harness.perpetualtask.instanceSync.PcfInstanceSyncPerpTaskClientParams;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import software.wings.api.DeploymentSummary;
import software.wings.api.PcfDeploymentInfo;
import software.wings.beans.FeatureName;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureMappingType;
import software.wings.beans.infrastructure.instance.Instance;
import software.wings.beans.infrastructure.instance.info.PcfInstanceInfo;
import software.wings.service.impl.instance.InstanceHandler;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.instance.InstanceService;

import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PCFInstanceSyncPerpetualTaskController implements InstanceSyncPerpetualTaskController {
  @Inject FeatureFlagService featureFlagService;
  @Inject AppService appService;
  @Inject InstanceService instanceService;
  @Inject InfrastructureMappingService infrastructureMappingService;
  @Inject PcfInstanceSyncPerpTaskClient pcfPerpTaskClient;

  private FeatureName perpetualTaskCreateFlag = MOVE_PCF_INSTANCE_SYNC_TO_PERP_TASK;
  private FeatureName stopIteratorSyncFlag = STOP_PROCESSING_INSTANCE_SYNC_FROM_ITERATOR_PCF_DEPLOYMENTS;

  @Override
  public boolean shouldSkipIteratorInstanceSync(InfrastructureMapping infrastructureMapping) {
    return featureFlagService.isEnabled(stopIteratorSyncFlag, infrastructureMapping.getAccountId());
  }

  public boolean canUpdateDb(InstanceSyncController.InstanceSyncFlow instanceSyncFlow, String accountId,
      Class<? extends InstanceHandler> callerClass) {
    // new deployment should always go through.
    if (instanceSyncFlow.equals(NEW_DEPLOYMENT)) {
      return true;
    }

    boolean isPerpetualTaskEnabled = featureFlagService.isEnabled(perpetualTaskCreateFlag, accountId);

    // if ff is enabled, PERPETUAL_TASK should go through otherwise instance_sync should go through
    return isPerpetualTaskEnabled ? instanceSyncFlow.equals(PERPETUAL_TASK)
                                  : instanceSyncFlow.equals(ITERATOR_INSTANCE_SYNC);
  }

  @Override
  public boolean enablePerpetualTaskForAccount(String accountId) {
    // enable feature flag to enable to perpetual task creation for new deployments
    featureFlagService.enableAccount(MOVE_PCF_INSTANCE_SYNC_TO_PERP_TASK, accountId);
    List<String> appIdsByAccountId = appService.getAppIdsByAccountId(accountId);

    HashMap<String, List<InfrastructureMapping>> appInfraStructureMappingMap = new HashMap<>();
    appIdsByAccountId.forEach(app -> {
      List<InfrastructureMapping> infraMappingListForApp = getInfrastructureMappingsForApp(app);
      appInfraStructureMappingMap.put(app, infraMappingListForApp);
    });

    logger.info("Found {} inframapping for account: {}",
        appInfraStructureMappingMap.values().stream().mapToInt(List::size).sum(), accountId);

    appInfraStructureMappingMap.forEach(
        (key, infrastructureMappingList)
            -> infrastructureMappingList.forEach(infrastructureMapping
                -> createPerpetualTaskForInfraMappingId(
                    key, infrastructureMapping.getUuid(), infrastructureMapping.getAccountId())));
    return true;
  }

  @Override
  public boolean createPerpetualTaskForNewDeployment(
      InfrastructureMappingType infrastructureMappingType, List<DeploymentSummary> deploymentSummaries) {
    String appId = deploymentSummaries.iterator().next().getAppId();
    String infraId = deploymentSummaries.iterator().next().getInfraMappingId();
    String accountId = deploymentSummaries.iterator().next().getAccountId();

    if (!featureFlagService.isEnabled(MOVE_PCF_INSTANCE_SYNC_TO_PERP_TASK, accountId)) {
      logger.info("FF MOVE_PCF_INSTANCE_SYNC_TO_PERP_TASK is disabled for accountid: {}, inframapping type: {}",
          accountId, infrastructureMappingType);
      return false;
    }

    logger.info("Creating perpetual tasks for new deployments for account: {}", accountId);
    Set<String> applicationNameSet = deploymentSummaries.stream()
                                         .map(deploymentSummary -> {
                                           PcfDeploymentInfo deploymentInfo =
                                               (PcfDeploymentInfo) deploymentSummary.getDeploymentInfo();
                                           return deploymentInfo.getApplicationName();
                                         })
                                         .collect(Collectors.toSet());

    createPerpetualTaskForApplicationName(appId, infraId, accountId, applicationNameSet);
    return true;
  }

  private void createPerpetualTaskForInfraMappingId(String appId, String infraMappingId, String accountId) {
    Set<String> applicationNames = getApplicationNames(appId, infraMappingId);
    createPerpetualTaskForApplicationName(appId, infraMappingId, accountId, applicationNames);
  }

  private void createPerpetualTaskForApplicationName(
      String appId, String infraMappingId, String accountId, Set<String> applicationNames) {
    applicationNames.forEach(applicationName -> {
      logger.info("Creating perpetual tasks for appId: {}, inframapping: {}, applicationNames size: {}", appId,
          infraMappingId, applicationNames.size());
      PcfInstanceSyncPerpTaskClientParams pcfInstanceSyncParams = PcfInstanceSyncPerpTaskClientParams.builder()
                                                                      .appId(appId)
                                                                      .inframappingId(infraMappingId)
                                                                      .applicationName(applicationName)
                                                                      .build();
      pcfPerpTaskClient.create(accountId, pcfInstanceSyncParams);
    });
  }

  private Set<String> getApplicationNames(String appId, String infraMappingId) {
    List<Instance> instanceList = instanceService.getInstancesForAppAndInframapping(appId, infraMappingId);
    return instanceList.stream()
        .map(instance -> {
          PcfInstanceInfo instanceInfo = (PcfInstanceInfo) instance.getInstanceInfo();
          return instanceInfo.getPcfApplicationName();
        })
        .collect(Collectors.toSet());
  }

  private List<InfrastructureMapping> getInfrastructureMappingsForApp(String app) {
    logger.info("Getting infra mappings for application: " + app);
    PageRequest<InfrastructureMapping> pageRequest = new PageRequest<>();
    pageRequest.addFilter(APP_ID_KEY, SearchFilter.Operator.EQ, app);
    pageRequest.addFilter(INFRA_MAPPING_TYPE_KEY, SearchFilter.Operator.EQ, PCF_PCF);
    PageResponse<InfrastructureMapping> response = infrastructureMappingService.list(pageRequest);
    return response.getResponse();
  }
}
