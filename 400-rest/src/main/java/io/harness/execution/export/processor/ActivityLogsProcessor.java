/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.execution.export.processor;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static java.lang.String.format;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.beans.SearchFilter.Operator;
import io.harness.beans.SortOrder.OrderType;
import io.harness.exception.ExportExecutionsException;
import io.harness.execution.export.metadata.ActivityCommandUnitMetadata;
import io.harness.execution.export.metadata.ApprovalMetadata;
import io.harness.execution.export.metadata.ExecutionDetailsMetadata;
import io.harness.execution.export.metadata.ExecutionMetadata;
import io.harness.execution.export.metadata.GraphNodeMetadata;
import io.harness.execution.export.metadata.GraphNodeVisitor;
import io.harness.persistence.CreatedAtAware;

import software.wings.beans.CGConstants;
import software.wings.beans.Log;
import software.wings.beans.Log.LogKeys;
import software.wings.service.impl.LogServiceImpl;
import software.wings.service.intfc.DataStoreService;
import software.wings.service.intfc.LogService;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import lombok.Setter;
import lombok.Value;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;

@OwnedBy(CDC)
@Value
@Slf4j
public class ActivityLogsProcessor implements ExportExecutionsProcessor {
  private static final int LOGS_BATCH_SIZE = 500;

  @Inject @NonFinal @Setter LogService logService;
  @Inject @NonFinal @Setter DataStoreService dataStoreService;
  @Inject @Named("gdsExecutor") @NonFinal @Setter ExecutorService gdsExecutorService;

  Map<String, ExecutionDetailsMetadata> activityIdToExecutionDetailsMap;
  Map<String, ExecutionMetadata> activityIdToExecutionMap;
  ZipOutputStream zipOutputStream;
  Map<String, String> folderNamesMap;
  Map<String, Set<String>> executionLogFilesMap;

  SimpleDateFormat dateFormat;

  public ActivityLogsProcessor(ZipOutputStream zipOutputStream, Map<String, String> folderNamesMap,
      Map<String, Set<String>> executionLogFilesMap) {
    this.activityIdToExecutionDetailsMap = new HashMap<>();
    this.activityIdToExecutionMap = new HashMap<>();
    this.zipOutputStream = zipOutputStream;
    this.folderNamesMap = folderNamesMap == null ? Collections.emptyMap() : folderNamesMap;
    this.executionLogFilesMap = executionLogFilesMap;

    // Keep the date format same as the UI logs.
    this.dateFormat = new SimpleDateFormat(LogServiceImpl.DATE_FORMATTER_PATTERN);
  }

  public void visitExecutionMetadata(ExecutionMetadata executionMetadata) {
    ActivityIdsVisitor activityIdsVisitor = new ActivityIdsVisitor();
    executionMetadata.accept(activityIdsVisitor);
    Map<String, ExecutionDetailsMetadata> newActivityIdToNodeMetadataMap =
        activityIdsVisitor.getActivityIdToNodeMetadataMap();
    newActivityIdToNodeMetadataMap =
        updateWithShellScriptApprovalMetadata(executionMetadata, newActivityIdToNodeMetadataMap);
    if (isEmpty(newActivityIdToNodeMetadataMap)) {
      return;
    }

    activityIdToExecutionDetailsMap.putAll(newActivityIdToNodeMetadataMap);
    activityIdToExecutionMap.putAll(newActivityIdToNodeMetadataMap.keySet().stream().collect(
        toMap(Function.identity(), ignored -> executionMetadata)));
  }

  private Map<String, ExecutionDetailsMetadata> updateWithShellScriptApprovalMetadata(
      ExecutionMetadata executionMetadata, Map<String, ExecutionDetailsMetadata> existingMap) {
    List<ApprovalMetadata> shellScriptApprovalMetadataList =
        SubCommandsProcessor.getShellScriptApprovalMetadataList(executionMetadata);
    if (isEmpty(shellScriptApprovalMetadataList)) {
      return existingMap;
    }

    Map<String, ApprovalMetadata> shellScriptApprovalMetadataMap =
        shellScriptApprovalMetadataList.stream().collect(toMap(ApprovalMetadata::getActivityId, Function.identity()));
    if (isEmpty(shellScriptApprovalMetadataMap)) {
      return existingMap;
    }

    if (isEmpty(existingMap)) {
      existingMap = new HashMap<>();
    }
    existingMap.putAll(shellScriptApprovalMetadataMap);
    return existingMap;
  }

