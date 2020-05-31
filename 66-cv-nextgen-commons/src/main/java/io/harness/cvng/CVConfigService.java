package io.harness.cvng;

import io.harness.cvng.core.services.entities.CVConfig;

import java.util.List;
import javax.annotation.Nullable;

public interface CVConfigService {
  CVConfig save(String accountId, CVConfig cvConfig);
  List<CVConfig> save(String accountId, List<CVConfig> cvConfig);
  void update(CVConfig cvConfig);
  void update(List<CVConfig> cvConfigs);
  @Nullable CVConfig get(String cvConfigId);
  void delete(String cvConfigId);
  void delete(List<String> cvConfigIds);
  List<CVConfig> list(String accountId, String connectorId);
}
