package software.wings.delegate.service;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus.FAILURE;
import static software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus.RUNNING;
import static software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus.SUCCESS;
import static software.wings.managerclient.SafeHttpCall.execute;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Log;
import software.wings.beans.RestResponse;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.delegatetasks.DelegateLogService;
import software.wings.managerclient.ManagerClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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
  private ManagerClient managerClient;

  @Inject
  public DelegateLogServiceImpl(ManagerClient managerClient, ExecutorService executorService) {
    this.managerClient = managerClient;
    this.cache = Caffeine.newBuilder()
                     .executor(executorService)
                     .expireAfterWrite(1000, TimeUnit.MILLISECONDS)
                     .removalListener(this ::dispatchCommandExecutionLogs)
                     .build();
    Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(()
                                                                         -> this.cache.cleanUp(),
        1000, 1000,
        TimeUnit.MILLISECONDS); // periodic cleanup for expired keys
  }

  @Override
  @SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
  public void save(String accountId, Log log) {
    cache.get(accountId, s -> new ArrayList<>()).add(log);
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
    try {
      CommandExecutionStatus commandUnitStatus = commandLogs.stream()
                                                     .filter(Objects::nonNull)
                                                     .map(Log::getCommandExecutionStatus)
                                                     .filter(asList(SUCCESS, FAILURE)::contains)
                                                     .findFirst()
                                                     .orElse(RUNNING);

      String logText = commandLogs.stream().map(Log::getLogLine).collect(joining("\n"));
      Log log = commandLogs.get(0);
      log.setLogLine(logText);
      log.setLinesCount(commandLogs.size());
      log.setCommandExecutionStatus(commandUnitStatus);
      log.setCreatedAt(System.currentTimeMillis());
      logger.info("Dispatched log status- [{}] [{}]", log.getCommandUnitName(), log.getCommandExecutionStatus());
      RestResponse restResponse = execute(managerClient.saveCommandUnitLogs(activityId, unitName, accountId, log));
      logger.info("{} log lines dispatched for accountId: {}",
          restResponse.getResource() != null ? commandLogs.size() : 0, accountId);
    } catch (Exception e) {
      logger.error("Dispatch log failed. printing lost logs[{}]", commandLogs.size(), e);
      commandLogs.forEach(log -> logger.error(log.toString()));
      logger.error("Finished printing lost logs");
    }
  }
}
