package software.wings.service.impl.analysis;

import static io.harness.data.structure.HasPredicate.hasNone;

import software.wings.metrics.RiskLevel;
import software.wings.sm.StateType;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;

/**
 * Created by rsingh on 6/30/17.
 */

@Data
@Builder
public class LogMLAnalysisSummary {
  private String query;
  private RiskLevel riskLevel;
  private String analysisSummaryMessage;
  private String baseLineExecutionId;
  private double score;
  private int highRiskClusters;
  private int mediumRiskClusters;
  private int lowRiskClusters;
  @Default private List<LogMLClusterSummary> controlClusters = new ArrayList<>();
  @Default private List<LogMLClusterSummary> testClusters = new ArrayList<>();
  @Default private List<LogMLClusterSummary> unknownClusters = new ArrayList<>();
  @Default private List<LogMLClusterSummary> ignoreClusters = new ArrayList<>();
  private StateType stateType;
  private AnalysisComparisonStrategy analysisComparisonStrategy;
  private int analysisMinute;
  private int progress;
  private int timeDuration;
  private Set<String> newVersionNodes;
  private Set<String> previousVersionNodes;
  private long baselineStartTime;
  private long baselineEndTime;

  public boolean isEmptyResult() {
    return hasNone(testClusters) && hasNone(unknownClusters) && hasNone(controlClusters) && hasNone(ignoreClusters);
  }
}
