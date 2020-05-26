package io.harness.cvng;

import io.harness.cvng.core.services.entities.CVConfig;

import javax.annotation.Nullable;

public interface CVConfigService {
  CVConfig save(String accountId, CVConfig cvConfig);
  void update(CVConfig cvConfig);
  @Nullable CVConfig get(String cvConfigId);
  void delete(String cvConfigId);
}
