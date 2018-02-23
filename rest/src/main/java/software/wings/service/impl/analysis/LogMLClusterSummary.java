package software.wings.service.impl.analysis;

import lombok.Data;
import software.wings.metrics.RiskLevel;

import java.util.List;
import java.util.Map;

/**
 * Created by rsingh on 6/30/17.
 */

@Data
public class LogMLClusterSummary {
  private Map<String, LogMLHostSummary> hostSummary;
  private String logText;
  private List<String> tags;
  private double score;
  private RiskLevel riskLevel;
  private int clusterLabel;
  private AnalysisServiceImpl.LogMLFeedbackType logMLFeedbackType;
  private String logMLFeedbackId;
}
