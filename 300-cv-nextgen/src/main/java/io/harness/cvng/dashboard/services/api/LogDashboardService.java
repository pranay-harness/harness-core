package io.harness.cvng.dashboard.services.api;

import io.harness.beans.NGPageResponse;
import io.harness.cvng.beans.CVMonitoringCategory;
import io.harness.cvng.dashboard.beans.AnalyzedLogDataDTO;
import io.harness.cvng.dashboard.beans.LogDataByTag;

import java.util.List;

public interface LogDashboardService {
  NGPageResponse<AnalyzedLogDataDTO> getAnomalousLogs(String accountId, String projectIdentifier, String orgIdentifier,
      String serviceIdentifier, String environmentIdentifer, CVMonitoringCategory category, long startTimeMillis,
      long endTimeMillis, int page, int size);
  NGPageResponse<AnalyzedLogDataDTO> getAllLogs(String accountId, String projectIdentifier, String orgIdentifier,
      String serviceIdentifier, String environmentIdentifer, CVMonitoringCategory category, long startTimeMillis,
      long endTimeMillis, int page, int size);

  List<LogDataByTag> getLogCountByTag(String accountId, String projectIdentifier, String orgIdentifier,
      String serviceIdentifier, String environmentIdentifer, CVMonitoringCategory category, long startTimeMillis,
      long endTimeMillis);
}
