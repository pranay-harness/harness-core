/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.migrations.all;

import io.harness.beans.PageRequest;
import io.harness.beans.PageRequest.PageRequestBuilder;
import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import io.harness.migrations.Migration;

import software.wings.api.PhaseElement;
import software.wings.beans.WorkflowExecution;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.MongoDataStoreServiceImpl;
import software.wings.service.impl.analysis.CVFeedbackRecord;
import software.wings.service.impl.analysis.FeedbackAction;
import software.wings.service.impl.analysis.LogMLFeedbackRecord;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.DataStoreService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.sm.ContextElement;
import software.wings.sm.StateExecutionInstance;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ExplodeLogMLFeedbackRecordsMigration implements Migration {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private DataStoreService dataStoreService;
  @Inject private WorkflowExecutionService workflowExecutionService;
  @Inject private AppService appService;

  @Override
  public void migrate() {
    if (dataStoreService instanceof MongoDataStoreServiceImpl) {
      log.info("Datastore service is an instance of MongoDataStoreServiceImpl. Not migrating the records now.");
      return;
    }

    PageRequest<LogMLFeedbackRecord> feedbackRecordPageRequest = PageRequestBuilder.aPageRequest().build();
    List<LogMLFeedbackRecord> logMLFeedbackRecords =
        dataStoreService.list(LogMLFeedbackRecord.class, feedbackRecordPageRequest);

    List<CVFeedbackRecord> cvFeedbackRecords = new ArrayList<>();

    for (LogMLFeedbackRecord record : logMLFeedbackRecords) {
      try {
        cvFeedbackRecords.add(createCVFeedbackRecordFromLogMLFeedback(record));
      } catch (Exception ex) {
        log.info("Exception while creating feedback record for " + record);
      }
    }
    dataStoreService.save(CVFeedbackRecord.class, cvFeedbackRecords, true);

    log.info("Moved {} records to CVFeedback record", cvFeedbackRecords.size());
  }

  private CVFeedbackRecord createCVFeedbackRecordFromLogMLFeedback(LogMLFeedbackRecord record) {
    StateExecutionInstance stateExecutionInstance =
        wingsPersistence.get(StateExecutionInstance.class, record.getStateExecutionId());

    return CVFeedbackRecord.builder()
        .stateExecutionId(record.getStateExecutionId())
        .serviceId(record.getServiceId())
        .envId(getEnvIdForStateExecutionId(record.getAppId(), stateExecutionInstance.getExecutionUuid()))
        .cvConfigId(record.getCvConfigId())
        .actionTaken(FeedbackAction.ADD_TO_BASELINE)
        .clusterLabel(record.getClusterLabel())
        .clusterType(record.getClusterType())
        .analysisMinute(record.getAnalysisMinute())
        .logMessage(record.getLogMessage())
        .createdBy(record.getCreatedBy())
        .lastUpdatedBy(record.getLastUpdatedBy())
        .build();
  }

  private String getEnvIdForStateExecutionId(String appId, String workflowExecutionId) {
    WorkflowExecution execution = workflowExecutionService.getWorkflowExecution(appId, workflowExecutionId);
    if (execution == null) {
      throw new WingsException(
          ErrorCode.GENERAL_ERROR, "This stateExecutionId does not correspond to a valid workflow");
    }
    return execution.getEnvId();
  }

  private String getServiceIdFromStateExecutionInstance(StateExecutionInstance instance) {
    PhaseElement phaseElement = null;
    for (ContextElement element : instance.getContextElements()) {
      if (element instanceof PhaseElement) {
        phaseElement = (PhaseElement) element;
        break;
      }
    }
    if (phaseElement != null) {
      return phaseElement.getServiceElement().getUuid();
    }
    throw new WingsException("There is no serviceID associated with the stateExecutionId: " + instance.getUuid());
  }
}
