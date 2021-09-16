package io.harness.cvng.migration.list;

import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.core.entities.MonitoringSourcePerpetualTask;
import io.harness.cvng.core.services.api.CVConfigService;
import io.harness.cvng.core.services.api.MonitoringSourcePerpetualTaskService;
import io.harness.cvng.migration.CVNGMigration;
import io.harness.cvng.migration.beans.ChecklistItem;
import io.harness.persistence.HPersistence;

import com.google.inject.Inject;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CleanUpMonitoringSourcePerpetualTask implements CVNGMigration {
  @Inject private HPersistence hPersistence;
  @Inject private CVConfigService cvConfigService;
  @Inject private MonitoringSourcePerpetualTaskService monitoringSourcePerpetualTaskService;

  @Override
  public void migrate() {
    Set<String> identifiersSet = new HashSet<>();
    hPersistence.createQuery(CVConfig.class).asList().forEach(cvConfig -> {
      identifiersSet.add(cvConfig.getIdentifier());
    });

    List<MonitoringSourcePerpetualTask> monitoringSourcePerpetualTaskList =
        hPersistence.createQuery(MonitoringSourcePerpetualTask.class).asList();
    monitoringSourcePerpetualTaskList.forEach(monitoringSourcePerpetualTask -> {
      if (!identifiersSet.contains(monitoringSourcePerpetualTask.getMonitoringSourceIdentifier())) {
        monitoringSourcePerpetualTaskService.deleteTask(monitoringSourcePerpetualTask.getAccountId(),
            monitoringSourcePerpetualTask.getOrgIdentifier(), monitoringSourcePerpetualTask.getProjectIdentifier(),
            monitoringSourcePerpetualTask.getMonitoringSourceIdentifier());
        log.info("Deleted Monitoring Perpetual task : {}", monitoringSourcePerpetualTask);
      }
    });
    log.info("Monitoring Perpetual task cleanup complete");
  }

  @Override
  public ChecklistItem whatHappensOnRollback() {
    return ChecklistItem.NA;
  }

  @Override
  public ChecklistItem whatHappensIfOldVersionIteratorPicksMigratedEntity() {
    return ChecklistItem.NA;
  }
}
