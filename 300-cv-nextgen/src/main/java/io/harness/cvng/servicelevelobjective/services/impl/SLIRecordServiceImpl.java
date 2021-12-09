package io.harness.cvng.servicelevelobjective.services.impl;

import io.harness.cvng.servicelevelobjective.beans.SLODashboardWidget;
import io.harness.cvng.servicelevelobjective.entities.SLIRecord;
import io.harness.cvng.servicelevelobjective.entities.SLIRecord.SLIRecordKeys;
import io.harness.cvng.servicelevelobjective.entities.SLIRecord.SLIRecordParam;
import io.harness.cvng.servicelevelobjective.entities.SLIRecord.SLIState;
import io.harness.cvng.servicelevelobjective.services.api.SLIRecordService;
import io.harness.persistence.HPersistence;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.mongodb.morphia.query.Sort;

public class SLIRecordServiceImpl implements SLIRecordService {
  @Inject private HPersistence hPersistence;

  @Override
  public void create(SLIRecord sliRecord) {
    hPersistence.save(sliRecord);
  }
  @Override
  public List<SLODashboardWidget.Point> sliPerformanceTrend(String sliId, Instant startTime, Instant endTime) {
    return sliPerformanceTrend(sliId, startTime, endTime, Duration.ofMinutes(1));
  }

  @VisibleForTesting
  List<SLODashboardWidget.Point> sliPerformanceTrend(
      String sliId, Instant startTime, Instant endTime, Duration rollUpDuration) {
    List<SLIRecord> sliRecords = sliRecords(sliId, startTime, endTime, rollUpDuration);
    List<SLODashboardWidget.Point> sliTread = new ArrayList<>();
    if (!sliRecords.isEmpty()) {
      long beginningMinute = sliRecords.get(0).getEpochMinute();
      for (int i = 0; i < sliRecords.size(); i++) {
        long goodCountFromStart = sliRecords.get(i).getRunningGoodCount() - sliRecords.get(0).getRunningGoodCount();
        long badCountFromStart = sliRecords.get(i).getRunningBadCount() - sliRecords.get(0).getRunningBadCount();
        long minutesFromStart = sliRecords.get(i).getEpochMinute() - beginningMinute + 1;
        long deltaMissingData = minutesFromStart - (goodCountFromStart + badCountFromStart);
        // TODO: change missing data interpretation based on user input
        double percentageSLIValue = ((goodCountFromStart + deltaMissingData) * 100) / (double) minutesFromStart;
        sliTread.add(SLODashboardWidget.Point.builder()
                         .timestamp(sliRecords.get(i).getTimestamp().toEpochMilli())
                         .value(percentageSLIValue)
                         .build());
      }
    }
    return sliTread;
  }

  @Override
  public void create(List<SLIRecordParam> sliRecordParamList, String sliId, String verificationTaskId) {
    List<SLIRecord> sliRecordList = new ArrayList<>();
    SLIRecord lastSLIRecord = getLastNonMissingSliRecord(sliRecordParamList.get(0).getTimeStamp());
    long runningGoodCount = 0L;
    long runningBadCount = 0L;
    if (Objects.nonNull(lastSLIRecord)) {
      runningGoodCount = lastSLIRecord.getRunningGoodCount();
      runningBadCount = lastSLIRecord.getRunningBadCount();
    }
    for (SLIRecordParam sliRecordParam : sliRecordParamList) {
      if (SLIState.GOOD.equals(sliRecordParam.getSliState())) {
        runningGoodCount++;
      } else if (SLIState.BAD.equals(sliRecordParam.getSliState())) {
        runningBadCount++;
      }
      SLIRecord sliRecord = SLIRecord.builder()
                                .runningBadCount(runningBadCount)
                                .runningGoodCount(runningGoodCount)
                                .sliId(sliId)
                                .verificationTaskId(verificationTaskId)
                                .timestamp(sliRecordParam.getTimeStamp())
                                .sliState(sliRecordParam.getSliState())
                                .build();
      sliRecordList.add(sliRecord);
    }
    hPersistence.save(sliRecordList);
  }

  private SLIRecord getLastNonMissingSliRecord(Instant startTimeStamp) {
    return hPersistence.createQuery(SLIRecord.class)
        .field(SLIRecordKeys.timestamp)
        .lessThan(startTimeStamp)
        .field(SLIRecordKeys.sliState)
        .in(Arrays.asList(SLIState.GOOD, SLIState.BAD))
        .get();
  }

  private List<SLIRecord> sliRecords(String sliId, Instant startTime, Instant endTime, Duration duration) {
    return hPersistence.createQuery(SLIRecord.class)
        .filter(SLIRecordKeys.sliId, sliId)
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
