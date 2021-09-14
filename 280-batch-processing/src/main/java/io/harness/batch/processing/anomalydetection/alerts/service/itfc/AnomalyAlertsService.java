/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.batch.processing.anomalydetection.alerts.service.itfc;

import java.time.Instant;

public interface AnomalyAlertsService {
  void sendAnomalyDailyReport(String accountId, Instant date);
}
