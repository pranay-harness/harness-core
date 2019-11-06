package software.wings.delegatetasks.cv;

import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;
import static io.harness.threading.Morpheus.sleep;
import static software.wings.common.VerificationConstants.RATE_LIMIT_STATUS;
import static software.wings.common.VerificationConstants.URL_STRING;

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
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.HttpStatus;
import retrofit2.Call;
import retrofit2.Response;
import software.wings.delegatetasks.AbstractDelegateRunnableTask;
import software.wings.delegatetasks.DelegateCVActivityLogService;
import software.wings.delegatetasks.DelegateCVActivityLogService.Logger;
import software.wings.delegatetasks.DelegateLogService;
import software.wings.service.impl.ThirdPartyApiCallLog;
import software.wings.service.impl.ThirdPartyApiCallLog.FieldType;
import software.wings.service.impl.ThirdPartyApiCallLog.ThirdPartyApiCallField;
import software.wings.service.impl.VerificationLogContext;
import software.wings.service.impl.analysis.DataCollectionInfoV2;
import software.wings.service.impl.analysis.DataCollectionTaskResult;
import software.wings.service.impl.analysis.DataCollectionTaskResult.DataCollectionTaskStatus;
import software.wings.service.intfc.security.EncryptionService;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
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
  private DataCollectionExecutionContext dataCollectionExecutionContext;
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
    try (VerificationLogContext ignored =
             new VerificationLogContext(dataCollectionInfo.getAccountId(), dataCollectionInfo.getCvConfigId(),
                 dataCollectionInfo.getStateExecutionId(), dataCollectionInfo.getStateType(), OVERRIDE_ERROR)) {
      initializeActivityLogger();
      try {
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
          initializeDataCollector();
          dataCollectionExecutionContext = createDataCollectionExecutionContext();
          dataCollector.init(dataCollectionExecutionContext, (T) dataCollectionInfo);
          collectAndSaveData((T) dataCollectionInfo);
        });
      } catch (Exception e) {
        taskResult.setStatus(DataCollectionTaskStatus.FAILURE);
        taskResult.setErrorMessage(e.getMessage());
      }

      logger.info("Data collection task completed with status {}", taskResult.getStatus());
      activityLogger.info("Finished data collection with status: " + taskResult.getStatus());
      return taskResult;
    }
  }

  private void decryptIfHasEncryptableSetting(DataCollectionInfoV2 dataCollectionInfo) {
    if (dataCollectionInfo.getEncryptableSetting().isPresent()) {
      encryptionService.decrypt(
          dataCollectionInfo.getEncryptableSetting().get(), dataCollectionInfo.getEncryptedDataDetails());
    }
  }

  private void initializeDataCollector() {
    try {
      this.dataCollector =
          (DataCollector<T>) dataCollectorFactory.newInstance(dataCollectionInfo.getDataCollectorImplClass());
      injector.injectMembers(dataCollector);
    } catch (NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
      throw new RuntimeException(e);
    }
  }

  private void initializeActivityLogger() {
    activityLogger =
        cvActivityLogService.getLogger(dataCollectionInfo.getAccountId(), dataCollectionInfo.getCvConfigId(),
            TimeUnit.MILLISECONDS.toMinutes(dataCollectionInfo.getEndTime().toEpochMilli()),
            dataCollectionInfo.getStateExecutionId(), " Time range %t to %t",
            dataCollectionInfo.getStartTime().toEpochMilli(), dataCollectionInfo.getEndTime().toEpochMilli() + 1);
  }

  private DataCollectionExecutionContext createDataCollectionExecutionContext() {
    return new DataCollectionExecutionContext() {
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

      @Override
      public <U> U executeRequest(String thirdPartyApiCallTitle, Call<U> request) {
        int retryCount = 0;
        while (true) {
          try {
            return executeRequest(thirdPartyApiCallTitle, request, retryCount);
          } catch (RateLimitExceededException e) {
            int randomNum = ThreadLocalRandom.current().nextInt(1, 5);
            logger.info("Encountered Rate limiting. Sleeping {} seconds for stateExecutionId {}",
                RETRY_SLEEP_DURATION.getSeconds() + randomNum, dataCollectionInfo.getStateExecutionId());
            sleep(RETRY_SLEEP_DURATION.plus(Duration.ofSeconds(randomNum)));
            if (retryCount == MAX_RETRIES) {
              logger.error("Request did not succeed after " + MAX_RETRIES + "  retries stateExecutionId {}",
                  dataCollectionInfo.getStateExecutionId());
              throw new DataCollectionException(e);
            }
          } catch (Exception e) {
            if (retryCount == MAX_RETRIES) {
              logger.error("Request did not succeed after " + MAX_RETRIES + "  retries");
              throw new DataCollectionException(e);
            }
          }
          retryCount++;
        }
      }

      private <U> U executeRequest(String thirdPartyApiCallTitle, Call<U> request, int retryCount) {
        ThirdPartyApiCallLog apiCallLog = createApiCallLog();
        apiCallLog.setTitle(thirdPartyApiCallTitle);
        apiCallLog.setRequestTimeStamp(OffsetDateTime.now().toInstant().toEpochMilli());
        try {
          apiCallLog.addFieldToRequest(ThirdPartyApiCallField.builder()
                                           .name(URL_STRING)
                                           .value(request.request().url().toString())
                                           .type(FieldType.URL)
                                           .build());
          if (retryCount != 0) {
            apiCallLog.addFieldToRequest(ThirdPartyApiCallField.builder()
                                             .name("RETRY")
                                             .value(String.valueOf(retryCount))
                                             .type(FieldType.NUMBER)
                                             .build());
          }
          Response<U> response = request.clone().execute(); // TODO: add retry logic and rate limit exceeded logic here.
          apiCallLog.setResponseTimeStamp(OffsetDateTime.now().toInstant().toEpochMilli());
          if (response.isSuccessful()) {
            apiCallLog.addFieldToResponse(response.code(), response.body(), FieldType.JSON);

          } else if (response.code() == RATE_LIMIT_STATUS) {
            apiCallLog.addFieldToResponse(response.code(), response.toString(), FieldType.TEXT);
            throw new RateLimitExceededException(
                "Response code: " + response.code() + " Error: " + response.errorBody().string());
          } else {
            apiCallLog.addFieldToResponse(response.code(), response.toString(), FieldType.TEXT);
            throw new DataCollectionException(
                "Response code: " + response.code() + " Error: " + response.errorBody().string());
          }
          return response.body();
        } catch (IOException e) {
          apiCallLog.setResponseTimeStamp(OffsetDateTime.now().toInstant().toEpochMilli());
          apiCallLog.addFieldToResponse(HttpStatus.SC_BAD_REQUEST, ExceptionUtils.getStackTrace(e), FieldType.TEXT);
          throw new DataCollectionException(e);
        } finally {
          saveThirdPartyApiCallLog(apiCallLog);
        }
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
