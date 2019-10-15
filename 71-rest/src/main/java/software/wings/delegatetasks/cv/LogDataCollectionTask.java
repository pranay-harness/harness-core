package software.wings.delegatetasks.cv;

import com.google.common.collect.Iterables;
import com.google.inject.Inject;

import io.harness.beans.DelegateTask;
import io.harness.delegate.beans.DelegateTaskResponse;
import software.wings.common.VerificationConstants;
import software.wings.delegatetasks.LogAnalysisStoreService;
import software.wings.service.impl.analysis.LogDataCollectionInfoV2;
import software.wings.service.impl.analysis.LogElement;
import software.wings.service.intfc.analysis.ClusterLevel;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class LogDataCollectionTask<T extends LogDataCollectionInfoV2> extends AbstractDataCollectionTask<T> {
  private LogDataCollector<T> logDataCollector;
  @Inject private LogAnalysisStoreService logAnalysisStoreService;

  public LogDataCollectionTask(String delegateId, DelegateTask delegateTask, Consumer<DelegateTaskResponse> consumer,
      Supplier<Boolean> preExecute) {
    super(delegateId, delegateTask, consumer, preExecute);
  }

  @Override
  protected void collectAndSaveData(LogDataCollectionInfoV2 dataCollectionInfo) throws DataCollectionException {
    this.logDataCollector = (LogDataCollector<T>) getDataCollector();
    final List<LogElement> logElements = new ArrayList<>();
    final List<Callable<List<LogElement>>> callables = new ArrayList<>();
    if (dataCollectionInfo.getHosts().isEmpty()) {
      addHeartbeats(Optional.empty(), dataCollectionInfo, logElements);
      callables.add(() -> logDataCollector.fetchLogs());
    } else {
      Iterables.partition(dataCollectionInfo.getHosts(), logDataCollector.getHostBatchSize())
          .forEach(batch -> callables.add(() -> logDataCollector.fetchLogs(batch)));
      dataCollectionInfo.getHosts().forEach(host -> addHeartbeats(Optional.of(host), dataCollectionInfo, logElements));
    }
    List<Optional<List<LogElement>>> results = execute(callables);
    results.forEach(result -> {
      if (result.isPresent()) {
        logElements.addAll(result.get());
      }
    });
    save(dataCollectionInfo, logElements);
  }

  private void save(LogDataCollectionInfoV2 dataCollectionInfo, List<LogElement> logElements)
      throws DataCollectionException {
    try {
      boolean response = logAnalysisStoreService.save(dataCollectionInfo.getStateType(),
          dataCollectionInfo.getAccountId(), dataCollectionInfo.getApplicationId(), dataCollectionInfo.getCvConfigId(),
          dataCollectionInfo.getStateExecutionId(), dataCollectionInfo.getWorkflowId(),
          dataCollectionInfo.getWorkflowExecutionId(), dataCollectionInfo.getServiceId(), getTaskId(), logElements);
      if (!response) {
        throw new DataCollectionException("Unable to save log elements. API returned false");
      }
    } catch (IOException e) {
      throw new DataCollectionException(e);
    }
  }

  private List<Optional<List<LogElement>>> execute(List<Callable<List<LogElement>>> callables)
      throws DataCollectionException {
    List<Optional<List<LogElement>>> results;
    try {
      results = executeParallel(callables);
    } catch (IOException e) {
      throw new DataCollectionException(e);
    }
    return results;
  }

  protected void addHeartbeats(
      Optional<String> host, LogDataCollectionInfoV2 logDataCollectionInfo, List<LogElement> logElements) {
    // end time is excluded for heartbeat creation.
    for (long heartbeatMin = TimeUnit.MILLISECONDS.toMinutes(logDataCollectionInfo.getStartTime().toEpochMilli());
         heartbeatMin <= TimeUnit.MILLISECONDS.toMinutes(logDataCollectionInfo.getEndTime().toEpochMilli() - 1);
         heartbeatMin++) {
      logElements.add(
          LogElement.builder()
              .query(logDataCollectionInfo.getQuery())
              .clusterLabel(String.valueOf(ClusterLevel.H2.getLevel()))
              .host(host.isPresent()
                      ? host.get()
                      : VerificationConstants.DUMMY_HOST_NAME) // TODO: we should get rid of this requirement and
                                                               // everything should work without setting the host.
              .count(0)
              .logMessage("")
              .timeStamp(TimeUnit.MINUTES.toMillis(heartbeatMin))
              .logCollectionMinute((int) heartbeatMin)
              .build());
    }
  }
}
