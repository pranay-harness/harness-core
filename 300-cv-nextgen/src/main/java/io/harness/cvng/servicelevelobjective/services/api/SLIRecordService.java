package io.harness.cvng.servicelevelobjective.services.api;

import io.harness.cvng.servicelevelobjective.beans.SLODashboardWidget;
import io.harness.cvng.servicelevelobjective.entities.SLIRecord;
import io.harness.cvng.servicelevelobjective.entities.SLIRecord.SLIRecordParam;

import java.time.Instant;
import java.util.List;

public interface SLIRecordService {
  void create(SLIRecord sliRecord);
  void create(List<SLIRecordParam> sliRecordList, String sliId, String verificationTaskId);
  List<SLODashboardWidget.Point> sliPerformanceTrend(String sliId, Instant startTime, Instant endTime);
}
