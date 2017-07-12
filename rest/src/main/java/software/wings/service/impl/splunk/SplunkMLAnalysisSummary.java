package software.wings.service.impl.splunk;

import lombok.Data;
import software.wings.metrics.RiskLevel;

import java.util.List;

/**
 * Created by rsingh on 6/30/17.
 */

@Data
public class SplunkMLAnalysisSummary {
  private String query;
  private RiskLevel riskLevel;
  private String analysisSummaryMessage;
  private List<SplunkMLClusterSummary> controlClusters;
  private List<SplunkMLClusterSummary> testClusters;
  private List<SplunkMLClusterSummary> unknownClusters;
}
