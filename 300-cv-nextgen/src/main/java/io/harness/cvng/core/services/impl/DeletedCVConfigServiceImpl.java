package io.harness.cvng.core.services.impl;

import com.google.inject.Inject;

import io.harness.cvng.core.entities.DeletedCVConfig;
import io.harness.cvng.core.services.api.DataCollectionTaskService;
import io.harness.cvng.core.services.api.DeletedCVConfigService;
import io.harness.persistence.HPersistence;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

@Slf4j
public class DeletedCVConfigServiceImpl implements DeletedCVConfigService {
  @Inject private HPersistence hPersistence;
  @Inject private DataCollectionTaskService dataCollectionTaskService;

  @Override
  public DeletedCVConfig save(DeletedCVConfig deletedCVConfig) {
    hPersistence.save(deletedCVConfig);
    return deletedCVConfig;
  }

  @Nullable
  @Override
  public DeletedCVConfig get(@NotNull String deletedCVConfigId) {
    return hPersistence.get(DeletedCVConfig.class, deletedCVConfigId);
  }

  @Override
  public void triggerCleanup(DeletedCVConfig deletedCVConfig) {
    dataCollectionTaskService.deleteDataCollectionTask(
        deletedCVConfig.getAccountId(), deletedCVConfig.getDataCollectionTaskId());
    logger.info("Deleting DeletedCVConfig {}", deletedCVConfig.getUuid());
    delete(deletedCVConfig.getUuid());
    logger.info("Deletion of DeletedCVConfig {} was successful", deletedCVConfig.getUuid());
    // TODO We need retry mechanism if things get failing and retry count exceeds max number we should alert it
  }

  private void delete(String deletedCVConfigId) {
    hPersistence.delete(DeletedCVConfig.class, deletedCVConfigId);
  }
}
