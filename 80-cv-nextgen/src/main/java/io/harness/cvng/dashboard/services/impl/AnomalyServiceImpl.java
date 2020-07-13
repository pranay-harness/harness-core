package io.harness.cvng.dashboard.services.impl;

import static io.harness.persistence.HQuery.excludeAuthority;

import com.google.inject.Inject;

import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.core.services.api.CVConfigService;
import io.harness.cvng.dashboard.entities.Anomaly;
import io.harness.cvng.dashboard.entities.Anomaly.AnomalousMetric;
import io.harness.cvng.dashboard.entities.Anomaly.AnomalyDetail;
import io.harness.cvng.dashboard.entities.Anomaly.AnomalyKeys;
import io.harness.cvng.dashboard.entities.Anomaly.AnomalyStatus;
import io.harness.cvng.dashboard.services.api.AnomalyService;
import io.harness.persistence.HPersistence;
import org.mongodb.morphia.UpdateOptions;
import org.mongodb.morphia.query.Query;

import java.time.Instant;
import java.util.List;

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
}
