package software.wings.service.impl.analysis;

import lombok.Data;
import software.wings.metrics.RiskLevel;

import java.util.List;

/**
 * Created by rsingh on 6/30/17.
 */

@Data
public class LogMLAnalysisSummary {
  private String query;
  private RiskLevel riskLevel;
  private String analysisSummaryMessage;
  private List<LogMLClusterSummary> controlClusters;
  private List<LogMLClusterSummary> testClusters;
  private List<LogMLClusterSummary> unknownClusters;
}
