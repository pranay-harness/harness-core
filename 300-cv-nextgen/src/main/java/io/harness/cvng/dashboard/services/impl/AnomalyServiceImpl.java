package io.harness.cvng.dashboard.services.impl;

import static io.harness.persistence.HQuery.excludeAuthority;

import io.harness.cvng.beans.CVMonitoringCategory;
import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.core.services.api.CVConfigService;
import io.harness.cvng.dashboard.beans.AnomalyDTO;
import io.harness.cvng.dashboard.beans.AnomalyDTO.AnomalyDetailDTO;
import io.harness.cvng.dashboard.beans.AnomalyDTO.AnomalyMetricDetail;
import io.harness.cvng.dashboard.beans.AnomalyDTO.AnomalyTxnDetail;
import io.harness.cvng.dashboard.entities.Anomaly;
import io.harness.cvng.dashboard.entities.Anomaly.AnomalousMetric;
import io.harness.cvng.dashboard.entities.Anomaly.AnomalyDetail;
import io.harness.cvng.dashboard.entities.Anomaly.AnomalyKeys;
import io.harness.cvng.dashboard.entities.Anomaly.AnomalyStatus;
import io.harness.cvng.dashboard.services.api.AnomalyService;
import io.harness.persistence.HIterator;
import io.harness.persistence.HPersistence;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.common.util.concurrent.AtomicDouble;
import com.google.inject.Inject;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import org.mongodb.morphia.UpdateOptions;
import org.mongodb.morphia.query.Query;

public class AnomalyServiceImpl implements AnomalyService {
  @Inject private HPersistence hPersistence;
  @Inject private CVConfigService cvConfigService;

  @Override
  public void openAnomaly(
      String accountId, String cvConfigId, Instant anomalyTimestamp, List<AnomalousMetric> anomalousMetrics) {
    CVConfig cvConfig = cvConfigService.get(cvConfigId);
    if (cvConfig == null) {
      return;
    }

    hPersistence.getDatastore(Anomaly.class)
        .update(hPersistence.createQuery(Anomaly.class, excludeAuthority)
                    .filter(AnomalyKeys.projectIdentifier, cvConfig.getProjectIdentifier())
                    .filter(AnomalyKeys.serviceIdentifier, cvConfig.getServiceIdentifier())
                    .filter(AnomalyKeys.envIdentifier, cvConfig.getEnvIdentifier())
                    .filter(AnomalyKeys.category, cvConfig.getCategory())
                    .filter(AnomalyKeys.status, AnomalyStatus.OPEN)
                    .field(AnomalyKeys.anomalyStartTime)
                    .lessThanOrEq(anomalyTimestamp),
            hPersistence.createUpdateOperations(Anomaly.class)
                .setOnInsert(AnomalyKeys.accountId, accountId)
                .setOnInsert(AnomalyKeys.anomalyStartTime, anomalyTimestamp)
                .addToSet(AnomalyKeys.anomalousConfigIds, cvConfigId)
                .addToSet(AnomalyKeys.anomalyDetails,
                    AnomalyDetail.builder()
                        .reportedTime(anomalyTimestamp)
                        .cvConfigId(cvConfigId)
                        .anomalousMetrics(anomalousMetrics)
                        .build()),
            new UpdateOptions().upsert(true));
  }

  @Override
  public void closeAnomaly(String accountId, String cvConfigId, Instant anomalyCloseTimestamp) {
    CVConfig cvConfig = cvConfigService.get(cvConfigId);
    if (cvConfig == null) {
      return;
    }

    Query<Anomaly> query = hPersistence.createQuery(Anomaly.class, excludeAuthority)
                               .filter(AnomalyKeys.projectIdentifier, cvConfig.getProjectIdentifier())
                               .filter(AnomalyKeys.serviceIdentifier, cvConfig.getServiceIdentifier())
                               .filter(AnomalyKeys.envIdentifier, cvConfig.getEnvIdentifier())
                               .filter(AnomalyKeys.category, cvConfig.getCategory())
                               .filter(AnomalyKeys.status, AnomalyStatus.OPEN)
                               .field(AnomalyKeys.anomalyStartTime)
                               .lessThanOrEq(anomalyCloseTimestamp);
    hPersistence.getDatastore(Anomaly.class)
        .update(query,
            hPersistence.createUpdateOperations(Anomaly.class).removeAll(AnomalyKeys.anomalousConfigIds, cvConfigId));

    // close alert if no cv config has anomaly open
    hPersistence.getDatastore(Anomaly.class)
        .update(query.field(AnomalyKeys.anomalousConfigIds).sizeEq(0),
            hPersistence.createUpdateOperations(Anomaly.class)
                .set(AnomalyKeys.status, AnomalyStatus.CLOSED)
                .set(AnomalyKeys.anomalyEndTime, anomalyCloseTimestamp));
  }

