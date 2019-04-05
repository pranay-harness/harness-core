package software.wings.delegate.service;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus.FAILURE;
import static io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus.RUNNING;
import static io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus.SUCCESS;
import static io.harness.network.SafeHttpCall.execute;
import static java.lang.String.format;
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
import static software.wings.delegate.service.DelegateServiceImpl.getDelegateId;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.rest.RestResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Log;
import software.wings.beans.Log.LogLevel;
import software.wings.delegatetasks.DelegateLogService;
import software.wings.managerclient.ManagerClient;
import software.wings.service.impl.ThirdPartyApiCallLog;

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

/**
 * Created by peeyushaggarwal on 1/9/17.
 */
@Singleton
@ValidateOnExecution
public class DelegateLogServiceImpl implements DelegateLogService {
  private static final Logger logger = LoggerFactory.getLogger(DelegateLogServiceImpl.class);
  private Cache<String, List<Log>> cache;
  private Cache<String, List<ThirdPartyApiCallLog>> apiCallLogCache;
  private ManagerClient managerClient;

  @Inject
  public DelegateLogServiceImpl(ManagerClient managerClient, @Named("asyncExecutor") ExecutorService executorService) {
    this.managerClient = managerClient;
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
    Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(
        ()
            -> {
          this.cache.cleanUp();
          this.apiCallLogCache.cleanUp();
        },
        1000, 1000,
        TimeUnit.MILLISECONDS); // periodic cleanup for expired keys
  }

  @Override
  public synchronized void save(String accountId, Log log) {
    if (isBlank(log.getActivityId()) || isBlank(log.getCommandUnitName())) {
      // ToDo Remove it once the root cause of the empty activityId or commandUnitName is found
      logger.info("Logging stack while saving the execution log ", new Exception(""));
    }

    String line = log.getLogLine();
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
        logger.error(format("Dispatch log failed. printing lost logs[%d]", logBatch.size()), e);
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
}
