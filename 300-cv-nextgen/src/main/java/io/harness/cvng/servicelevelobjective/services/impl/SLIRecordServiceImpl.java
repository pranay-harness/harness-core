package io.harness.cvng.servicelevelobjective.services.impl;

import io.harness.cvng.servicelevelobjective.beans.SLODashboardWidget;
import io.harness.cvng.servicelevelobjective.entities.SLIRecord;
import io.harness.cvng.servicelevelobjective.entities.SLIRecord.SLIRecordKeys;
import io.harness.cvng.servicelevelobjective.services.api.SLIRecordService;
import io.harness.persistence.HPersistence;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.mongodb.morphia.query.Sort;

public class SLIRecordServiceImpl implements SLIRecordService {
  @Inject private HPersistence hPersistence;
  @Override
  public void create(SLIRecord sliRecord) {
    hPersistence.save(sliRecord);
  }

  @VisibleForTesting
  List<SLODashboardWidget.Point> sliPerformanceTread(
      String verificationTaskId, Instant startTime, Instant endTime, Duration rollUpDuration) {
    List<SLIRecord> sliRecords = sliRecords(verificationTaskId, startTime, endTime, rollUpDuration);

    List<SLODashboardWidget.Point> sliTread = new ArrayList<>();
    int goodCount = 0;
    long beginningMinute = sliRecords.get(0).getEpochMinute();
    for (int i = 1; i < sliRecords.size(); i++) {
      long deltaBad = sliRecords.get(i).getRunningBadCount() - sliRecords.get(i - 1).getRunningBadCount();
      long deltaGood = sliRecords.get(i).getRunningGoodCount() - sliRecords.get(i - 1).getRunningGoodCount();
      long minutesFromStart = sliRecords.get(i).getEpochMinute() - beginningMinute + 1;
      long deltaMissingData = minutesFromStart - (deltaBad + deltaGood);
      // TODO: change missing data interpretation based on user input
      double percentageSLIValue = ((goodCount + deltaMissingData) * 100) / (double) minutesFromStart;
      sliTread.add(SLODashboardWidget.Point.builder()
                       .timestamp(sliRecords.get(i).getTimestamp().toEpochMilli())
                       .value(percentageSLIValue)
                       .build());
    }
    return sliTread;
  }

  private List<SLIRecord> sliRecords(String verificationTaskId, Instant startTime, Instant endTime, Duration duration) {
    return hPersistence.createQuery(SLIRecord.class)
        .filter(SLIRecordKeys.verificationTaskId, verificationTaskId)
        .field(SLIRecordKeys.timestamp)
        .greaterThanOrEq(startTime)
        .field(SLIRecordKeys.timestamp)
        .lessThan(endTime)
        .field(SLIRecordKeys.epochMinute)
        .mod(duration.toMinutes(), 0)
        .order(Sort.ascending(SLIRecordKeys.timestamp))
        .asList();
  }
}
