package io.harness.ccm;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.ccm.cluster.ClusterRecordObserver;
import io.harness.ccm.cluster.ClusterRecordService;
import io.harness.ccm.cluster.entities.ClusterRecord;
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
      ccmPerpetualTaskManager.createPerpetualTasks(clusterRecord);
    }
    return true;
  }

  @Override
  public void onDeleting(ClusterRecord clusterRecord) {
    ccmPerpetualTaskManager.deletePerpetualTasks(clusterRecord);
  }
}