  public void process() {
    if (isEmpty(activityIdToExecutionDetailsMap)) {
      return;
    }

    List<Log> logs = getAllLogs();
    if (isEmpty(logs)) {
      return;
    }

    Map<String, List<Log>> activityIdToLogsMap = logs.stream().collect(groupingBy(Log::getActivityId));

    try {
      for (Map.Entry<String, List<Log>> entry : activityIdToLogsMap.entrySet()) {
        String activityId = entry.getKey();
        ExecutionMetadata executionMetadata = activityIdToExecutionMap.get(activityId);
        if (executionMetadata == null || executionMetadata.getId() == null) {
          continue;
        }

        updateExecutionDetailsMetadata(
            activityIdToExecutionDetailsMap.get(entry.getKey()), entry.getValue(), executionMetadata.getId());
      }
    } catch (IOException ex) {
      throw new ExportExecutionsException("Unable to create zip file for export executions request", ex);
    }
  }

  private void updateExecutionDetailsMetadata(
      ExecutionDetailsMetadata executionDetailsMetadata, List<Log> logs, String executionId) throws IOException {
    String folderName = folderNamesMap.get(executionId);
    if (executionDetailsMetadata == null || isEmpty(logs) || folderName == null) {
      return;
    }

    List<ActivityCommandUnitMetadata> commandUnits = executionDetailsMetadata.getSubCommands();
    if (isEmpty(commandUnits)) {
      return;
    }

    Map<String, ActivityCommandUnitMetadata> commandUnitMap =
        commandUnits.stream().collect(toMap(ActivityCommandUnitMetadata::getName, Function.identity()));
    for (Log logObject : logs) {
      ActivityCommandUnitMetadata commandUnit = commandUnitMap.getOrDefault(logObject.getCommandUnitName(), null);
      if (commandUnit == null) {
        continue;
      }

      if (commandUnit.getExecutionLogFile() == null) {
        commandUnit.setExecutionLogFile(prepareLogFileName(executionDetailsMetadata, commandUnit));
      }
      addLogLine(commandUnit, logObject);
    }

    for (ActivityCommandUnitMetadata commandUnit : commandUnits) {
      if (commandUnit.getExecutionLogFile() == null) {
        continue;
      }

      zipOutputStream.putNextEntry(new ZipEntry(folderName + "/" + commandUnit.getExecutionLogFile()));
      zipOutputStream.write(commandUnit.getExecutionLogFileContent().getBytes(StandardCharsets.UTF_8));
      zipOutputStream.closeEntry();
      commandUnit.setExecutionLogFileContent(null);

      // Store the log file names.
      Set<String> logFiles = executionLogFilesMap.get(executionId);
      if (logFiles == null) {
        logFiles = new HashSet<>();
      }
      logFiles.add(commandUnit.getExecutionLogFile());
      executionLogFilesMap.put(executionId, logFiles);
    }
  }

  @VisibleForTesting
  List<Log> getAllLogs() {
    // If the dataStoreService used is backed by Google Data Store, we need to have specials logic, because Google Data
    // Store doesn't support the IN filter. So we have to EQ filters in that case and run queries in parallel because
    // spawning a request for each activityId sequentially will be slow.
    if (dataStoreService.supportsInOperator()) {
      return getPageRequestLogs(
          aPageRequest()
              .addFilter(LogKeys.activityId, Operator.IN, activityIdToExecutionDetailsMap.keySet().toArray())
              .addOrder(CreatedAtAware.CREATED_AT_KEY, OrderType.ASC)
              .build());
    } else {
      return getAllLogsGoogleDataStore();
    }
  }

