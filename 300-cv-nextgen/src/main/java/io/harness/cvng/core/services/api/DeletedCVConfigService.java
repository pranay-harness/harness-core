package io.harness.cvng.core.services.api;

import io.harness.cvng.core.entities.DeletedCVConfig;

import java.time.Duration;
import javax.annotation.Nullable;

public interface DeletedCVConfigService {
  DeletedCVConfig save(DeletedCVConfig deletedCVConfig, Duration toDeleteAfterDuration);
  @Nullable DeletedCVConfig get(String deletedCVConfigId);
  void triggerCleanup(DeletedCVConfig deletedCVConfig);
}
