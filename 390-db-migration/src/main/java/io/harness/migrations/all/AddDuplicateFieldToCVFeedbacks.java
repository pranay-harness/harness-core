/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.migrations.all;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.beans.PageRequest;
import io.harness.beans.PageRequest.PageRequestBuilder;
import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import io.harness.migrations.Migration;

import software.wings.beans.WorkflowExecution;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.GoogleDataStoreServiceImpl;
import software.wings.service.impl.analysis.AnalysisServiceImpl.LogMLFeedbackType;
import software.wings.service.impl.analysis.CVFeedbackRecord;
import software.wings.service.impl.analysis.FeedbackAction;
import software.wings.service.impl.analysis.LogMLFeedbackRecord;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.DataStoreService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.sm.StateExecutionInstance;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AddDuplicateFieldToCVFeedbacks implements Migration {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private DataStoreService dataStoreService;
  @Inject private AppService appService;
  @Inject private WorkflowExecutionService workflowExecutionService;
  @Inject private EnvironmentService environmentService;

  private static final String HARNESS_ACCOUNT = "wFHXHD0RRQWoO8tIZT5YVw";
  @Override
  public void migrate() {
    if (dataStoreService instanceof GoogleDataStoreServiceImpl) {
      deleteAllNonHarnessFeedbacks();

      PageRequest<LogMLFeedbackRecord> feedbackRecordPageRequest = PageRequestBuilder.aPageRequest().build();
      List<LogMLFeedbackRecord> logMLFeedbackRecords =
          dataStoreService.list(LogMLFeedbackRecord.class, feedbackRecordPageRequest);

      List<CVFeedbackRecord> cvFeedbackRecords = new ArrayList<>();

      for (LogMLFeedbackRecord record : logMLFeedbackRecords) {
        try {
          cvFeedbackRecords.addAll(createCVFeedbackRecordFromLogMLFeedback(record));
        } catch (Exception ex) {
          log.info("Exception while creating feedback record for " + record);
        }
      }
      dataStoreService.save(CVFeedbackRecord.class, cvFeedbackRecords, true);

      log.info("Moved {} records to CVFeedback record", cvFeedbackRecords.size());
    }
  }

  private String getEnvIdForStateExecutionId(String appId, String workflowExecutionId) {
    WorkflowExecution execution = workflowExecutionService.getWorkflowExecution(appId, workflowExecutionId);
    if (execution == null) {
      throw new WingsException(
          ErrorCode.GENERAL_ERROR, "This stateExecutionId does not correspond to a valid workflow");
    }
    return execution.getEnvId();
  }

  private void deleteAllNonHarnessFeedbacks() {
    int numDeleted = 0;
    PageRequest<CVFeedbackRecord> feedbackRecordPageRequest = PageRequestBuilder.aPageRequest().build();
    List<CVFeedbackRecord> feedbackRecords = dataStoreService.list(CVFeedbackRecord.class, feedbackRecordPageRequest);
    feedbackRecords.forEach(cvFeedbackRecord -> {
      if (isNotEmpty(cvFeedbackRecord.getAccountId()) && !cvFeedbackRecord.getAccountId().equals(HARNESS_ACCOUNT)) {
        dataStoreService.delete(CVFeedbackRecord.class, cvFeedbackRecord.getUuid());
      }
    });
  }

  private List<CVFeedbackRecord> createCVFeedbackRecordFromLogMLFeedback(LogMLFeedbackRecord record) {
    StateExecutionInstance stateExecutionInstance =
        wingsPersistence.get(StateExecutionInstance.class, record.getStateExecutionId());

    String accountId = appService.getAccountIdByAppId(record.getAppId());
    String envId = getEnvIdForStateExecutionId(record.getAppId(), stateExecutionInstance.getExecutionUuid());
    List<CVFeedbackRecord> recordList = new ArrayList<>();
    recordList.add(CVFeedbackRecord.builder()
                       .accountId(accountId)
                       .stateExecutionId(record.getStateExecutionId())
                       .serviceId(record.getServiceId())
                       .envId(envId)
                       .cvConfigId(record.getCvConfigId())
                       .actionTaken(FeedbackAction.ADD_TO_BASELINE)
                       .clusterLabel(record.getClusterLabel())
                       .clusterType(record.getClusterType())
                       .analysisMinute(record.getAnalysisMinute())
                       .logMessage(record.getLogMessage())
                       .createdBy(record.getCreatedBy())
                       .lastUpdatedBy(record.getLastUpdatedBy())
                       .isDuplicate(false)
                       .build());

    if (record.getLogMLFeedbackType() == LogMLFeedbackType.IGNORE_SERVICE
        || record.getLogMLFeedbackType() == LogMLFeedbackType.IGNORE_ALWAYS) {
      // get all envs for the app and save those as duplicates
      List<String> envIds = environmentService.getEnvIdsByApp(record.getAppId());
      envIds.remove(envId);
      if (isNotEmpty(envIds)) {
        envIds.forEach(environment -> {
          recordList.add(CVFeedbackRecord.builder()
                             .accountId(accountId)
                             .stateExecutionId(record.getStateExecutionId())
                             .serviceId(record.getServiceId())
                             .envId(environment)
                             .isDuplicate(true)
                             .cvConfigId(record.getCvConfigId())
                             .actionTaken(FeedbackAction.ADD_TO_BASELINE)
                             .clusterLabel(record.getClusterLabel())
                             .clusterType(record.getClusterType())
                             .analysisMinute(record.getAnalysisMinute())
                             .logMessage(record.getLogMessage())
                             .createdBy(record.getCreatedBy())
                             .lastUpdatedBy(record.getLastUpdatedBy())
                             .build());
        });
      }
    }

    return recordList;
  }
}
