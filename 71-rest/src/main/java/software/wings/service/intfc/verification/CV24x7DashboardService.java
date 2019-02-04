package software.wings.service.intfc.verification;

import software.wings.service.impl.analysis.LogMLAnalysisSummary;
import software.wings.verification.HeatMap;

import java.util.List;

public interface CV24x7DashboardService {
  List<HeatMap> getHeatMapForLogs(
      String accountId, String appId, String serviceId, long startTime, long endTime, boolean detailed);
  LogMLAnalysisSummary getAnalysisSummary(String cvConfigId, long startTime, long endTime, String appId);
}
