package software.wings.delegatetasks.cv;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Injector;

import io.harness.beans.DelegateTask;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.beans.ResponseData;
import io.harness.delegate.task.TaskParameters;
import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import software.wings.delegatetasks.AbstractDelegateRunnableTask;
import software.wings.delegatetasks.DelegateCVActivityLogService;
import software.wings.delegatetasks.DelegateCVActivityLogService.Logger;
import software.wings.delegatetasks.DelegateLogService;
import software.wings.service.impl.ThirdPartyApiCallLog;
import software.wings.service.impl.analysis.DataCollectionInfoV2;
import software.wings.service.impl.analysis.DataCollectionTaskResult;
import software.wings.service.impl.analysis.DataCollectionTaskResult.DataCollectionTaskStatus;
import software.wings.service.intfc.security.EncryptionService;

import java.lang.reflect.InvocationTargetException;
import java.time.Duration;
import java.util.function.Consumer;
import java.util.function.Supplier;
@Slf4j
public abstract class AbstractDataCollectionTask<T extends DataCollectionInfoV2> extends AbstractDelegateRunnableTask {
  @VisibleForTesting static Duration RETRY_SLEEP_DURATION = Duration.ofSeconds(10);
  private static final int MAX_RETRIES = 3;
  @Inject private DelegateLogService delegateLogService;
  @Inject private Injector injector;
  @Inject private DataCollectorFactory dataCollectorFactory;
  @Inject private EncryptionService encryptionService;
  @Inject private DelegateCVActivityLogService cvActivityLogService;
  private DataCollectionInfoV2 dataCollectionInfo;
  private DataCollector<T> dataCollector;
  private DataCollectionCallback dataCollectionCallback;
  private Logger activityLogger;

  public AbstractDataCollectionTask(String delegateId, DelegateTask delegateTask,
      Consumer<DelegateTaskResponse> consumer, Supplier<Boolean> preExecute) {
    super(delegateId, delegateTask, consumer, preExecute);
  }

  @Override
  public ResponseData run(Object[] parameters) {
    throw new RuntimeException("Not implemented. This should not get called for data collection tasks.");
  }

  @Override
  public ResponseData run(TaskParameters parameters) {
    dataCollectionInfo = (DataCollectionInfoV2) parameters;
    decryptIfHasEncryptableSetting(dataCollectionInfo);
    DataCollectionTaskResult taskResult = DataCollectionTaskResult.builder()
                                              .status(DataCollectionTaskStatus.SUCCESS)
                                              .stateType(dataCollectionInfo.getStateType())
                                              .build();
    try {
      initializeDataCollector();
      dataCollectionCallback = createDataCollectionCallback();
      RetryPolicy<Object> retryPolicy =
          new RetryPolicy<>()
              .handle(Exception.class)
              .withDelay(RETRY_SLEEP_DURATION)
              .withMaxRetries(MAX_RETRIES)
              .onFailedAttempt(event -> {
                activityLogger.warn(
                    "[Retrying] Data collection task failed with exception: " + event.getLastFailure().getMessage());
                taskResult.setErrorMessage(event.getLastFailure().getMessage());
              })
              .onFailure(event -> {
                activityLogger.error("Data collection failed with exception: " + event.getFailure().getMessage());
                logger.error("DataCollectionException: ", event.getFailure());
                taskResult.setStatus(DataCollectionTaskStatus.FAILURE);
                taskResult.setErrorMessage(event.getFailure().getMessage());
              });
      activityLogger.info("Starting data collection.");

      Failsafe.with(retryPolicy).run(() -> {
        dataCollector.init(dataCollectionCallback, (T) dataCollectionInfo);
        collectAndSaveData((T) dataCollectionInfo);
      });
    } catch (Exception e) {
      taskResult.setStatus(DataCollectionTaskStatus.FAILURE);
      taskResult.setErrorMessage(e.getMessage());
    }

    logger.info("Data collection task completed {}", dataCollectionInfo.getStateExecutionId());
    activityLogger.info("Finished data collection with status: " + taskResult.getStatus());
    return taskResult;
  }

  private void decryptIfHasEncryptableSetting(DataCollectionInfoV2 dataCollectionInfo) {
    if (dataCollectionInfo.getEncryptableSetting().isPresent()) {
      encryptionService.decrypt(
          dataCollectionInfo.getEncryptableSetting().get(), dataCollectionInfo.getEncryptedDataDetails());
    }
  }

  private void initializeDataCollector() {
    activityLogger = cvActivityLogService.getLogger(dataCollectionInfo.getAccountId(),
        dataCollectionInfo.getCvConfigId(), dataCollectionInfo.getEndTime().toEpochMilli(),
        dataCollectionInfo.getStateExecutionId(), " Time range %t to %t",
        dataCollectionInfo.getStartTime().toEpochMilli(), dataCollectionInfo.getEndTime().toEpochMilli() + 1);
    try {
      this.dataCollector =
          (DataCollector<T>) dataCollectorFactory.newInstance(dataCollectionInfo.getDataCollectorImplClass());
      injector.injectMembers(dataCollector);
    } catch (NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
      throw new RuntimeException(e);
    }
  }

  private DataCollectionCallback createDataCollectionCallback() {
    return new DataCollectionCallback() {
      @Override
      public ThirdPartyApiCallLog createApiCallLog() {
        return AbstractDataCollectionTask.this.createApiCallLog();
      }

      @Override
      public void saveThirdPartyApiCallLog(ThirdPartyApiCallLog thirdPartyApiCallLog) {
        AbstractDataCollectionTask.this.saveThirdPartyApiCallLog(thirdPartyApiCallLog);
      }

      @Override
      public Logger getActivityLogger() {
        return activityLogger;
      }
    };
  }

  protected abstract void collectAndSaveData(T parameters) throws DataCollectionException;

  private void saveThirdPartyApiCallLog(ThirdPartyApiCallLog thirdPartyApiCallLog) {
    delegateLogService.save(dataCollectionInfo.getAccountId(), thirdPartyApiCallLog);
  }

  private ThirdPartyApiCallLog createApiCallLog() {
    // TODO: need to fix this for 24  * 7. For 24 * 7 task stateExecutionId is set to CV_24x7_STATE_EXECUTION + "-" +
    // cvConfigId
    return createApiCallLog(dataCollectionInfo.getStateExecutionId());
  }

  protected DataCollector<T> getDataCollector() {
    return dataCollector;
  }
}
