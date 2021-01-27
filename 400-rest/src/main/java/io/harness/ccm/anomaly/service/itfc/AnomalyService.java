package io.harness.ccm.anomaly.service.itfc;

import io.harness.ccm.anomaly.entities.AnomalyEntity;

import java.time.Instant;
import java.util.List;

public interface AnomalyService {
  List<AnomalyEntity> list(String account, Instant date);
  void delete(List<String> ids, Instant date);
}
