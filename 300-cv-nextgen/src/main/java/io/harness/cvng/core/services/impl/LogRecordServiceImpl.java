/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.core.services.impl;

import static io.harness.persistence.HQuery.excludeAuthority;

import io.harness.cvng.beans.LogRecordDTO;
import io.harness.cvng.core.entities.LogCVConfig;
import io.harness.cvng.core.entities.LogRecord;
import io.harness.cvng.core.entities.LogRecord.LogRecordKeys;
import io.harness.cvng.core.services.api.CVConfigService;
import io.harness.cvng.core.services.api.LogRecordService;
import io.harness.cvng.core.services.api.VerificationTaskService;
import io.harness.cvng.core.services.api.demo.CVNGDemoDataIndexService;
import io.harness.persistence.HPersistence;
import io.harness.serializer.JsonUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.google.inject.Inject;
import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class LogRecordServiceImpl implements LogRecordService {
  @Inject private HPersistence hPersistence;
  @Inject private CVNGDemoDataIndexService cvngDemoDataIndexService;
  @Inject private VerificationTaskService verificationTaskService;
  @Inject private CVConfigService cvConfigService;
  @Override
  public void save(List<LogRecordDTO> logRecords) {
    saveRecords(logRecords.stream().map(this::toLogRecord).collect(Collectors.toList()));
  }

  @Override
  public List<LogRecord> getLogRecords(String verificationTaskId, Instant startTime, Instant endTime) {
    return hPersistence.createQuery(LogRecord.class, excludeAuthority)
        .filter(LogRecordKeys.verificationTaskId, verificationTaskId)
        .field(LogRecordKeys.timestamp)
        .greaterThanOrEq(startTime)
        .field(LogRecordKeys.timestamp)
        .lessThan(endTime)
        .asList();
  }

  private void saveRecords(List<LogRecord> logRecords) {
    hPersistence.save(logRecords);
  }
  private LogRecord toLogRecord(LogRecordDTO logRecordDTO) {
    return LogRecord.builder()
        .accountId(logRecordDTO.getAccountId())
        .verificationTaskId(logRecordDTO.getVerificationTaskId())
        .host(logRecordDTO.getHost())
        .log(logRecordDTO.getLog())
        .timestamp(Instant.ofEpochMilli(logRecordDTO.getTimestamp()))
        .build();
  }

  @Override
  public void createDemoAnalysisData(String accountId, String verificationTaskId, String dataCollectionWorkerId,
      String demoTemplateIdentifier, Instant startTime, Instant endTime) throws IOException {
    List<LogRecordDTO> logRecordsToBeSaved = new ArrayList<>();
    Instant time = startTime;

    String demoTemplatePath = getDemoTemplate(verificationTaskId, endTime, demoTemplateIdentifier);
    List<List<LogRecordDTO>> logRecordsList =
        JsonUtils.asObject(demoTemplatePath, new TypeReference<List<List<LogRecordDTO>>>() {});
    int index = cvngDemoDataIndexService.readIndexForDemoData(accountId, dataCollectionWorkerId, verificationTaskId);
    while (time.compareTo(endTime) < 0) {
      if (index >= logRecordsList.size()) {
        index = index % logRecordsList.size();
      }
      List<LogRecordDTO> logRecordsDTOAtTime = logRecordsList.get(index);
      for (LogRecordDTO logRecordDTO : logRecordsDTOAtTime) {
        logRecordDTO.setAccountId(accountId);
        logRecordDTO.setVerificationTaskId(verificationTaskId);
        logRecordDTO.setTimestamp(time.toEpochMilli());
      }

      logRecordsToBeSaved.addAll(logRecordsDTOAtTime);
      index++;
      time = time.plus(1, ChronoUnit.MINUTES);
    }
    cvngDemoDataIndexService.saveIndexForDemoData(accountId, dataCollectionWorkerId, verificationTaskId, index);
    save(logRecordsToBeSaved);
  }

  private String getDemoTemplate(String verificationTaskId, Instant endTime, String demoTemplateIdentifier)
      throws IOException {
    if (verificationTaskService.isServiceGuardId(verificationTaskId)) {
      LogCVConfig cvConfig =
          (LogCVConfig) cvConfigService.get(verificationTaskService.getCVConfigId(verificationTaskId));
      if (cvConfig.getBaseline().getEndTime().isAfter(endTime)) {
        // use default template if time range is before baseline.
        demoTemplateIdentifier = "default";
      }
    }
    String path = "/io/harness/cvng/analysis/liveMonitoring/logs/$template_demo_template.json";
    path = path.replace("$template", demoTemplateIdentifier);
    return Resources.toString(this.getClass().getResource(path), Charsets.UTF_8);
  }
}
