package io.harness.cvng.core.services.impl;

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
import io.harness.cvng.core.dashboard.entities.HeatMap;
import io.harness.cvng.core.dashboard.entities.HeatMap.HeatMapRisk;
import io.harness.cvng.core.dashboard.services.api.HeatMapService;
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
    long currentTimeMillis = System.currentTimeMillis();
    heatMapService.updateRiskScore(
        accountId, serviceIdentifier, envIdentifier, CVMonitoringCategory.PERFORMANCE, currentTimeMillis, 0.6);
    verifyUpdates(currentTimeMillis, 0.6);
    List<HeatMap> heatMaps;
    HeatMap heatMap;
    Set<HeatMapRisk> heatMapRisks;
    HeatMapRisk heatMapRisk;

    // update and test
    heatMapService.updateRiskScore(
        accountId, serviceIdentifier, envIdentifier, CVMonitoringCategory.PERFORMANCE, currentTimeMillis, 0.7);
    verifyUpdates(currentTimeMillis, 0.7);

    // updating with lower risk score shouldn't change anything
    heatMapService.updateRiskScore(
        accountId, serviceIdentifier, envIdentifier, CVMonitoringCategory.PERFORMANCE, currentTimeMillis, 0.5);
    verifyUpdates(currentTimeMillis, 0.7);
  }

  private void verifyUpdates(long currentTimeMillis, double riskScore) {
    List<HeatMap> heatMaps = hPersistence.createQuery(HeatMap.class, excludeAuthority).asList();
    assertThat(heatMaps.size()).isEqualTo(1);
    HeatMap heatMap = heatMaps.get(0);
    assertThat(heatMap.getAccountId()).isEqualTo(accountId);
    assertThat(heatMap.getServiceIdentifier()).isEqualTo(serviceIdentifier);
    assertThat(heatMap.getEnvIdentifier()).isEqualTo(envIdentifier);
    assertThat(heatMap.getCategory()).isEqualTo(CVMonitoringCategory.PERFORMANCE);
    assertThat(heatMap.getHeatMapBucketStartTime())
        .isEqualTo(
            Instant.ofEpochMilli(currentTimeMillis - Math.floorMod(currentTimeMillis, TimeUnit.HOURS.toMillis(4))));
    Set<HeatMapRisk> heatMapRisks = heatMap.getHeatMapRisks();
    assertThat(heatMapRisks.size()).isEqualTo(1);
    HeatMapRisk heatMapRisk = heatMapRisks.iterator().next();
    assertThat(heatMapRisk.getTimeStamp()).isEqualTo(Instant.ofEpochMilli(currentTimeMillis));
    assertThat(heatMapRisk.getRiskScore()).isEqualTo(riskScore, offset(0.001));
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testUpsert_whenMultipleBoundaries() {
    int numOfUnits = 100;
    for (int minuteBoundry = 0; minuteBoundry < numOfUnits * CV_ANALYSIS_WINDOW_MINUTES;
         minuteBoundry += CV_ANALYSIS_WINDOW_MINUTES) {
      heatMapService.updateRiskScore(accountId, serviceIdentifier, envIdentifier, CVMonitoringCategory.PERFORMANCE,
          TimeUnit.MINUTES.toMillis(minuteBoundry), 0.6);
    }
    List<HeatMap> heatMaps = hPersistence.createQuery(HeatMap.class, excludeAuthority).asList();
    assertThat(heatMaps.size()).isEqualTo(3);

    AtomicLong timeStamp = new AtomicLong(0);
    HeatMap heatMap = heatMaps.get(0);
    assertThat(heatMap.getHeatMapBucketStartTime()).isEqualTo(Instant.ofEpochMilli(timeStamp.get()));
    assertThat(heatMap.getHeatMapRisks().size()).isEqualTo(TimeUnit.HOURS.toMinutes(4) / CV_ANALYSIS_WINDOW_MINUTES);

    SortedSet<HeatMapRisk> heatMapRisks = new TreeSet<>(heatMap.getHeatMapRisks());
    heatMapRisks.forEach(heatMapRisk -> {
      assertThat(heatMapRisk.getTimeStamp()).isEqualTo(Instant.ofEpochMilli(timeStamp.get()));
      timeStamp.addAndGet(TimeUnit.MINUTES.toMillis(CV_ANALYSIS_WINDOW_MINUTES));
    });
    heatMap = heatMaps.get(1);
    assertThat(heatMap.getHeatMapBucketStartTime()).isEqualTo(Instant.ofEpochMilli(timeStamp.get()));
    assertThat(heatMap.getHeatMapRisks().size()).isEqualTo(TimeUnit.HOURS.toMinutes(4) / CV_ANALYSIS_WINDOW_MINUTES);
    heatMapRisks = new TreeSet<>(heatMap.getHeatMapRisks());
    heatMapRisks.forEach(heatMapRisk -> {
      assertThat(heatMapRisk.getTimeStamp()).isEqualTo(Instant.ofEpochMilli(timeStamp.get()));
      timeStamp.addAndGet(TimeUnit.MINUTES.toMillis(CV_ANALYSIS_WINDOW_MINUTES));
    });

    // last bucket should contain 4 risks
    heatMap = heatMaps.get(2);
    assertThat(heatMap.getHeatMapBucketStartTime()).isEqualTo(Instant.ofEpochMilli(timeStamp.get()));
    assertThat(heatMap.getHeatMapRisks().size()).isEqualTo(4);
    heatMapRisks = new TreeSet<>(heatMap.getHeatMapRisks());
    heatMapRisks.forEach(heatMapRisk -> {
      assertThat(heatMapRisk.getTimeStamp()).isEqualTo(Instant.ofEpochMilli(timeStamp.get()));
      timeStamp.addAndGet(TimeUnit.MINUTES.toMillis(CV_ANALYSIS_WINDOW_MINUTES));
    });

    // update a few riskscores
    heatMapService.updateRiskScore(accountId, serviceIdentifier, envIdentifier, CVMonitoringCategory.PERFORMANCE,
        TimeUnit.MINUTES.toMillis(70), 0.7);
    heatMapService.updateRiskScore(accountId, serviceIdentifier, envIdentifier, CVMonitoringCategory.PERFORMANCE,
        TimeUnit.MINUTES.toMillis(90), 0.3);
    heatMapService.updateRiskScore(accountId, serviceIdentifier, envIdentifier, CVMonitoringCategory.PERFORMANCE,
        TimeUnit.MINUTES.toMillis(270), 0.8);
    heatMapService.updateRiskScore(accountId, serviceIdentifier, envIdentifier, CVMonitoringCategory.PERFORMANCE,
        TimeUnit.MINUTES.toMillis(490), 0.9);

    heatMaps = hPersistence.createQuery(HeatMap.class, excludeAuthority).asList();
    heatMap = heatMaps.get(0);
    heatMap.getHeatMapRisks().forEach(heatMapRisk
        -> assertThat(heatMapRisk.getRiskScore())
               .isEqualTo(
                   heatMapRisk.getTimeStamp().equals(Instant.ofEpochMilli(TimeUnit.MINUTES.toMillis(70))) ? 0.7 : 0.6,
                   offset(0.0001)));
    heatMap = heatMaps.get(1);
    heatMap.getHeatMapRisks().forEach(heatMapRisk
        -> assertThat(heatMapRisk.getRiskScore())
               .isEqualTo(
                   heatMapRisk.getTimeStamp().equals(Instant.ofEpochMilli(TimeUnit.MINUTES.toMillis(270))) ? 0.8 : 0.6,
                   offset(0.0001)));
    heatMap = heatMaps.get(2);
    heatMap.getHeatMapRisks().forEach(heatMapRisk
        -> assertThat(heatMapRisk.getRiskScore())
               .isEqualTo(
                   heatMapRisk.getTimeStamp().equals(Instant.ofEpochMilli(TimeUnit.MINUTES.toMillis(490))) ? 0.9 : 0.6,
                   offset(0.0001)));
  }
}
