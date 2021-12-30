package io.harness.cvng.core.utils.dataCollection;

import io.harness.cvng.core.entities.AnalysisInfo;
import io.harness.cvng.core.entities.VerificationTask.TaskType;

import lombok.experimental.UtilityClass;

@UtilityClass
public class MetricDataCollectionUtils {
  public static boolean isMetricApplicableForDataCollection(AnalysisInfo analysisInfo, TaskType taskType) {
    switch (taskType) {
      case SLI:
        return analysisInfo.getSli().isEnabled();
      case DEPLOYMENT:
        return analysisInfo.getDeploymentVerification().isEnabled();
      case LIVE_MONITORING:
        return analysisInfo.getLiveMonitoring().isEnabled();
      default:
        throw new IllegalStateException("TaskType:" + taskType + " not supported for metric dataCollection");
    }
  }
}
