package io.harness.cvng.dashboard.services.impl;

import static io.harness.cvng.core.services.CVNextGenConstants.PERFORMANCE_PACK_IDENTIFIER;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.persistence.HQuery.excludeAuthority;
import static io.harness.rule.OwnerRule.RAGHU;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

import io.harness.CvNextGenTest;
import io.harness.category.element.UnitTests;
import io.harness.cvng.beans.CVMonitoringCategory;
import io.harness.cvng.beans.DataSourceType;
import io.harness.cvng.core.entities.AppDynamicsCVConfig;
import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.core.entities.MetricPack;
import io.harness.cvng.core.services.api.CVConfigService;
import io.harness.cvng.core.services.api.MetricPackService;
import io.harness.cvng.dashboard.beans.AnomalyDTO;
import io.harness.cvng.dashboard.beans.AnomalyDTO.AnomalyDetailDTO;
import io.harness.cvng.dashboard.beans.AnomalyDTO.AnomalyMetricDetail;
import io.harness.cvng.dashboard.beans.AnomalyDTO.AnomalyTxnDetail;
import io.harness.cvng.dashboard.entities.Anomaly;
import io.harness.cvng.dashboard.entities.Anomaly.AnomalousMetric;
import io.harness.cvng.dashboard.entities.Anomaly.AnomalyDetail;
import io.harness.cvng.dashboard.entities.Anomaly.AnomalyStatus;
import io.harness.cvng.dashboard.services.api.AnomalyService;
import io.harness.data.structure.CollectionUtils;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.SortedSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class AnomalyServiceImplTest extends CvNextGenTest {
  @Inject private AnomalyService anomalyService;
  @Inject private MetricPackService metricPackService;
  @Inject private CVConfigService cvConfigService;

  private String projectIdentifier;
  private String serviceIdentifier;
  private String envIdentifier;
  private String accountId;
  @Inject private HPersistence hPersistence;

  @Before
  public void setUp() {
    projectIdentifier = generateUuid();
    serviceIdentifier = generateUuid();
    envIdentifier = generateUuid();
    accountId = generateUuid();
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testOpenAndCloseanomaly_withSingleCvConfig() {
    Instant instant = Instant.now();
    CVConfig cvConfig = createAndSaveAppDConfig(CVMonitoringCategory.PERFORMANCE);
    List<AnomalousMetric> anomalousMetrics = getAnomalousMetrics(10, 0);

    // open anomaly multiple times
    for (int i = 0; i < 5; i++) {
      anomalyService.openAnomaly(accountId, cvConfig.getUuid(), instant, anomalousMetrics);
    }
    List<Anomaly> anomalies = hPersistence.createQuery(Anomaly.class, excludeAuthority).asList();
    assertThat(anomalies.size()).isEqualTo(1);
    Anomaly anomaly = anomalies.get(0);
    assertThat(anomaly.getAccountId()).isEqualTo(accountId);
    assertThat(anomaly.getServiceIdentifier()).isEqualTo(serviceIdentifier);
    assertThat(anomaly.getEnvIdentifier()).isEqualTo(envIdentifier);
    assertThat(anomaly.getProjectIdentifier()).isEqualTo(projectIdentifier);
    assertThat(anomaly.getCategory()).isEqualTo(cvConfig.getCategory());
    assertThat(anomaly.getStatus()).isEqualTo(AnomalyStatus.OPEN);
    assertThat(anomaly.getAnomalyStartTime()).isEqualTo(instant);
    assertThat(anomaly.getAnomalyEndTime()).isNull();
    assertThat(anomaly.getAnomalyDetails().size()).isEqualTo(1);

    AnomalyDetail anomalyDetail = anomaly.getAnomalyDetails().stream().findFirst().orElse(null);
    assertThat(anomalyDetail).isNotNull();
    assertThat(anomalyDetail.getCvConfigId()).isEqualTo(cvConfig.getUuid());
    assertThat(anomalyDetail.getReportedTime()).isEqualTo(instant);
    assertThat(CollectionUtils.isEqualCollection(anomalyDetail.getAnomalousMetrics(), anomalousMetrics)).isTrue();

    anomalyService.closeAnomaly(accountId, cvConfig.getUuid(), instant);
    anomalies = hPersistence.createQuery(Anomaly.class, excludeAuthority).asList();
    assertThat(anomalies.size()).isEqualTo(1);
    anomaly = anomalies.get(0);
    assertThat(anomaly.getStatus()).isEqualTo(AnomalyStatus.CLOSED);
    assertThat(anomaly.getAnomalyEndTime()).isEqualTo(instant);
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testOpenAndCloseAnomaly_withMultipleConfigsAndInstants() {
    Instant now = Instant.now();
    int numOfCvConfigs = 5;
    List<CVConfig> cvConfigs = new ArrayList<>();
    for (int i = 0; i < numOfCvConfigs; i++) {
      cvConfigs.add(createAndSaveAppDConfig(CVMonitoringCategory.PERFORMANCE));
    }
    List<AnomalousMetric> anomalousMetrics = getAnomalousMetrics(5, 0);
    for (Instant instant = now; instant.toEpochMilli() < now.plus(1, ChronoUnit.HOURS).toEpochMilli();
         instant = instant.plus(5, ChronoUnit.MINUTES)) {
      for (int i = 0; i < cvConfigs.size(); i++) {
        anomalyService.openAnomaly(accountId, cvConfigs.get(i).getUuid(), instant, anomalousMetrics);
      }
    }
    List<Anomaly> anomalies = hPersistence.createQuery(Anomaly.class, excludeAuthority).asList();
    assertThat(anomalies.size()).isEqualTo(1);
    Anomaly anomaly = anomalies.get(0);
    assertThat(anomaly.getAccountId()).isEqualTo(accountId);
    assertThat(anomaly.getServiceIdentifier()).isEqualTo(serviceIdentifier);
    assertThat(anomaly.getEnvIdentifier()).isEqualTo(envIdentifier);
    assertThat(anomaly.getProjectIdentifier()).isEqualTo(projectIdentifier);
    assertThat(anomaly.getCategory()).isEqualTo(CVMonitoringCategory.PERFORMANCE);
    assertThat(anomaly.getStatus()).isEqualTo(AnomalyStatus.OPEN);
    assertThat(anomaly.getAnomalyStartTime()).isEqualTo(now);
    assertThat(anomaly.getAnomalyEndTime()).isNull();
    assertThat(anomaly.getAnomalyDetails().size())
        .isEqualTo(12 * numOfCvConfigs); // 5 min boundaries in an hour * numOfCvConfigs

    for (Instant instant = now; instant.toEpochMilli() < now.plus(1, ChronoUnit.HOURS).toEpochMilli();
         instant = instant.plus(5, ChronoUnit.MINUTES)) {
      for (int i = 0; i < cvConfigs.size(); i++) {
        assertThat(anomaly.getAnomalyDetails())
            .contains(AnomalyDetail.builder().reportedTime(instant).cvConfigId(cvConfigs.get(i).getUuid()).build());
      }
    }

    anomaly.getAnomalyDetails().forEach(anomalyDetail
        -> assertThat(CollectionUtils.isEqualCollection(anomalyDetail.getAnomalousMetrics(), anomalousMetrics))
               .isTrue());

    // close anomalies and test
    for (int i = 0; i < numOfCvConfigs; i++) {
      anomalyService.closeAnomaly(accountId, cvConfigs.get(i).getUuid(), now.plus(i * 10, ChronoUnit.MINUTES));
      anomalies = hPersistence.createQuery(Anomaly.class, excludeAuthority).asList();
      assertThat(anomalies.size()).isEqualTo(1);
      anomaly = anomalies.get(0);
      if (i == numOfCvConfigs - 1) {
        assertThat(anomaly.getStatus()).isEqualTo(AnomalyStatus.CLOSED);
        assertThat(anomaly.getAnomalyEndTime()).isEqualTo(now.plus(i * 10, ChronoUnit.MINUTES));
      } else {
        assertThat(anomaly.getStatus()).isEqualTo(AnomalyStatus.OPEN);
      }
    }
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testAnoamlies_withMultipleCategory() {
    Instant now = Instant.now();
    List<CVConfig> cvConfigs = new ArrayList<>();
    for (CVMonitoringCategory category : CVMonitoringCategory.values()) {
      cvConfigs.add(createAndSaveAppDConfig(category));
    }
    List<AnomalousMetric> anomalousMetrics = getAnomalousMetrics(5, 0);
    for (Instant instant = now; instant.toEpochMilli() < now.plus(1, ChronoUnit.HOURS).toEpochMilli();
         instant = instant.plus(5, ChronoUnit.MINUTES)) {
      for (int i = 0; i < cvConfigs.size(); i++) {
        anomalyService.openAnomaly(accountId, cvConfigs.get(i).getUuid(), instant, anomalousMetrics);
      }
    }
    List<Anomaly> anomalies = hPersistence.createQuery(Anomaly.class, excludeAuthority).asList();
    assertThat(anomalies.size()).isEqualTo(CVMonitoringCategory.values().length);

    for (int i = 0; i < CVMonitoringCategory.values().length; i++) {
      CVMonitoringCategory category = CVMonitoringCategory.values()[i];
      Anomaly anomaly = anomalies.get(i);
      assertThat(anomaly.getAccountId()).isEqualTo(accountId);
      assertThat(anomaly.getServiceIdentifier()).isEqualTo(serviceIdentifier);
      assertThat(anomaly.getEnvIdentifier()).isEqualTo(envIdentifier);
      assertThat(anomaly.getProjectIdentifier()).isEqualTo(projectIdentifier);
      assertThat(anomaly.getCategory()).isEqualTo(category);
      assertThat(anomaly.getStatus()).isEqualTo(AnomalyStatus.OPEN);
      assertThat(anomaly.getAnomalyStartTime()).isEqualTo(now);
      assertThat(anomaly.getAnomalyEndTime()).isNull();
    }
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testCloseAnomaly_whenCvConfigDeleted() {
    Instant instant = Instant.now();
    CVConfig cvConfig = createAndSaveAppDConfig(CVMonitoringCategory.PERFORMANCE);
    List<AnomalousMetric> anomalousMetrics = getAnomalousMetrics(10, 0);

    anomalyService.openAnomaly(accountId, cvConfig.getUuid(), instant, anomalousMetrics);
    List<Anomaly> anomalies = hPersistence.createQuery(Anomaly.class, excludeAuthority).asList();
    assertThat(anomalies.size()).isEqualTo(1);
    Anomaly anomaly = anomalies.get(0);
    assertThat(anomaly.getStatus()).isEqualTo(AnomalyStatus.OPEN);

    cvConfigService.delete(cvConfig.getUuid());
    anomaly = hPersistence.createQuery(Anomaly.class, excludeAuthority).asList().get(0);
    assertThat(anomaly.getStatus()).isEqualTo(AnomalyStatus.CLOSED);
  }

  private CVConfig createAndSaveAppDConfig(CVMonitoringCategory category) {
    metricPackService.getMetricPacks(accountId, projectIdentifier, DataSourceType.APP_DYNAMICS);
    AppDynamicsCVConfig appDynamicsCVConfig = new AppDynamicsCVConfig();
    appDynamicsCVConfig.setServiceIdentifier(serviceIdentifier);
    appDynamicsCVConfig.setEnvIdentifier(envIdentifier);
    appDynamicsCVConfig.setProjectIdentifier(projectIdentifier);
    appDynamicsCVConfig.setAccountId(accountId);
    appDynamicsCVConfig.setCategory(category);
    appDynamicsCVConfig.setMetricPack(MetricPack.builder()
                                          .identifier(PERFORMANCE_PACK_IDENTIFIER)
                                          .metrics(Sets.newHashSet(MetricPack.MetricDefinition.builder().build()))
                                          .build());
    appDynamicsCVConfig.setConnectorIdentifier(generateUuid());
    appDynamicsCVConfig.setGroupId(generateUuid());
    appDynamicsCVConfig.setApplicationName(generateUuid());
    appDynamicsCVConfig.setTierName(generateUuid());
    return cvConfigService.save(appDynamicsCVConfig);
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testGetAnomaly_withSingleCvConfig() {
    Instant instant = Instant.now();
    CVConfig cvConfig = createAndSaveAppDConfig(CVMonitoringCategory.PERFORMANCE);

    int numOfAnomalousMetrics = 10;
    int numOfAnomalies = 5;
    for (int i = 0; i < numOfAnomalies; i++) {
      anomalyService.openAnomaly(accountId, cvConfig.getUuid(), instant.plus(i, ChronoUnit.MINUTES),
          getAnomalousMetrics(numOfAnomalousMetrics, i * numOfAnomalousMetrics));
    }
    Collection<AnomalyDTO> anomalies = anomalyService.getAnomalies(accountId, serviceIdentifier, envIdentifier,
        CVMonitoringCategory.PERFORMANCE, instant, instant.plus(1, ChronoUnit.HOURS));
    assertThat(anomalies.size()).isEqualTo(1);
    AnomalyDTO anomaly = anomalies.iterator().next();
    assertThat(anomaly.getStartTimestamp()).isEqualTo(instant.toEpochMilli());
    assertThat(anomaly.getEndTimestamp()).isNull();
    assertThat(anomaly.getCategory()).isEqualTo(CVMonitoringCategory.PERFORMANCE);
    assertThat(anomaly.getStatus()).isEqualTo(AnomalyStatus.OPEN);
    assertThat(anomaly.getRiskScore()).isEqualTo(0.1 * (numOfAnomalies * numOfAnomalousMetrics - 1));
    assertThat(anomaly.getServiceName()).isEqualTo(serviceIdentifier);
    assertThat(anomaly.getEnvName()).isEqualTo(envIdentifier);
    SortedSet<AnomalyDetailDTO> anomalyDetails = anomaly.getAnomalyDetails();
    assertThat(anomalyDetails.size()).isEqualTo(1);
    AnomalyDetailDTO anomalyDetail = anomalyDetails.first();
    assertThat(anomalyDetail.getCvConfigId()).isEqualTo(cvConfig.getUuid());
    assertThat(anomalyDetail.getRiskScore()).isEqualTo(0.1 * (numOfAnomalies * numOfAnomalousMetrics - 1));
    assertThat(anomalyDetail.getMetricDetails().size()).isEqualTo(numOfAnomalousMetrics);

    int i = numOfAnomalousMetrics;
    int maxIndex = numOfAnomalies * numOfAnomalousMetrics - 1;
    for (AnomalyMetricDetail metricDetail : anomalyDetail.getMetricDetails()) {
      assertThat(metricDetail.getMetricName()).isEqualTo("m-" + (i - 1));
      assertThat(metricDetail.getRiskScore()).isEqualTo(maxIndex * 0.1, offset(0.0001));
      assertThat(metricDetail.getTxnDetails().size()).isEqualTo(numOfAnomalies);

      int j = 0;
      for (AnomalyTxnDetail txnDetail : metricDetail.getTxnDetails()) {
        assertThat(txnDetail.getGroupName()).isEqualTo("g-" + (maxIndex - j * numOfAnomalousMetrics));
        assertThat(txnDetail.getRiskScore()).isEqualTo(maxIndex * 0.1 - j, offset(0.0001));
        j++;
      }
      i--;
      maxIndex--;
    }

    anomalyService.closeAnomaly(accountId, cvConfig.getUuid(), instant.plus(1, ChronoUnit.HOURS));
    anomalies = anomalyService.getAnomalies(accountId, serviceIdentifier, envIdentifier,
        CVMonitoringCategory.PERFORMANCE, instant, instant.plus(1, ChronoUnit.HOURS));
    assertThat(anomalies.size()).isEqualTo(1);
    anomaly = anomalies.iterator().next();
    assertThat(anomaly.getStartTimestamp()).isEqualTo(instant.toEpochMilli());
    assertThat(anomaly.getEndTimestamp()).isEqualTo(instant.plus(1, ChronoUnit.HOURS).toEpochMilli());
    assertThat(anomaly.getCategory()).isEqualTo(CVMonitoringCategory.PERFORMANCE);
    assertThat(anomaly.getStatus()).isEqualTo(AnomalyStatus.CLOSED);
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testGetAnomaly_SortedByStatus() {
    Instant instant = Instant.now();
    int numOfAnomalousMetrics = 10;
    CVConfig cvConfig = createAndSaveAppDConfig(CVMonitoringCategory.PERFORMANCE);
    anomalyService.openAnomaly(accountId, cvConfig.getUuid(), instant, getAnomalousMetrics(numOfAnomalousMetrics, 0));
    anomalyService.closeAnomaly(accountId, cvConfig.getUuid(), instant);

    anomalyService.openAnomaly(accountId, cvConfig.getUuid(), instant.plus(1, ChronoUnit.MINUTES),
        getAnomalousMetrics(numOfAnomalousMetrics, 0));
    anomalyService.closeAnomaly(accountId, cvConfig.getUuid(), instant.plus(1, ChronoUnit.MINUTES));

    anomalyService.openAnomaly(accountId, cvConfig.getUuid(), instant.plus(5, ChronoUnit.MINUTES),
        getAnomalousMetrics(numOfAnomalousMetrics, 0));

    Collection<AnomalyDTO> anomalies = anomalyService.getAnomalies(accountId, serviceIdentifier, envIdentifier,
        CVMonitoringCategory.PERFORMANCE, instant, instant.plus(1, ChronoUnit.HOURS));

    assertThat(anomalies.size()).isEqualTo(3);
    List<AnomalyDTO> anomalyDTOS = new ArrayList<>(anomalies);
    assertThat(anomalyDTOS.get(0).getStatus()).isEqualTo(AnomalyStatus.OPEN);
    assertThat(anomalyDTOS.get(1).getStatus()).isEqualTo(AnomalyStatus.CLOSED);
    assertThat(anomalyDTOS.get(2).getStatus()).isEqualTo(AnomalyStatus.CLOSED);
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testGetAnomaly_withMultipleConfigsAndInstants() {
    Instant now = Instant.now();
    int numOfCvConfigs = 5;
    int numOfAnomalousTxns = 5;
    List<CVConfig> cvConfigs = new ArrayList<>();
    for (int i = 0; i < numOfCvConfigs; i++) {
      cvConfigs.add(createAndSaveAppDConfig(CVMonitoringCategory.PERFORMANCE));
    }
    int numOfIntervals = (int) (TimeUnit.HOURS.toMinutes(1) / 5);
    for (int i = 0; i < cvConfigs.size(); i++) {
      int j = 0;
      for (Instant instant = now; instant.toEpochMilli() < now.plus(1, ChronoUnit.HOURS).toEpochMilli();
           instant = instant.plus(5, ChronoUnit.MINUTES)) {
        anomalyService.openAnomaly(accountId, cvConfigs.get(i).getUuid(), instant,
            getAnomalousMetrics(numOfAnomalousTxns, j * numOfAnomalousTxns));
        j++;
      }
    }

    Collection<AnomalyDTO> anomalies = anomalyService.getAnomalies(accountId, serviceIdentifier, envIdentifier,
        CVMonitoringCategory.PERFORMANCE, now, now.plus(1, ChronoUnit.HOURS));

    assertThat(anomalies.size()).isEqualTo(1);
    AnomalyDTO anomaly = anomalies.iterator().next();
    assertThat(anomaly.getStatus()).isEqualTo(AnomalyStatus.OPEN);
    assertThat(anomaly.getCategory()).isEqualTo(CVMonitoringCategory.PERFORMANCE);

    SortedSet<AnomalyDetailDTO> anomalyDetails = anomaly.getAnomalyDetails();
    assertThat(anomalyDetails.size()).isEqualTo(numOfCvConfigs);
    // verify that anomaly details are sorted by risk score
    for (AnomalyDetailDTO anomalyDetail : anomalyDetails) {
      final AtomicInteger maxIndex = new AtomicInteger(numOfAnomalousTxns * numOfIntervals - 1);
      assertThat(anomalyDetail.getRiskScore()).isEqualTo(maxIndex.get() * 0.1);
      assertThat(anomalyDetail.getMetricDetails().size()).isEqualTo(numOfAnomalousTxns);

      AtomicInteger metricIndex = new AtomicInteger(numOfAnomalousTxns);
      anomalyDetail.getMetricDetails().forEach(anomalyMetricDetail -> {
        int metricNameIndex = metricIndex.decrementAndGet();
        assertThat(anomalyMetricDetail.getMetricName()).isEqualTo("m-" + metricNameIndex);
        assertThat(anomalyMetricDetail.getRiskScore()).isEqualTo(maxIndex.get() * 0.1);
        assertThat(anomalyMetricDetail.getTxnDetails().size()).isEqualTo(numOfIntervals);

        AtomicInteger txnNameIndex = new AtomicInteger(maxIndex.get());
        anomalyMetricDetail.getTxnDetails().forEach(anomalyTxnDetail -> {
          assertThat(anomalyTxnDetail.getGroupName()).isEqualTo("g-" + txnNameIndex.get());
          assertThat(anomalyTxnDetail.getRiskScore()).isEqualTo(txnNameIndex.get() * 0.1, offset(0.00001));
          txnNameIndex.addAndGet(-numOfAnomalousTxns);
        });
        maxIndex.decrementAndGet();
      });
    }
  }

  private List<AnomalousMetric> getAnomalousMetrics(int numOfMetrics, int offset) {
    List<AnomalousMetric> rv = new ArrayList<>();
    for (int i = offset; i < offset + numOfMetrics; i++) {
      rv.add(AnomalousMetric.builder()
                 .groupName("g-" + i)
                 .metricName("m-" + (i % numOfMetrics))
                 .riskScore(0.1 * i)
                 .build());
    }

    return rv;
  }
}
