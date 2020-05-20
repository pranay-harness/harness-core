package io.harness.execution.export.processor;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.lang.String.format;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;

import com.google.inject.Inject;

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
import lombok.Setter;
import lombok.Value;
import lombok.experimental.NonFinal;
import org.apache.commons.lang3.RandomStringUtils;
import software.wings.beans.Application;
import software.wings.beans.Log;
import software.wings.beans.Log.LogKeys;
import software.wings.service.impl.LogServiceImpl;
import software.wings.service.intfc.LogService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@OwnedBy(CDC)
@Value
public class ActivityLogsProcessor implements ExportExecutionsProcessor {
  private static final int LOGS_BATCH_SIZE = 500;

  @Inject @NonFinal @Setter LogService logService;

  Map<String, ExecutionDetailsMetadata> activityIdToExecutionDetailsMap;
  Map<String, String> activityIdToExecutionIdMap;
  ZipOutputStream zipOutputStream;
  Map<String, String> folderNamesMap;
  Map<String, Set<String>> executionLogFilesMap;

  SimpleDateFormat dateFormat;

  public ActivityLogsProcessor(ZipOutputStream zipOutputStream, Map<String, String> folderNamesMap,
      Map<String, Set<String>> executionLogFilesMap) {
    this.activityIdToExecutionDetailsMap = new HashMap<>();
    this.activityIdToExecutionIdMap = new HashMap<>();
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
    activityIdToExecutionIdMap.putAll(newActivityIdToNodeMetadataMap.keySet().stream().collect(
        toMap(Function.identity(), ignored -> executionMetadata.getId())));
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
        String executionId = activityIdToExecutionIdMap.get(activityId);
        if (executionId == null) {
          continue;
        }

        updateExecutionDetailsMetadata(
            activityIdToExecutionDetailsMap.get(entry.getKey()), entry.getValue(), executionId);
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
    for (Log log : logs) {
      ActivityCommandUnitMetadata commandUnit = commandUnitMap.getOrDefault(log.getCommandUnitName(), null);
      if (commandUnit == null) {
        continue;
      }

      if (commandUnit.getExecutionLogFile() == null) {
        commandUnit.setExecutionLogFile(prepareLogFileName(executionDetailsMetadata, commandUnit));
      }
      addLogLine(commandUnit, log);
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

  private List<Log> getAllLogs() {
    PageRequest<Log> pageRequest =
        aPageRequest()
            .addFilter(LogKeys.activityId, Operator.IN, activityIdToExecutionDetailsMap.keySet().toArray())
            .addOrder(CreatedAtAware.CREATED_AT_KEY, OrderType.DESC)
            .build();

    // We don't want the count as it will increase the number of queries.
    pageRequest.setOptions(Collections.singletonList(PageRequest.Option.LIST));

    List<Log> logs = new ArrayList<>();
    for (int currIdx = 0;; currIdx++) {
      pageRequest.setOffset(String.valueOf(currIdx * LOGS_BATCH_SIZE));
      pageRequest.setLimit(String.valueOf(LOGS_BATCH_SIZE));
      PageResponse<Log> pageResponse = logService.list(Application.GLOBAL_APP_ID, pageRequest);
      if (pageResponse == null || isEmpty(pageResponse.getResponse())) {
        break;
      }

      logs.addAll(pageResponse.getResponse());
    }

    return logs;
  }

  private void addLogLine(ActivityCommandUnitMetadata commandUnit, Log log) {
    String newLogLine =
        format("%s   %s   %s%n", log.getLogLevel(), dateFormat.format(new Date(log.getCreatedAt())), log.getLogLine());
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
        nodeMetadata.getExecutionHistory().forEach(this ::addExecutionDetailsMetadata);
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
