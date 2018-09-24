package software.wings.service.impl.analysis;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LogMLFeedback {
  private String appId;
  private String stateExecutionId;
  private AnalysisServiceImpl.CLUSTER_TYPE clusterType;
  private int clusterLabel;
  private AnalysisServiceImpl.LogMLFeedbackType logMLFeedbackType;
  private String comment;
  private String logMLFeedbackId;
}
