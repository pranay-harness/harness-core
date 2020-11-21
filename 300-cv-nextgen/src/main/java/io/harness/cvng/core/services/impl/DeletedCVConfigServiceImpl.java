package io.harness.cvng.core.services.impl;

import io.harness.cvng.core.entities.DeletedCVConfig;
import io.harness.cvng.core.services.api.DataCollectionTaskService;
import io.harness.cvng.core.services.api.DeletedCVConfigService;
import io.harness.cvng.core.services.api.VerificationTaskService;
import io.harness.persistence.HPersistence;

import com.google.inject.Inject;
import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DeletedCVConfigServiceImpl implements DeletedCVConfigService {
  @Inject private HPersistence hPersistence;
  @Inject private DataCollectionTaskService dataCollectionTaskService;
  @Inject private VerificationTaskService verificationTaskService;

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
    dataCollectionTaskService.deletePerpetualTasks(
        deletedCVConfig.getAccountId(), deletedCVConfig.getPerpetualTaskId());
    verificationTaskService.removeCVConfigMappings(deletedCVConfig.getCvConfig().getUuid());
    log.info("Deleting DeletedCVConfig {}", deletedCVConfig.getUuid());
    delete(deletedCVConfig.getUuid());
    log.info("Deletion of DeletedCVConfig {} was successful", deletedCVConfig.getUuid());
    // TODO We need retry mechanism if things get failing and retry count exceeds max number we should alert it
  }

  private void delete(String deletedCVConfigId) {
    hPersistence.delete(DeletedCVConfig.class, deletedCVConfigId);
  }
}
