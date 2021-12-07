package io.harness.cvng.servicelevelobjective.services.impl;

import static io.harness.cvng.servicelevelobjective.entities.SLIRecord.SLIState.BAD;
import static io.harness.cvng.servicelevelobjective.entities.SLIRecord.SLIState.GOOD;
import static io.harness.cvng.servicelevelobjective.entities.SLIRecord.SLIState.NO_DATA;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.KAMAL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

import io.harness.CvNextGenTestBase;
import io.harness.category.element.UnitTests;
import io.harness.cvng.core.utils.DateTimeUtils;
import io.harness.cvng.servicelevelobjective.beans.SLODashboardWidget.Point;
import io.harness.cvng.servicelevelobjective.entities.SLIRecord;
import io.harness.cvng.servicelevelobjective.entities.SLIRecord.SLIState;
import io.harness.rule.Owner;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class SLIRecordServiceImplTest extends CvNextGenTestBase {
  @Inject private SLIRecordServiceImpl sliRecordService;
  @Inject private Clock clock;

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testSliPerformanceTrend_noData() {
    assertThat(
        sliRecordService.sliPerformanceTrend(generateUuid(), clock.instant(), clock.instant(), Duration.ofMinutes(5)))
        .isEmpty();
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testSliPerformanceTrend_withAllStates() {
    String verificationTaskId = generateUuid();
    String sliId = generateUuid();
    Instant startTime = Instant.parse("2020-07-27T10:50:00Z").minus(Duration.ofMinutes(10));
    List<SLIState> sliStates = Arrays.asList(BAD, GOOD, GOOD, NO_DATA, GOOD, GOOD, BAD, BAD, BAD, BAD);
    createData(verificationTaskId, sliId, startTime, sliStates);
    assertThat(sliRecordService.sliPerformanceTrend(
                   sliId, startTime, startTime.plus(Duration.ofMinutes(11)), Duration.ofMinutes(2)))
        .hasSize(5);
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testSliPerformanceTrend_test2() {
    String verificationTaskId = generateUuid();
    String sliId = generateUuid();
    Instant startTime = DateTimeUtils.roundDownTo1MinBoundary(clock.instant().minus(Duration.ofMinutes(4)));
    List<SLIState> sliStates = Arrays.asList(GOOD, NO_DATA, BAD, GOOD);
    createData(verificationTaskId, sliId, startTime, sliStates);
    List<Point> sliTrends =
        sliRecordService.sliPerformanceTrend(sliId, startTime, startTime.plus(Duration.ofMinutes(5)));
    List<Double> expected = Lists.newArrayList(100.0, 100.0, 66.66, 75.0);
    assertThat(sliTrends).hasSize(4);
    for (int i = 0; i < expected.size(); i++) {
      assertThat(sliTrends.get(i).getTimestamp()).isEqualTo(startTime.plus(Duration.ofMinutes(i)).toEpochMilli());
      assertThat(sliTrends.get(i).getValue()).isCloseTo(expected.get(i), offset(0.01));
    }
  }

  private void createData(String verificationTaskId, String sliId, Instant startTime, List<SLIState> sliStates) {
    int runningGoodCount = 10;
    int runningBadCount = 5;
    for (int i = 0; i < sliStates.size(); i++) {
      SLIState sliState = sliStates.get(i);
      if (sliState == GOOD) {
        runningGoodCount++;
      } else if (sliState == BAD) {
        runningBadCount++;
      }
      sliRecordService.create(SLIRecord.builder()
                                  .verificationTaskId(verificationTaskId)
                                  .sliId(sliId)
                                  .runningGoodCount(runningGoodCount)
                                  .runningBadCount(runningBadCount)
                                  .timestamp(startTime.plus(Duration.ofMinutes(i)))
                                  .sliState(sliState)
                                  .build());
    }
  }
}