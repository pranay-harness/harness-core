/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.core.services.impl.demo;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.cvng.beans.DataCollectionExecutionStatus;
import io.harness.cvng.beans.DataCollectionTaskDTO.DataCollectionTaskResult;
import io.harness.cvng.beans.DataCollectionTaskDTO.DataCollectionTaskResult.DataCollectionTaskResultBuilder;
import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.core.entities.DataCollectionTask;
import io.harness.cvng.core.entities.VerificationTask;
import io.harness.cvng.core.entities.demo.CVNGDemoPerpetualTask;
import io.harness.cvng.core.services.api.CVConfigService;
import io.harness.cvng.core.services.api.DataCollectionTaskService;
import io.harness.cvng.core.services.api.LogRecordService;
import io.harness.cvng.core.services.api.TimeSeriesRecordService;
import io.harness.cvng.core.services.api.VerificationTaskService;
import io.harness.cvng.core.services.api.demo.CVNGDemoPerpetualTaskService;
import io.harness.cvng.models.VerificationType;
import io.harness.persistence.HPersistence;

import com.google.inject.Inject;
import java.io.IOException;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CVNGDemoPerpetualTaskServiceImpl implements CVNGDemoPerpetualTaskService {
  @Inject HPersistence hPersistence;
  @Inject DataCollectionTaskService dataCollectionTaskService;
  @Inject TimeSeriesRecordService timeSeriesRecordService;
  @Inject LogRecordService logRecordService;
  @Inject private VerificationTaskService verificationTaskService;
  @Inject private CVConfigService cvConfigService;

  @Override
  public String createCVNGDemoPerpetualTask(String accountId, String dataCollectionWorkerId) {
    CVNGDemoPerpetualTask cvngDemoPerpetualTask =
        CVNGDemoPerpetualTask.builder().accountId(accountId).dataCollectionWorkerId(dataCollectionWorkerId).build();
    return hPersistence.save(cvngDemoPerpetualTask);
  }

  @Override
  public void execute(CVNGDemoPerpetualTask cvngDemoPerpetualTask) throws Exception {
    while (true) {
      Optional<DataCollectionTask> dataCollectionTask = dataCollectionTaskService.getNextTask(
          cvngDemoPerpetualTask.getAccountId(), cvngDemoPerpetualTask.getDataCollectionWorkerId());
      if (!dataCollectionTask.isPresent()) {
        break;
      }
      DataCollectionTaskResultBuilder dataCollectionTaskResultBuilder =
          DataCollectionTaskResult.builder().dataCollectionTaskId(dataCollectionTask.get().getUuid());
      try {
        saveResults(dataCollectionTask.get());
        dataCollectionTaskResultBuilder.status(DataCollectionExecutionStatus.SUCCESS);
      } catch (Exception e) {
        dataCollectionTaskResultBuilder.status(DataCollectionExecutionStatus.FAILED)
            .stacktrace(e.getStackTrace().toString())
            .exception(e.getMessage());
        log.warn("Demo data perpetual task failed for verificationTaskId"
            + dataCollectionTask.get().getVerificationTaskId() + "  for time frame: "
            + dataCollectionTask.get().getStartTime() + " to " + dataCollectionTask.get().getEndTime()
            + " with exception: " + e.getMessage() + ": stacktrace:" + e.getStackTrace());
        throw e;
      } finally {
        dataCollectionTaskService.updateTaskStatus(dataCollectionTaskResultBuilder.build());
      }
    }
  }

  @Override
  public void deletePerpetualTask(String accountId, String perpetualTaskId) {
    hPersistence.delete(hPersistence.get(CVNGDemoPerpetualTask.class, perpetualTaskId));
  }

  private void saveResults(DataCollectionTask dataCollectionTask) throws IOException {
    String demoTemplateIdentifier = getTemplateIdentifier(dataCollectionTask.getVerificationTaskId());
    if (dataCollectionTask.getDataCollectionInfo().getVerificationType().equals(VerificationType.TIME_SERIES)) {
      timeSeriesRecordService.createDemoAnalysisData(dataCollectionTask.getAccountId(),
          dataCollectionTask.getVerificationTaskId(), dataCollectionTask.getDataCollectionWorkerId(),
          demoTemplateIdentifier, dataCollectionTask.getStartTime(), dataCollectionTask.getEndTime());
    } else {
      logRecordService.createDemoAnalysisData(dataCollectionTask.getAccountId(),
          dataCollectionTask.getVerificationTaskId(), dataCollectionTask.getDataCollectionWorkerId(),
          demoTemplateIdentifier, dataCollectionTask.getStartTime(), dataCollectionTask.getEndTime());
    }
  }

  private String getTemplateIdentifier(String verificationTaskId) {
    VerificationTask verificationTask = verificationTaskService.get(verificationTaskId);
    String template = "default";
    if (verificationTask.getTaskInfo().getTaskType() == VerificationTask.TaskType.LIVE_MONITORING) {
      CVConfig cvConfig =
          cvConfigService.get(((VerificationTask.LiveMonitoringInfo) verificationTask.getTaskInfo()).getCvConfigId());
      // appd_template_demo_dev
      Pattern identifierTemplatePattern = Pattern.compile(".*template_(.*)_dev");
      Matcher matcher = identifierTemplatePattern.matcher(cvConfig.getFullyQualifiedIdentifier());
      if (matcher.matches()) {
        String templateSubstring = matcher.group(1);
        if (isNotEmpty(templateSubstring)) {
          template = templateSubstring;
        }
      }
    }
    return template;
  }
}
