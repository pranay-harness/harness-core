package io.harness.batch.processing.anomalydetection.alerts.service.itfc;

import java.time.Instant;

public interface AnomalyAlertsService {
  void sendAnomalyDailyReport(String accountId, Instant date);
}
