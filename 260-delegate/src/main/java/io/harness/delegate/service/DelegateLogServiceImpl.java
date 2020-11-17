package io.harness.delegate.service;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.delegate.service.DelegateAgentServiceImpl.getDelegateId;
import static io.harness.logging.CommandExecutionStatus.FAILURE;
import static io.harness.logging.CommandExecutionStatus.RUNNING;
import static io.harness.logging.CommandExecutionStatus.SUCCESS;
import static io.harness.network.SafeHttpCall.execute;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static software.wings.beans.LogColor.Red;
import static software.wings.beans.LogColor.Yellow;
import static software.wings.beans.LogHelper.color;
import static software.wings.beans.LogHelper.doneColoring;
import static software.wings.beans.LogWeight.Bold;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogLevel;
import io.harness.managerclient.DelegateAgentManagerClient;
import io.harness.managerclient.ManagerClient;
import io.harness.managerclient.VerificationServiceClient;
import io.harness.observer.Subject;
import io.harness.rest.RestResponse;
import io.harness.serializer.KryoSerializer;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import org.apache.commons.lang3.StringUtils;
import retrofit2.Response;
import software.wings.beans.Log;
import software.wings.delegatetasks.DelegateLogService;
import software.wings.delegatetasks.LogSanitizer;
import software.wings.service.impl.ThirdPartyApiCallLog;
import software.wings.verification.CVActivityLog;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.validation.executable.ValidateOnExecution;

@Singleton
@ValidateOnExecution
@Slf4j
public class DelegateLogServiceImpl implements DelegateLogService {
  // 10 KB Activity logs batch size
  static final int ACTIVITY_LOGS_BATCH_SIZE = 1024 * 10;

  private final String LOGS_COMMON_MESSAGE_ERROR = "Unexpected Cache eviction accountId={}, logs={}, removalCause={}";

  private Cache<String, List<Log>> cache;
  private Cache<String, List<ThirdPartyApiCallLog>> apiCallLogCache;
  private Cache<String, List<CVActivityLog>> cvActivityLogCache;
  private ManagerClient managerClient;
  private DelegateAgentManagerClient delegateAgentManagerClient;
  private final Subject<LogSanitizer> logSanitizerSubject = new Subject<>();
  private VerificationServiceClient verificationServiceClient;
  private final KryoSerializer kryoSerializer;

