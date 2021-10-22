package io.harness.cvng.core.services.impl.demo;

import io.harness.cvng.beans.DataCollectionExecutionStatus;
import io.harness.cvng.beans.DataCollectionTaskDTO.DataCollectionTaskResult;
import io.harness.cvng.beans.DataCollectionTaskDTO.DataCollectionTaskResult.DataCollectionTaskResultBuilder;
import io.harness.cvng.core.entities.DataCollectionTask;
import io.harness.cvng.core.entities.demo.CVNGDemoPerpetualTask;
import io.harness.cvng.core.services.api.DataCollectionTaskService;
import io.harness.cvng.core.services.api.LogRecordService;
import io.harness.cvng.core.services.api.TimeSeriesRecordService;
import io.harness.cvng.core.services.api.demo.CVNGDemoPerpetualTaskService;
import io.harness.cvng.models.VerificationType;
import io.harness.persistence.HPersistence;

import com.google.inject.Inject;
import java.io.IOException;
import java.time.Clock;
import java.util.Optional;

public class CVNGDemoPerpetualTaskServiceImpl implements CVNGDemoPerpetualTaskService {
  @Inject HPersistence hPersistence;
  @Inject Clock clock;
  @Inject DataCollectionTaskService dataCollectionTaskService;
  @Inject TimeSeriesRecordService timeSeriesRecordService;
  @Inject LogRecordService logRecordService;

  @Override
  public String createCVNGDemoPerpetualTask(String accountId, String dataCollectionWorkerId) {
    CVNGDemoPerpetualTask cvngDemoPerpetualTask =
        CVNGDemoPerpetualTask.builder().accountId(accountId).dataCollectionWorkerId(dataCollectionWorkerId).build();
    return hPersistence.save(cvngDemoPerpetualTask);
  }

  @Override
  public void execute(CVNGDemoPerpetualTask cvngDemoPerpetualTask) {
    Optional<DataCollectionTask> dataCollectionTask = dataCollectionTaskService.getNextTask(
        cvngDemoPerpetualTask.getAccountId(), cvngDemoPerpetualTask.getDataCollectionWorkerId());
    if (dataCollectionTask.isPresent()) {
      DataCollectionTaskResultBuilder dataCollectionTaskResultBuilder =
          DataCollectionTaskResult.builder().dataCollectionTaskId(dataCollectionTask.get().getUuid());
      try {
        saveResults(dataCollectionTask.get());
        dataCollectionTaskResultBuilder.status(DataCollectionExecutionStatus.SUCCESS);
      } catch (Exception e) {
        dataCollectionTaskResultBuilder.status(DataCollectionExecutionStatus.FAILED)
            .stacktrace(e.getStackTrace().toString())
            .exception(e.getMessage());
      }
      dataCollectionTaskService.updateTaskStatus(dataCollectionTaskResultBuilder.build());
    }
  }

  @Override
  public void deletePerpetualTask(String accountId, String perpetualTaskId) {
    hPersistence.delete(hPersistence.get(CVNGDemoPerpetualTask.class, perpetualTaskId));
  }

  private void saveResults(DataCollectionTask dataCollectionTask) throws IOException {
    if (dataCollectionTask.getDataCollectionInfo().getVerificationType().equals(VerificationType.TIME_SERIES)) {
      timeSeriesRecordService.createDemoAnalysisData(dataCollectionTask.getAccountId(),
          dataCollectionTask.getVerificationTaskId(), dataCollectionTask.getDataCollectionWorkerId(),
          dataCollectionTask.getStartTime(), dataCollectionTask.getEndTime());
    } else {
      logRecordService.createDemoAnalysisData(dataCollectionTask.getAccountId(),
          dataCollectionTask.getVerificationTaskId(), dataCollectionTask.getDataCollectionWorkerId(),
          dataCollectionTask.getStartTime(), dataCollectionTask.getEndTime());
    }
  }
}
