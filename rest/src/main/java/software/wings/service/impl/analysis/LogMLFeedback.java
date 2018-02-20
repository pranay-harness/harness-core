package software.wings.service.impl.analysis;

import lombok.Data;

@Data
public class LogMLFeedback {
  private String appId;
  private String stateExecutionId;
  private String text;
  private AnalysisServiceImpl.CLUSTER_TYPE clusterType;
  private int clusterLabel;
  private AnalysisServiceImpl.LogMLFeedbackType logMLFeedbackType;
  private String comment;
}
