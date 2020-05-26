package io.harness.cvng;

import com.google.inject.Inject;

import io.harness.cvng.core.services.entities.CVConfig;
import io.harness.persistence.HPersistence;

import javax.annotation.Nullable;

public class CVConfigServiceImpl implements CVConfigService {
  @Inject HPersistence hPersistence;
  @Override
  public CVConfig save(String accountId, CVConfig cvConfig) {
    hPersistence.save(cvConfig);
    return cvConfig;
  }

  @Nullable
  @Override
  public CVConfig get(String cvConfigId) {
    return hPersistence.get(CVConfig.class, cvConfigId);
  }

  @Override
  public void update(CVConfig cvConfig) {
    hPersistence.save(cvConfig);
  }

  @Override
  public void delete(String cvConfigId) {
    hPersistence.delete(CVConfig.class, cvConfigId);
  }
}
