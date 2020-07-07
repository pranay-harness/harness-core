package io.harness.cvng.dashboard.services.impl;

import static io.harness.cvng.core.services.CVNextGenConstants.CV_ANALYSIS_WINDOW_MINUTES;
import static io.harness.cvng.dashboard.entities.HeatMap.HeatMapResolution.FIFTEEN_MINUTES;
import static io.harness.cvng.dashboard.entities.HeatMap.HeatMapResolution.FIVE_MIN;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.persistence.HQuery.excludeAuthority;
import static io.harness.rule.OwnerRule.RAGHU;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.cvng.CVNextGenBaseTest;
import io.harness.cvng.core.beans.CVMonitoringCategory;
import io.harness.cvng.core.dashboard.beans.HeatMapDTO;
import io.harness.cvng.dashboard.entities.HeatMap;
import io.harness.cvng.dashboard.entities.HeatMap.HeatMapKeys;
import io.harness.cvng.dashboard.entities.HeatMap.HeatMapResolution;
import io.harness.cvng.dashboard.entities.HeatMap.HeatMapRisk;
import io.harness.cvng.dashboard.services.api.HeatMapService;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.time.Instant;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class HeatMapServiceImplTest extends CVNextGenBaseTest {
  @Inject private HeatMapService heatMapService;

  private String serviceIdentifier;
  private String envIdentifier;
  private String accountId;
  @Inject private HPersistence hPersistence;

  @Before
  public void setUp() {
    serviceIdentifier = generateUuid();
    envIdentifier = generateUuid();
    accountId = generateUuid();
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testUpsertAndUpdate() {
    Instant instant = Instant.now();
    heatMapService.updateRiskScore(
        accountId, serviceIdentifier, envIdentifier, CVMonitoringCategory.PERFORMANCE, instant, 0.6);
    verifyUpdates(instant, 0.6);
    List<HeatMap> heatMaps;
    HeatMap heatMap;
    Set<HeatMapRisk> heatMapRisks;
    HeatMapRisk heatMapRisk;

    // update and test
    heatMapService.updateRiskScore(
        accountId, serviceIdentifier, envIdentifier, CVMonitoringCategory.PERFORMANCE, instant, 0.7);
    verifyUpdates(instant, 0.7);

    // updating with lower risk score shouldn't change anything
    heatMapService.updateRiskScore(
        accountId, serviceIdentifier, envIdentifier, CVMonitoringCategory.PERFORMANCE, instant, 0.5);
    verifyUpdates(instant, 0.7);
  }

  private void verifyUpdates(Instant instant, double riskScore) {
    List<HeatMap> heatMaps = hPersistence.createQuery(HeatMap.class, excludeAuthority).asList();
    assertThat(heatMaps.size()).isEqualTo(HeatMapResolution.values().length);
    for (int i = 0; i < HeatMapResolution.values().length; i++) {
      HeatMapResolution heatMapResolution = HeatMapResolution.values()[i];
      HeatMap heatMap = heatMaps.get(i);
      assertThat(heatMap.getAccountId()).isEqualTo(accountId);
      assertThat(heatMap.getServiceIdentifier()).isEqualTo(serviceIdentifier);
      assertThat(heatMap.getEnvIdentifier()).isEqualTo(envIdentifier);
      assertThat(heatMap.getCategory()).isEqualTo(CVMonitoringCategory.PERFORMANCE);
      assertThat(heatMap.getHeatMapResolution()).isEqualTo(heatMapResolution);
      assertThat(heatMap.getHeatMapBucketStartTime())
          .isEqualTo(Instant.ofEpochMilli(instant.toEpochMilli()
              - Math.floorMod(instant.toEpochMilli(), heatMapResolution.getBucketSize().toMillis())));
      assertThat(heatMap.getHeatMapBucketEndTime())
          .isEqualTo(heatMap.getHeatMapBucketStartTime().plusMillis(heatMapResolution.getBucketSize().toMillis() - 1));
      Set<HeatMapRisk> heatMapRisks = heatMap.getHeatMapRisks();
      assertThat(heatMapRisks.size()).isEqualTo(1);
      HeatMapRisk heatMapRisk = heatMapRisks.iterator().next();
      assertThat(heatMapRisk.getStartTime())
          .isEqualTo(Instant.ofEpochMilli(instant.toEpochMilli()
              - Math.floorMod(instant.toEpochMilli(), heatMapResolution.getResolution().toMillis())));
      assertThat(heatMapRisk.getEndTime())
          .isEqualTo(heatMapRisk.getStartTime().plusMillis(heatMapResolution.getResolution().toMillis() - 1));
      assertThat(heatMapRisk.getRiskScore()).isEqualTo(riskScore, offset(0.001));
    }
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testUpsert_whenMultipleBoundaries() {
    double numOfUnits = 3000;
    for (int minuteBoundry = 0; minuteBoundry < numOfUnits * CV_ANALYSIS_WINDOW_MINUTES;
         minuteBoundry += CV_ANALYSIS_WINDOW_MINUTES) {
      heatMapService.updateRiskScore(accountId, serviceIdentifier, envIdentifier, CVMonitoringCategory.PERFORMANCE,
          Instant.ofEpochMilli(TimeUnit.MINUTES.toMillis(minuteBoundry)), 0.6);
    }
    for (int i = 0; i < HeatMapResolution.values().length; i++) {
      HeatMapResolution heatMapResolution = HeatMapResolution.values()[i];
      List<HeatMap> heatMaps = hPersistence.createQuery(HeatMap.class, excludeAuthority)
                                   .filter(HeatMapKeys.heatMapResolution, heatMapResolution)
                                   .asList();
      assertThat(heatMaps.size())
          .isEqualTo((int) Math.ceil(numOfUnits * TimeUnit.MINUTES.toMillis(CV_ANALYSIS_WINDOW_MINUTES)
              / heatMapResolution.getBucketSize().toMillis()));
      for (int j = 0; j < heatMaps.size(); j++) {
        HeatMap heatMap = heatMaps.get(j);
        assertThat(heatMap.getHeatMapResolution()).isEqualTo(heatMapResolution);
        assertThat(heatMap.getHeatMapBucketStartTime())
            .isEqualTo(Instant.ofEpochMilli(j * heatMapResolution.getBucketSize().toMillis()));
        SortedSet<HeatMapRisk> heatMapRisks = new TreeSet<>(heatMap.getHeatMapRisks());
        AtomicLong timeStamp = new AtomicLong(j * heatMapResolution.getBucketSize().toMillis());
        heatMapRisks.forEach(heatMapRisk -> {
          assertThat(heatMapRisk.getStartTime()).isEqualTo(Instant.ofEpochMilli(timeStamp.get()));
          timeStamp.addAndGet(heatMapResolution.getResolution().toMillis());
          assertThat(heatMapRisk.getRiskScore()).isEqualTo(0.6, offset(0.001));
        });
      }
    }

    // update a riskscore
    Instant updateInstant = Instant.ofEpochMilli(TimeUnit.MINUTES.toMillis(680));
    heatMapService.updateRiskScore(
        accountId, serviceIdentifier, envIdentifier, CVMonitoringCategory.PERFORMANCE, updateInstant, 0.7);

    for (int i = 0; i < HeatMapResolution.values().length; i++) {
      HeatMapResolution heatMapResolution = HeatMapResolution.values()[i];
      Instant bucketBoundary = Instant.ofEpochMilli(updateInstant.toEpochMilli()
          - Math.floorMod(updateInstant.toEpochMilli(), heatMapResolution.getBucketSize().toMillis()));
      List<HeatMap> heatMaps = hPersistence.createQuery(HeatMap.class, excludeAuthority)
                                   .filter(HeatMapKeys.heatMapResolution, heatMapResolution)
                                   .filter(HeatMapKeys.heatMapBucketStartTime, bucketBoundary)
                                   .asList();
      assertThat(heatMaps.size()).isEqualTo(1);
      HeatMap heatMap = heatMaps.get(0);
      Instant heatMapTimeStamp = Instant.ofEpochMilli(updateInstant.toEpochMilli()
          - Math.floorMod(updateInstant.toEpochMilli(), heatMapResolution.getResolution().toMillis()));

      AtomicBoolean verified = new AtomicBoolean(false);
      heatMap.getHeatMapRisks().forEach(heatMapRisk -> {
        if (heatMapRisk.getStartTime().equals(heatMapTimeStamp)) {
          verified.set(true);
          assertThat(heatMapRisk.getRiskScore()).isEqualTo(0.7, offset(0.0001));
        } else {
          assertThat(heatMapRisk.getRiskScore()).isEqualTo(0.6, offset(0.0001));
        }
      });
      assertThat(verified.get()).isTrue();
    }
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testGetHeatMap_whenFiveMinuteResolution() {
    // no analysis
    int startMin = 5;
    int endMin = 200;
    SortedSet<HeatMapDTO> heatMap = heatMapService.getHeatMap(accountId, serviceIdentifier, envIdentifier,
        CVMonitoringCategory.PERFORMANCE, Instant.ofEpochMilli(TimeUnit.MINUTES.toMillis(startMin)),
        Instant.ofEpochMilli(TimeUnit.MINUTES.toMillis(endMin)));

    assertThat(heatMap.size()).isEqualTo((endMin - startMin) / FIVE_MIN.getResolution().toMinutes() + 1);
    Iterator<HeatMapDTO> heatMapIterator = heatMap.iterator();
    for (long i = startMin; i <= endMin; i += FIVE_MIN.getResolution().toMinutes()) {
      HeatMapDTO heatMapDTO = heatMapIterator.next();
      assertThat(heatMapDTO)
          .isEqualTo(HeatMapDTO.builder()
                         .startTime(TimeUnit.MINUTES.toMillis(i))
                         .endTime(TimeUnit.MINUTES.toMillis(i) + FIVE_MIN.getResolution().toMillis() - 1)
                         .build());
    }
    int numOfRiskUnits = 24;
    int riskStartBoundary = 30;
    for (int minuteBoundry = riskStartBoundary;
         minuteBoundry < riskStartBoundary + numOfRiskUnits * CV_ANALYSIS_WINDOW_MINUTES;
         minuteBoundry += CV_ANALYSIS_WINDOW_MINUTES) {
      heatMapService.updateRiskScore(accountId, serviceIdentifier, envIdentifier, CVMonitoringCategory.PERFORMANCE,
          Instant.ofEpochMilli(TimeUnit.MINUTES.toMillis(minuteBoundry)), 0.01 * minuteBoundry);
    }

    heatMap = heatMapService.getHeatMap(accountId, serviceIdentifier, envIdentifier, CVMonitoringCategory.PERFORMANCE,
        Instant.ofEpochMilli(TimeUnit.MINUTES.toMillis(startMin)),
        Instant.ofEpochMilli(TimeUnit.MINUTES.toMillis(endMin)));

    assertThat(heatMap.size()).isEqualTo((endMin - startMin) / FIVE_MIN.getResolution().toMinutes() + 1);
    heatMapIterator = heatMap.iterator();
    for (long i = startMin; i <= endMin; i += FIVE_MIN.getResolution().toMinutes()) {
      HeatMapDTO heatMapDTO = heatMapIterator.next();
      if (i < riskStartBoundary || i >= riskStartBoundary + numOfRiskUnits * CV_ANALYSIS_WINDOW_MINUTES) {
        assertThat(heatMapDTO)
            .isEqualTo(HeatMapDTO.builder()
                           .startTime(TimeUnit.MINUTES.toMillis(i))
                           .endTime(TimeUnit.MINUTES.toMillis(i) + FIVE_MIN.getResolution().toMillis() - 1)
                           .build());
      } else {
        assertThat(heatMapDTO)
            .isEqualTo(HeatMapDTO.builder()
                           .startTime(TimeUnit.MINUTES.toMillis(i))
                           .endTime(TimeUnit.MINUTES.toMillis(i) + FIVE_MIN.getResolution().toMillis() - 1)
                           .riskScore(i * 0.01)
                           .build());
      }
    }
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testGetHeatMap_whenFifteenMinuteResolution() {
    // no analysis
    int startMin = 75;
    int endMin = 350;
    SortedSet<HeatMapDTO> heatMap = heatMapService.getHeatMap(accountId, serviceIdentifier, envIdentifier,
        CVMonitoringCategory.PERFORMANCE, Instant.ofEpochMilli(TimeUnit.MINUTES.toMillis(startMin)),
        Instant.ofEpochMilli(TimeUnit.MINUTES.toMillis(endMin)));

    assertThat(heatMap.size()).isEqualTo((endMin - startMin) / FIFTEEN_MINUTES.getResolution().toMinutes() + 1);
    Iterator<HeatMapDTO> heatMapIterator = heatMap.iterator();
    for (long i = startMin; i <= endMin; i += FIFTEEN_MINUTES.getResolution().toMinutes()) {
      HeatMapDTO heatMapDTO = heatMapIterator.next();
      assertThat(heatMapDTO)
          .isEqualTo(HeatMapDTO.builder()
                         .startTime(TimeUnit.MINUTES.toMillis(i))
                         .endTime(TimeUnit.MINUTES.toMillis(i) + FIFTEEN_MINUTES.getResolution().toMillis() - 1)
                         .build());
    }
    int numOfRiskUnits = 42;
    int riskStartBoundary = 70;
    for (int minuteBoundry = riskStartBoundary;
         minuteBoundry <= riskStartBoundary + numOfRiskUnits * CV_ANALYSIS_WINDOW_MINUTES;
         minuteBoundry += CV_ANALYSIS_WINDOW_MINUTES) {
      heatMapService.updateRiskScore(accountId, serviceIdentifier, envIdentifier, CVMonitoringCategory.PERFORMANCE,
          Instant.ofEpochMilli(TimeUnit.MINUTES.toMillis(minuteBoundry)), 0.01 * minuteBoundry);
    }

    heatMap = heatMapService.getHeatMap(accountId, serviceIdentifier, envIdentifier, CVMonitoringCategory.PERFORMANCE,
        Instant.ofEpochMilli(TimeUnit.MINUTES.toMillis(startMin)),
        Instant.ofEpochMilli(TimeUnit.MINUTES.toMillis(endMin)));

    assertThat(heatMap.size()).isEqualTo((endMin - startMin) / FIFTEEN_MINUTES.getResolution().toMinutes() + 1);
    heatMapIterator = heatMap.iterator();
    for (long i = startMin; i <= endMin; i += FIFTEEN_MINUTES.getResolution().toMinutes()) {
      HeatMapDTO heatMapDTO = heatMapIterator.next();
      if (i < riskStartBoundary
          || i >= riskStartBoundary + (numOfRiskUnits / 3) * FIFTEEN_MINUTES.getResolution().toMinutes()) {
        assertThat(heatMapDTO)
            .isEqualTo(HeatMapDTO.builder()
                           .startTime(TimeUnit.MINUTES.toMillis(i))
                           .endTime(TimeUnit.MINUTES.toMillis(i) + FIFTEEN_MINUTES.getResolution().toMillis() - 1)
                           .build());
      } else {
        assertThat(heatMapDTO)
            .isEqualTo(HeatMapDTO.builder()
                           .startTime(TimeUnit.MINUTES.toMillis(i))
                           .endTime(TimeUnit.MINUTES.toMillis(i) + FIFTEEN_MINUTES.getResolution().toMillis() - 1)
                           .riskScore((i + 10) * 0.01)
                           .build());
      }
    }
  }
}
