package io.harness.delegate.service;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus.FAILURE;
import static io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus.RUNNING;
import static io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus.SUCCESS;
import static io.harness.delegate.service.DelegateServiceImpl.getDelegateId;
import static io.harness.network.SafeHttpCall.execute;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static software.wings.beans.Log.LogColor.Red;
import static software.wings.beans.Log.LogColor.Yellow;
import static software.wings.beans.Log.LogWeight.Bold;
import static software.wings.beans.Log.color;
import static software.wings.beans.Log.doneColoring;

import com.google.common.collect.Iterables;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.managerclient.ManagerClient;
import io.harness.observer.Subject;
import io.harness.rest.RestResponse;
import io.harness.verification.VerificationServiceClient;
import lombok.extern.slf4j.Slf4j;
import retrofit2.Response;
import software.wings.beans.Log;
import software.wings.beans.Log.LogLevel;
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
  private Cache<String, List<Log>> cache;
  private Cache<String, List<ThirdPartyApiCallLog>> apiCallLogCache;
  private Cache<String, List<CVActivityLog>> cvActivityLogCache;
  private ManagerClient managerClient;
  private final Subject<LogSanitizer> logSanitizerSubject = new Subject<>();
  private VerificationServiceClient verificationServiceClient;

  @Inject
  public DelegateLogServiceImpl(ManagerClient managerClient, @Named("asyncExecutor") ExecutorService executorService,
      VerificationServiceClient verificationServiceClient) {
    this.managerClient = managerClient;
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
    Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(
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
  public synchronized void save(String accountId, Log log) {
    if (isBlank(log.getActivityId()) || isBlank(log.getCommandUnitName())) {
      logger.info("Logging stack while saving the execution log ", new Exception(""));
    }

    String line = logSanitizerSubject.fireProcess(LogSanitizer::sanitizeLog, log.getActivityId(), log.getLogLine());
    if (log.getLogLevel() == LogLevel.ERROR) {
      line = color(line, Red, Bold);
    } else if (log.getLogLevel() == LogLevel.WARN) {
      line = color(line, Yellow, Bold);
    }
    line = doneColoring(line);
    log.setLogLine(line);

    Optional.ofNullable(cache.get(accountId, s -> new ArrayList<>())).ifPresent(logs -> logs.add(log));
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
      logger.error("Unexpected Cache eviction accountId={}, logs={}, removalCause={}", accountId, logs, removalCause);
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

    for (Log log : commandLogs) {
      if (log.getLogLevel() != batchLogLevel) {
        if (isNotEmpty(batch)) {
          batchedLogs.add(batch);
          batch = new ArrayList<>();
        }
        batchLogLevel = log.getLogLevel();
      }
      batch.add(log);
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

        String logText = logBatch.stream().map(Log::getLogLine).collect(joining("\n"));
        Log log = logBatch.get(0);
        log.setLogLine(logText);
        log.setLinesCount(logBatch.size());
        log.setCommandExecutionStatus(commandUnitStatus);
        log.setCreatedAt(System.currentTimeMillis());
        logger.info("Dispatched log status- [{}] [{}]", log.getCommandUnitName(), log.getCommandExecutionStatus());
        RestResponse restResponse = execute(managerClient.saveCommandUnitLogs(
            activityId, URLEncoder.encode(unitName, StandardCharsets.UTF_8.toString()), accountId, log));
        logger.info("{} log lines dispatched for accountId: {}",
            restResponse.getResource() != null ? logBatch.size() : 0, accountId);
      } catch (Exception e) {
        logger.error("Dispatch log failed. printing lost logs[{}]", logBatch.size(), e);
        logBatch.forEach(log -> logger.error(log.toString()));
        logger.error("Finished printing lost logs");
      }
    }
  }

  private void dispatchApiCallLogs(String accountId, List<ThirdPartyApiCallLog> logs, RemovalCause removalCause) {
    if (accountId == null || logs.isEmpty()) {
      logger.error("Unexpected Cache eviction accountId={}, logs={}, removalCause={}", accountId, logs, removalCause);
      return;
    }
    logs.stream()
        .collect(groupingBy(ThirdPartyApiCallLog::getStateExecutionId, toList()))
        .forEach((activityId, logsList) -> {
          if (isEmpty(logsList)) {
            return;
          }
          String stateExecutionId = logsList.get(0).getStateExecutionId();
          String delegateId = getDelegateId();
          logsList.forEach(log -> log.setDelegateId(delegateId));
          try {
            logger.info("Dispatching {} api call logs for [{}] [{}]", logsList.size(), stateExecutionId, accountId);
            RestResponse restResponse = execute(managerClient.saveApiCallLogs(delegateId, accountId, logsList));
            logger.info("Dispatched {} api call logs for [{}] [{}]",
                restResponse == null || restResponse.getResource() != null ? logsList.size() : 0, stateExecutionId,
                accountId);
          } catch (IOException e) {
            logger.error("Dispatch log failed for {}. printing lost logs[{}]", stateExecutionId, logsList.size(), e);
            logsList.forEach(log -> logger.error(log.toString()));
            logger.error("Finished printing lost logs");
          }
        });
  }

  private void dispatchCVActivityLogs(String accountId, List<CVActivityLog> logs, RemovalCause removalCause) {
    if (accountId == null || logs.isEmpty()) {
      logger.error("Unexpected Cache eviction accountId={}, logs={}, removalCause={}", accountId, logs, removalCause);
      return;
    }
    Iterables.partition(logs, 100).forEach(batch -> {
      try {
        safeExecute(verificationServiceClient.saveActivityLogs(accountId, logs));
        logger.info("Dispatched {} cv activity logs [{}]", batch.size(), accountId);
      } catch (Exception e) {
        logger.error("Dispatch log failed. printing lost activity logs[{}]", batch.size(), e);
        batch.forEach(log -> logger.error(log.toString()));
        logger.error("Finished printing lost activity logs");
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
