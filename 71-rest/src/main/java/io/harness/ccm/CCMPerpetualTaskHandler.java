package io.harness.ccm;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.ccm.cluster.ClusterRecordObserver;
import io.harness.ccm.cluster.ClusterRecordService;
import io.harness.ccm.cluster.entities.ClusterRecord;
import io.harness.ccm.config.CCMSettingService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class CCMPerpetualTaskHandler implements ClusterRecordObserver {
  private CCMSettingService ccmSettingService;
  private CCMPerpetualTaskManager ccmPerpetualTaskManager;

  @Inject
  public CCMPerpetualTaskHandler(ClusterRecordService clusterRecordService, CCMSettingService ccmSettingService,
      CCMPerpetualTaskManager ccmPerpetualTaskManager) {
    this.ccmSettingService = ccmSettingService;
    this.ccmPerpetualTaskManager = ccmPerpetualTaskManager;
  }

  @Override
  public boolean onUpserted(ClusterRecord clusterRecord) {
    if (ccmSettingService.isCloudCostEnabled(clusterRecord)) {
      if (isEmpty(clusterRecord.getPerpetualTaskIds())) {
        ccmPerpetualTaskManager.createPerpetualTasks(clusterRecord);
      }
    }
    return true;
  }

  @Override
  public void onDeleting(ClusterRecord clusterRecord) {
    ccmPerpetualTaskManager.deletePerpetualTasks(clusterRecord);
  }

  @Override
  public void onDeactivating(ClusterRecord clusterRecord) {
    ccmPerpetualTaskManager.deletePerpetualTasks(clusterRecord);
  }
}