  private List<Log> getAllLogsGoogleDataStore() {
    // Create a completable future for each activityId.
    List<CompletableFuture<List<Log>>> futures = new ArrayList<>();
    for (String activityId : activityIdToExecutionDetailsMap.keySet()) {
      ExecutionMetadata executionMetadata = activityIdToExecutionMap.get(activityId);
      if (executionMetadata == null || executionMetadata.getAppId() == null) {
        continue;
      }

      CompletableFuture<List<Log>> future = CompletableFuture.supplyAsync(
          ()
              -> getPageRequestLogs(aPageRequest()
                                        .addFilter(LogKeys.appId, Operator.EQ, executionMetadata.getAppId())
                                        .addFilter(LogKeys.activityId, Operator.EQ, activityId)
                                        .addOrder(CreatedAtAware.CREATED_AT_KEY, OrderType.ASC)
                                        .build()),
          gdsExecutorService);
      futures.add(future);
    }

    // Wait for all the completable futures to complete.
    CompletableFuture<Void> allFuturesResult = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    return allFuturesResult
        .thenApply(
            v -> futures.stream().map(CompletableFuture::join).flatMap(Collection::stream).collect(Collectors.toList()))
        .join();
  }

  private List<Log> getPageRequestLogs(PageRequest<Log> pageRequest) {
    // We don't want the count as it will increase the number of queries.
    pageRequest.setOptions(Collections.singletonList(PageRequest.Option.LIST));

    List<Log> logs = new ArrayList<>();
    for (int currIdx = 0;; currIdx++) {
      pageRequest.setOffset(String.valueOf(currIdx * LOGS_BATCH_SIZE));
      pageRequest.setLimit(String.valueOf(LOGS_BATCH_SIZE));
      PageResponse<Log> pageResponse = logService.list(CGConstants.GLOBAL_APP_ID, pageRequest);
      if (pageResponse == null || isEmpty(pageResponse.getResponse())) {
        break;
      }

      logs.addAll(pageResponse.getResponse());
      if (pageResponse.getResponse().size() < LOGS_BATCH_SIZE) {
        break;
      }
    }

    return logs;
  }

  private void addLogLine(ActivityCommandUnitMetadata commandUnit, Log logObject) {
    String newLogLine = format("%s   %s   %s%n", logObject.getLogLevel(),
        dateFormat.format(new Date(logObject.getCreatedAt())), logObject.getLogLine());
    if (commandUnit.getExecutionLogFileContent() == null) {
      commandUnit.setExecutionLogFileContent(newLogLine);
    } else {
      commandUnit.setExecutionLogFileContent(commandUnit.getExecutionLogFileContent() + newLogLine);
    }
  }

  private static String prepareLogFileName(
      ExecutionDetailsMetadata executionDetailsMetadata, ActivityCommandUnitMetadata commandUnit) {
    StringBuilder sb = new StringBuilder();
    if (executionDetailsMetadata.getName() != null) {
      sb.append(executionDetailsMetadata.getName()).append('_');
    }
    if (commandUnit.getName() != null) {
      sb.append(commandUnit.getName()).append('_');
    }

    return sb.append(executionDetailsMetadata.getActivityId())
        .append(RandomStringUtils.randomAlphanumeric(4))
        .append(".log")
        .toString();
  }

  @Value
  public static class ActivityIdsVisitor implements GraphNodeVisitor {
    Map<String, ExecutionDetailsMetadata> activityIdToNodeMetadataMap;

    public ActivityIdsVisitor() {
      this.activityIdToNodeMetadataMap = new HashMap<>();
    }

    public void visitGraphNode(GraphNodeMetadata nodeMetadata) {
      addExecutionDetailsMetadata(nodeMetadata);
      if (nodeMetadata != null && isNotEmpty(nodeMetadata.getExecutionHistory())) {
        nodeMetadata.getExecutionHistory().forEach(this::addExecutionDetailsMetadata);
      }
    }

    private void addExecutionDetailsMetadata(ExecutionDetailsMetadata executionDetailsMetadata) {
      if (executionDetailsMetadata != null && executionDetailsMetadata.getActivityId() != null
          && isNotEmpty(executionDetailsMetadata.getSubCommands())) {
        activityIdToNodeMetadataMap.put(executionDetailsMetadata.getActivityId(), executionDetailsMetadata);
      }
    }
  }
}
