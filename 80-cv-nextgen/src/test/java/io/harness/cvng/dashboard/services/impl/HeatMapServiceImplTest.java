package io.harness.cvng.dashboard.services.impl;

import static io.harness.cvng.core.services.CVNextGenConstants.CV_ANALYSIS_WINDOW_MINUTES;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.persistence.HQuery.excludeAuthority;
import static io.harness.rule.OwnerRule.RAGHU;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.cvng.CVNextGenBaseTest;
import io.harness.cvng.core.beans.CVMonitoringCategory;
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
              - Math.floorMod(
                    instant.toEpochMilli(), TimeUnit.MINUTES.toMillis(heatMapResolution.getBucketSizeMinutes()))));
      Set<HeatMapRisk> heatMapRisks = heatMap.getHeatMapRisks();
      assertThat(heatMapRisks.size()).isEqualTo(1);
      HeatMapRisk heatMapRisk = heatMapRisks.iterator().next();
      assertThat(heatMapRisk.getTimeStamp())
          .isEqualTo(Instant.ofEpochMilli(instant.toEpochMilli()
              - Math.floorMod(
                    instant.toEpochMilli(), TimeUnit.MINUTES.toMillis(heatMapResolution.getResolutionMinutes()))));
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
          .isEqualTo(
              (int) Math.ceil(numOfUnits * CV_ANALYSIS_WINDOW_MINUTES / heatMapResolution.getBucketSizeMinutes()));
      for (int j = 0; j < heatMaps.size(); j++) {
        HeatMap heatMap = heatMaps.get(j);
        assertThat(heatMap.getHeatMapResolution()).isEqualTo(heatMapResolution);
        assertThat(heatMap.getHeatMapBucketStartTime())
            .isEqualTo(Instant.ofEpochMilli(TimeUnit.MINUTES.toMillis(j * heatMapResolution.getBucketSizeMinutes())));
        SortedSet<HeatMapRisk> heatMapRisks = new TreeSet<>(heatMap.getHeatMapRisks());
        AtomicLong timeStamp = new AtomicLong(TimeUnit.MINUTES.toMillis(j * heatMapResolution.getBucketSizeMinutes()));
        heatMapRisks.forEach(heatMapRisk -> {
          assertThat(heatMapRisk.getTimeStamp()).isEqualTo(Instant.ofEpochMilli(timeStamp.get()));
          timeStamp.addAndGet(TimeUnit.MINUTES.toMillis(heatMapResolution.getResolutionMinutes()));
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
          - Math.floorMod(
                updateInstant.toEpochMilli(), TimeUnit.MINUTES.toMillis(heatMapResolution.getBucketSizeMinutes())));
      List<HeatMap> heatMaps = hPersistence.createQuery(HeatMap.class, excludeAuthority)
                                   .filter(HeatMapKeys.heatMapResolution, heatMapResolution)
                                   .filter(HeatMapKeys.heatMapBucketStartTime, bucketBoundary)
                                   .asList();
      assertThat(heatMaps.size()).isEqualTo(1);
      HeatMap heatMap = heatMaps.get(0);
      Instant heatMapTimeStamp = Instant.ofEpochMilli(updateInstant.toEpochMilli()
          - Math.floorMod(
                updateInstant.toEpochMilli(), TimeUnit.MINUTES.toMillis(heatMapResolution.getResolutionMinutes())));

      AtomicBoolean verified = new AtomicBoolean(false);
      heatMap.getHeatMapRisks().forEach(heatMapRisk -> {
        if (heatMapRisk.getTimeStamp().equals(heatMapTimeStamp)) {
          verified.set(true);
          assertThat(heatMapRisk.getRiskScore()).isEqualTo(0.7, offset(0.0001));
        } else {
          assertThat(heatMapRisk.getRiskScore()).isEqualTo(0.6, offset(0.0001));
        }
      });
      assertThat(verified.get()).isTrue();
    }
  }
}
