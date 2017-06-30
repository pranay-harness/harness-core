package software.wings.service.impl.splunk;

import lombok.Data;

import java.util.List;

/**
 * Created by rsingh on 6/30/17.
 */

@Data
public class SplunkMLAnalysisSummary {
  private List<SplunkMLClusterSummary> controlClusters;
  private List<SplunkMLClusterSummary> testClusters;
  private List<SplunkMLClusterSummary> unknownClusters;
}