  @Inject
  public DelegateLogServiceImpl(ManagerClient managerClient, DelegateAgentManagerClient delegateAgentManagerClient,
      @Named("asyncExecutor") ExecutorService executorService, VerificationServiceClient verificationServiceClient,
      KryoSerializer kryoSerializer) {
    this.managerClient = managerClient;
    this.delegateAgentManagerClient = delegateAgentManagerClient;
    this.verificationServiceClient = verificationServiceClient;
    this.cache = Caffeine.newBuilder()
                     .executor(executorService)
                     .expireAfterWrite(1000, TimeUnit.MILLISECONDS)
                     .removalListener(this ::dispatchCommandExecutionLogs)
                     .build();
    this.apiCallLogCache = Caffeine.newBuilder()
                               .executor(executorService)
                               .expireAfterWrite(1000, TimeUnit.MILLISECONDS)
                               .removalListener(this ::dispatchApiCallLogs)
                               .build();
    this.cvActivityLogCache = Caffeine.newBuilder()
                                  .executor(executorService)
                                  .expireAfterWrite(1000, TimeUnit.MILLISECONDS)
                                  .removalListener(this ::dispatchCVActivityLogs)
                                  .build();
    this.kryoSerializer = kryoSerializer;

    Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder().setNameFormat("delegate-log-service").build())
        .scheduleAtFixedRate(
            ()
                -> {
              this.cache.cleanUp();
              this.apiCallLogCache.cleanUp();
              this.cvActivityLogCache.cleanUp();
            },
            1000, 1000,
            TimeUnit.MILLISECONDS); // periodic cleanup for expired keys
  }

  @Override
  public synchronized void save(String accountId, Log logObject) {
    if (isNotEmpty(accountId)) {
      logObject.setAccountId(accountId);
    }

    if (isBlank(logObject.getActivityId()) || isBlank(logObject.getCommandUnitName())) {
      log.info("Logging stack while saving the execution logObject ", new Exception(""));
    }

    String line =
        logSanitizerSubject.fireProcess(LogSanitizer::sanitizeLog, logObject.getActivityId(), logObject.getLogLine());
    if (logObject.getLogLevel() == LogLevel.ERROR) {
      line = color(line, Red, Bold);
    } else if (logObject.getLogLevel() == LogLevel.WARN) {
      line = color(line, Yellow, Bold);
    }
    line = doneColoring(line);
    logObject.setLogLine(line);

    Optional.ofNullable(cache.get(accountId, s -> new ArrayList<>())).ifPresent(logs -> logs.add(logObject));
  }

  @Override
  public synchronized void save(String accountId, ThirdPartyApiCallLog thirdPartyApiCallLog) {
    thirdPartyApiCallLog.setUuid(null);

    Optional.ofNullable(apiCallLogCache.get(accountId, s -> new ArrayList<>()))
        .ifPresent(logs -> logs.add(thirdPartyApiCallLog));
  }
  @Override
  public synchronized void save(String accountId, CVActivityLog cvActivityLog) {
    Optional.ofNullable(cvActivityLogCache.get(accountId, s -> new ArrayList<>()))
        .ifPresent(cvActivityLogs -> cvActivityLogs.add(cvActivityLog));
  }
  @Override
  public void registerLogSanitizer(LogSanitizer sanitizer) {
    logSanitizerSubject.register(sanitizer);
  }

  @Override
  public void unregisterLogSanitizer(LogSanitizer sanitizer) {
    logSanitizerSubject.unregister(sanitizer);
  }

  private void dispatchCommandExecutionLogs(String accountId, List<Log> logs, RemovalCause removalCause) {
    if (accountId == null || logs.isEmpty()) {
      log.error(LOGS_COMMON_MESSAGE_ERROR, accountId, logs, removalCause);
      return;
    }
    logs.stream()
        .collect(groupingBy(Log::getActivityId, toList()))
        .forEach((activityId, logsList) -> dispatchActivityLogs(accountId, activityId, logsList));
  }

  private void dispatchActivityLogs(String accountId, String activityId, List<Log> logsList) {
    logsList.stream()
        .collect(groupingBy(Log::getCommandUnitName, toList()))
        .forEach((unitName, commandLogs) -> dispatchCommandUnitLogs(accountId, activityId, unitName, commandLogs));
  }

  private void dispatchCommandUnitLogs(String accountId, String activityId, String unitName, List<Log> commandLogs) {
    List<List<Log>> batchedLogs = new ArrayList<>();
    List<Log> batch = new ArrayList<>();
    LogLevel batchLogLevel = LogLevel.INFO;

    for (Log logObject : commandLogs) {
      if (logObject.getLogLevel() != batchLogLevel) {
        if (isNotEmpty(batch)) {
          batchedLogs.add(batch);
          batch = new ArrayList<>();
        }
        batchLogLevel = logObject.getLogLevel();
      }
      batch.add(logObject);
    }
    if (isNotEmpty(batch)) {
      batchedLogs.add(batch);
    }

    for (List<Log> logBatch : batchedLogs) {
      try {
        CommandExecutionStatus commandUnitStatus = logBatch.stream()
                                                       .filter(Objects::nonNull)
                                                       .map(Log::getCommandExecutionStatus)
                                                       .filter(asList(SUCCESS, FAILURE)::contains)
                                                       .findFirst()
                                                       .orElse(RUNNING);

        String logText = trimAndIndicate(logBatch);
        Log logObject = logBatch.get(0);
        logObject.setLogLine(logText);
        logObject.setLinesCount(StringUtils.countMatches(logText, "\n"));
        logObject.setCommandExecutionStatus(commandUnitStatus);
        logObject.setCreatedAt(System.currentTimeMillis());

        byte[] logSerialized = kryoSerializer.asBytes(logObject);

        log.info("Dispatched logObject status- [{}] [{}]", logObject.getCommandUnitName(),
            logObject.getCommandExecutionStatus());
        RestResponse restResponse = execute(delegateAgentManagerClient.saveCommandUnitLogs(activityId,
            URLEncoder.encode(unitName, StandardCharsets.UTF_8.toString()), accountId,
            RequestBody.create(MediaType.parse("application/octet-stream"), logSerialized)));
        log.info("{} logObject lines dispatched for accountId: {}",
            restResponse.getResource() != null ? logBatch.size() : 0, accountId);
      } catch (Exception e) {
        log.error("Dispatch log failed. printing lost logs[{}]", logBatch.size(), e);
        logBatch.forEach(logObject -> log.error(logObject.toString()));
        log.error("Finished printing lost logs");
      }
    }
  }

  @VisibleForTesting
  String trimAndIndicate(List<Log> logBatch) {
    final String logLines = logBatch.stream().map(Log::getLogLine).collect(joining("\n"));
    if (StringUtils.length(logLines) > ACTIVITY_LOGS_BATCH_SIZE) {
      return new StringBuilder()
          .append(StringUtils.left(logLines, ACTIVITY_LOGS_BATCH_SIZE))
          .append("\nThe above log messages are truncated to 10KB\n")
          .toString();
    }
    return logLines;
  }

  private void dispatchApiCallLogs(String accountId, List<ThirdPartyApiCallLog> logs, RemovalCause removalCause) {
    if (accountId == null || logs.isEmpty()) {
      log.error(LOGS_COMMON_MESSAGE_ERROR, accountId, logs, removalCause);
      return;
    }
    logs.stream()
        .collect(groupingBy(ThirdPartyApiCallLog::getStateExecutionId, toList()))
        .forEach((activityId, logsList) -> {
          if (isEmpty(logsList)) {
            return;
          }
          String stateExecutionId = logsList.get(0).getStateExecutionId();
          String delegateId = getDelegateId().orElse(null);
          logsList.forEach(logObject -> logObject.setDelegateId(delegateId));
          try {
            log.info("Dispatching {} api call logs for [{}] [{}]", logsList.size(), stateExecutionId, accountId);

            log.debug("Converting the logs into a byte array.");
            byte[] logsListAsBytes = kryoSerializer.asBytes(logsList);
            RequestBody logsAsRequestBody =
                RequestBody.create(MediaType.parse("application/octet-stream"), logsListAsBytes);
            log.debug("Logs successfully converted!");

            RestResponse restResponse =
                execute(delegateAgentManagerClient.saveApiCallLogs(delegateId, accountId, logsAsRequestBody));
            log.info("Dispatched {} api call logs for [{}] [{}]",
                restResponse == null || restResponse.getResource() != null ? logsList.size() : 0, stateExecutionId,
                accountId);
          } catch (IOException e) {
            log.error("Dispatch log failed for {}. printing lost logs[{}]", stateExecutionId, logsList.size(), e);
            logsList.forEach(logObject -> log.error(logObject.toString()));
            log.error("Finished printing lost logs");
          }
        });
  }

  private void dispatchCVActivityLogs(String accountId, List<CVActivityLog> logs, RemovalCause removalCause) {
    if (accountId == null || logs.isEmpty()) {
      log.error(LOGS_COMMON_MESSAGE_ERROR, accountId, logs, removalCause);
      return;
    }
    Iterables.partition(logs, 100).forEach(batch -> {
      try {
        safeExecute(verificationServiceClient.saveActivityLogs(accountId, logs));
        log.info("Dispatched {} cv activity logs [{}]", batch.size(), accountId);
      } catch (Exception e) {
        log.error("Dispatch log failed. printing lost activity logs[{}]", batch.size(), e);
        batch.forEach(logObject -> log.error(logObject.toString()));
        log.error("Finished printing lost activity logs");
      }
    });
  }
  private void safeExecute(retrofit2.Call<?> call) throws IOException {
    Response<?> response = call.execute();
    if (!response.isSuccessful()) {
      throw new RuntimeException("Response code: " + response.code() + ", error body: " + response.errorBody());
    }
  }
}