  @Override
  public List<AnomalyDTO> getAnomalies(String accountId, String serviceIdentifier, String envIdentifier,
      CVMonitoringCategory category, Instant startTime, Instant endTime) {
    SortedSet<AnomalyDTO> anomalies = new TreeSet<>();

    try (HIterator<Anomaly> anomalyIterator =
             new HIterator<>(hPersistence.createQuery(Anomaly.class, excludeAuthority)
                                 .filter(AnomalyKeys.serviceIdentifier, serviceIdentifier)
                                 .filter(AnomalyKeys.envIdentifier, envIdentifier)
                                 .filter(AnomalyKeys.category, category)
                                 .field(AnomalyKeys.anomalyStartTime)
                                 .greaterThanOrEq(startTime)
                                 .field(AnomalyKeys.anomalyStartTime)
                                 .lessThanOrEq(endTime)
                                 .fetch())) {
      while (anomalyIterator.hasNext()) {
        Anomaly anomaly = anomalyIterator.next();
        AtomicDouble anomalyRisk = new AtomicDouble(0);

        Map<String, Set<AnomalousMetric>> anomalousMetricsByCvConfigs =
            getAnomalousMetricsByCvConfigs(anomaly.getAnomalyDetails());

        SortedSet<AnomalyDetailDTO> anomalyDetailDTOS = new TreeSet<>();
        anomalousMetricsByCvConfigs.forEach((cvConfigId, anomalousMetrics) -> {
          SortedSet<AnomalyMetricDetail> anomalyMetricDetails = getAnomalyMetricDetailsWithRisk(anomalousMetrics);
          AnomalousMetric maxRiskMetric = anomalousMetrics.stream()
                                              .max(Comparator.comparing(AnomalousMetric::getRiskScore))
                                              .orElse(AnomalousMetric.builder().riskScore(0.0).build());
          anomalyDetailDTOS.add(AnomalyDetailDTO.builder()
                                    .cvConfigId(cvConfigId)
                                    .riskScore(maxRiskMetric.getRiskScore())
                                    .metricDetails(anomalyMetricDetails)
                                    .build());

          if (anomalyRisk.get() < maxRiskMetric.getRiskScore()) {
            anomalyRisk.set(maxRiskMetric.getRiskScore());
          }
        });
        // TODO: replace identifier by name
        AnomalyDTO anomalyDTO =
            AnomalyDTO.builder()
                .serviceName(serviceIdentifier)
                .envName(envIdentifier)
                .startTimestamp(anomaly.getAnomalyStartTime().toEpochMilli())
                .endTimestamp(anomaly.getAnomalyEndTime() == null ? null : anomaly.getAnomalyEndTime().toEpochMilli())
                .status(anomaly.getStatus())
                .category(anomaly.getCategory())
                .anomalyDetails(anomalyDetailDTOS)
                .riskScore(anomalyRisk.get())
                .build();

        anomalies.add(anomalyDTO);
      }
    }

    return new ArrayList<>(anomalies);
  }

  private SortedSet<AnomalyMetricDetail> getAnomalyMetricDetailsWithRisk(Set<AnomalousMetric> anomalousMetrics) {
    Table<String, String, AnomalousMetric> metricTxnMap = HashBasedTable.create();
    anomalousMetrics.forEach(anomalousMetric -> {
      String metricName = anomalousMetric.getMetricName();
      String groupName = anomalousMetric.getGroupName();
      if (!metricTxnMap.contains(metricName, groupName)) {
        metricTxnMap.put(metricName, groupName, anomalousMetric);
      }

      if (metricTxnMap.get(metricName, groupName).getRiskScore() < anomalousMetric.getRiskScore()) {
        metricTxnMap.get(metricName, groupName).setRiskScore(anomalousMetric.getRiskScore());
      }
    });

    SortedSet<AnomalyMetricDetail> metricDetails = new TreeSet<>();
    metricTxnMap.rowMap().forEach((metricName, txnVsValueMap) -> {
      AtomicDouble maxRisk = new AtomicDouble(0);
      SortedSet<AnomalyTxnDetail> txnDetails = new TreeSet<>();
      txnVsValueMap.forEach((groupName, anomalousMetric) -> {
        txnDetails.add(
            AnomalyTxnDetail.builder().groupName(groupName).riskScore(anomalousMetric.getRiskScore()).build());
        if (maxRisk.get() < anomalousMetric.getRiskScore()) {
          maxRisk.set(anomalousMetric.getRiskScore());
        }
      });
      metricDetails.add(
          AnomalyMetricDetail.builder().metricName(metricName).riskScore(maxRisk.get()).txnDetails(txnDetails).build());
    });
    return metricDetails;
  }

  private Map<String, Set<AnomalousMetric>> getAnomalousMetricsByCvConfigs(Set<AnomalyDetail> anomalyDetails) {
    Map<String, Set<AnomalousMetric>> rv = new HashMap<>();

    Table<String, AnomalousMetric, AnomalousMetric> riskScores = HashBasedTable.create();
    anomalyDetails.forEach(anomalyDetail -> {
      if (!rv.containsKey(anomalyDetail.getCvConfigId())) {
        rv.put(anomalyDetail.getCvConfigId(), new HashSet<>());
      }

      anomalyDetail.getAnomalousMetrics().forEach(anomalousMetric -> {
        if (!riskScores.contains(anomalyDetail.getCvConfigId(), anomalousMetric)) {
          riskScores.put(anomalyDetail.getCvConfigId(), anomalousMetric, anomalousMetric);
        }

        if (riskScores.get(anomalyDetail.getCvConfigId(), anomalousMetric).getRiskScore()
            < anomalousMetric.getRiskScore()) {
          riskScores.get(anomalyDetail.getCvConfigId(), anomalousMetric).setRiskScore(anomalousMetric.getRiskScore());
        }
      });

      rv.get(anomalyDetail.getCvConfigId()).addAll(riskScores.row(anomalyDetail.getCvConfigId()).values());
    });
    return rv;
  }
}
