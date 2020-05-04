package io.harness.ccm;

import static io.harness.ccm.cluster.entities.ClusterType.DIRECT_KUBERNETES;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;
import static software.wings.beans.FeatureName.SIDE_NAVIGATION;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.ccm.cluster.ClusterRecordObserver;
import io.harness.ccm.cluster.ClusterRecordService;
import io.harness.ccm.cluster.entities.ClusterRecord;
import io.harness.ccm.config.CCMSettingService;
import io.harness.logging.AutoLogContext;
import io.harness.persistence.AccountLogContext;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.Account;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.account.AccountCrudObserver;

import java.util.List;

@Slf4j
@Singleton
public class CEPerpetualTaskHandler implements AccountCrudObserver, ClusterRecordObserver {
  private CCMSettingService ccmSettingService;
  private CEPerpetualTaskManager cePerpetualTaskManager;
  private ClusterRecordService clusterRecordService;
  private FeatureFlagService featureFlagService;

  @Inject
  public CEPerpetualTaskHandler(CCMSettingService ccmSettingService, CEPerpetualTaskManager cePerpetualTaskManager,
      ClusterRecordService clusterRecordService, FeatureFlagService featureFlagService) {
    this.ccmSettingService = ccmSettingService;
    this.cePerpetualTaskManager = cePerpetualTaskManager;
    this.clusterRecordService = clusterRecordService;
    this.featureFlagService = featureFlagService;
  }

  @Override
  public void onAccountCreated(Account account) {
    onAccountUpdated(account);
  }

  @Override
  public void onAccountUpdated(Account account) {
    try (AutoLogContext ignore0 = new AccountLogContext(account.getUuid(), OVERRIDE_ERROR)) {
      if (account.isCeAutoCollectK8sEvents()) {
        cePerpetualTaskManager.createPerpetualTasks(account, DIRECT_KUBERNETES);
        logger.info("Created perpetual tasks for all clusters with CE support under account id={}.", account.getUuid());
      } else if (!account.isCloudCostEnabled()) {
        cePerpetualTaskManager.deletePerpetualTasks(account, null);
        logger.info("Deleted perpetual tasks for all clusters with CE support under account id={}.", account.getUuid());
      } else { // account.isCloudCostEnabled() and !account.isCeAutoCollectK8sEvents())
        List<ClusterRecord> clusterRecords = clusterRecordService.list(account.getUuid(), DIRECT_KUBERNETES);
        clusterRecords.forEach(clusterRecord -> {
          if (ccmSettingService.isCloudCostEnabled(clusterRecord)) {
            cePerpetualTaskManager.createPerpetualTasks(clusterRecord);
          } else {
            cePerpetualTaskManager.deletePerpetualTasks(clusterRecord);
          }
        });
      }

      if (account.isCloudCostEnabled()) {
        featureFlagService.enableAccount(SIDE_NAVIGATION, account.getUuid());
      }
    }
  }

  @Override
  public boolean onUpserted(ClusterRecord clusterRecord) {
    if (isEmpty(clusterRecord.getPerpetualTaskIds())) {
      if (!clusterRecord.getCluster().getClusterType().equals(DIRECT_KUBERNETES)
          && ccmSettingService.isCloudCostEnabled(clusterRecord)) {
        cePerpetualTaskManager.createPerpetualTasks(clusterRecord);
      }
    }
    return true;
  }

  @Override
  public void onDeleting(ClusterRecord clusterRecord) {
    cePerpetualTaskManager.deletePerpetualTasks(clusterRecord);
  }

  @Override
  public void onDeactivating(ClusterRecord clusterRecord) {
    cePerpetualTaskManager.deletePerpetualTasks(clusterRecord);
  }
}
