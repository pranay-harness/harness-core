package software.wings.service.impl.analysis;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import lombok.Data;
import software.wings.metrics.RiskLevel;
import software.wings.sm.StateType;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by rsingh on 6/30/17.
 */

@Data
public class LogMLAnalysisSummary {
  private String query;
  private RiskLevel riskLevel;
  private String analysisSummaryMessage;
  private String baseLineExecutionId;
  private double score;
  private int highRiskClusters;
  private int mediumRiskClusters;
  private int lowRiskClusters;
  private List<LogMLClusterSummary> controlClusters;
  private List<LogMLClusterSummary> testClusters;
  private List<LogMLClusterSummary> unknownClusters;
  private List<LogMLClusterSummary> ignoreClusters;
  private StateType stateType;
  private int analysisMinute;

  public boolean isEmptyResult() {
    return isEmpty(testClusters) && isEmpty(unknownClusters);
  }
  public void initializeClusters() {
    controlClusters = new ArrayList<>();
    testClusters = new ArrayList<>();
    unknownClusters = new ArrayList<>();
    ignoreClusters = new ArrayList<>();
  }
}
