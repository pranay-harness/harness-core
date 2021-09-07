package io.harness.cvng.core.services.impl;

import io.harness.cvng.analysis.entities.ClusteredLog;
import io.harness.cvng.analysis.entities.DeploymentLogAnalysis;
import io.harness.cvng.analysis.entities.DeploymentTimeSeriesAnalysis;
import io.harness.cvng.analysis.entities.LearningEngineTask;
import io.harness.cvng.analysis.entities.LogAnalysisCluster;
import io.harness.cvng.analysis.entities.LogAnalysisRecord;
import io.harness.cvng.analysis.entities.LogAnalysisResult;
import io.harness.cvng.analysis.entities.TimeSeriesAnomalousPatterns;
import io.harness.cvng.analysis.entities.TimeSeriesCumulativeSums;
import io.harness.cvng.analysis.entities.TimeSeriesRiskSummary;
import io.harness.cvng.analysis.entities.TimeSeriesShortTermHistory;
import io.harness.cvng.core.entities.DataCollectionTask;
import io.harness.cvng.core.entities.DeletedCVConfig;
import io.harness.cvng.core.entities.HostRecord;
import io.harness.cvng.core.entities.LogRecord;
import io.harness.cvng.core.entities.TimeSeriesRecord;
import io.harness.cvng.core.entities.VerificationTask;
import io.harness.cvng.core.services.api.CVEventService;
import io.harness.cvng.core.services.api.DeletedCVConfigService;
import io.harness.cvng.core.services.api.VerificationTaskService;
import io.harness.cvng.statemachine.entities.AnalysisOrchestrator;
import io.harness.cvng.statemachine.entities.AnalysisStateMachine;
import io.harness.persistence.HPersistence;
import io.harness.persistence.PersistentEntity;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DeletedCVConfigServiceImpl implements DeletedCVConfigService {
  @VisibleForTesting
  static final Collection<? extends Class<? extends PersistentEntity>> ENTITIES_DELETE_BLACKLIST_BY_VERIFICATION_ID =
      Arrays.asList();
  @VisibleForTesting
  static final List<Class<? extends PersistentEntity>> ENTITIES_TO_DELETE_BY_VERIFICATION_ID =
      Arrays.asList(ClusteredLog.class, TimeSeriesShortTermHistory.class, TimeSeriesRecord.class,
          AnalysisOrchestrator.class, AnalysisStateMachine.class, LearningEngineTask.class, LogRecord.class,
          HostRecord.class, LogAnalysisRecord.class, LogAnalysisResult.class, LogAnalysisCluster.class,
          DeploymentTimeSeriesAnalysis.class, DeploymentLogAnalysis.class, TimeSeriesRiskSummary.class,
          TimeSeriesAnomalousPatterns.class, DataCollectionTask.class, TimeSeriesCumulativeSums.class);
  @Inject private HPersistence hPersistence;
  @Inject private VerificationTaskService verificationTaskService;
  @Inject private CVEventService eventService;

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
    List<String> verificationTaskIds =
        verificationTaskService.getVerificationTaskIds(deletedCVConfig.getCvConfig().getUuid());
    verificationTaskIds.forEach(verificationTaskId
        -> ENTITIES_TO_DELETE_BY_VERIFICATION_ID.forEach(entity
            -> hPersistence.delete(hPersistence.createQuery(entity).filter(
                VerificationTask.VERIFICATION_TASK_ID_KEY, verificationTaskId))));
    verificationTaskService.removeCVConfigMappings(deletedCVConfig.getCvConfig().getUuid());

    log.info("Deleting DeletedCVConfig {}", deletedCVConfig.getUuid());
    delete(deletedCVConfig.getUuid());
    log.info("Deletion of DeletedCVConfig {} was successful", deletedCVConfig.getUuid());
    // TODO We need retry mechanism if things get failing and retry count exceeds max number we should alert it

    sendScopedDeleteEvent(deletedCVConfig);
  }

  private void sendScopedDeleteEvent(DeletedCVConfig deletedCVConfig) {
    eventService.sendConnectorDeleteEvent(deletedCVConfig.getCvConfig());
    eventService.sendServiceDeleteEvent(deletedCVConfig.getCvConfig());
    eventService.sendEnvironmentDeleteEvent(deletedCVConfig.getCvConfig());
  }

  private void delete(String deletedCVConfigId) {
    hPersistence.delete(DeletedCVConfig.class, deletedCVConfigId);
  }
}
