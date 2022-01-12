/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.core.services.impl;

import static io.harness.cvng.CVConstants.TAG_ACCOUNT_ID;
import static io.harness.cvng.CVConstants.TAG_DATA_SOURCE;
import static io.harness.cvng.CVConstants.TAG_ONBOARDING;
import static io.harness.cvng.CVConstants.TAG_UNRECORDED;
import static io.harness.cvng.CVConstants.TAG_VERIFICATION_TYPE;
import static io.harness.cvng.beans.cvnglog.TraceableType.ONBOARDING;
import static io.harness.cvng.beans.cvnglog.TraceableType.VERIFICATION_TASK;
import static io.harness.persistence.HQuery.excludeAuthority;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cvng.beans.CVMonitoringCategory;
import io.harness.cvng.beans.cvnglog.CVNGLogDTO;
import io.harness.cvng.beans.cvnglog.CVNGLogType;
import io.harness.cvng.beans.cvnglog.TraceableType;
import io.harness.cvng.core.entities.CVNGLog;
import io.harness.cvng.core.entities.CVNGLog.CVNGLogKeys;
import io.harness.cvng.core.entities.VerificationTask;
import io.harness.cvng.core.entities.cvnglogs.CVNGLogRecord;
import io.harness.cvng.core.entities.cvnglogs.CVNGLogRecord.CVNGLogRecordComparator;
import io.harness.cvng.core.services.api.CVConfigService;
import io.harness.cvng.core.services.api.CVNGLogService;
import io.harness.cvng.core.services.api.VerificationTaskService;
import io.harness.metrics.service.api.MetricService;
import io.harness.ng.beans.PageResponse;
import io.harness.persistence.HPersistence;
import io.harness.utils.PageUtils;

import com.google.inject.Inject;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.UpdateOptions;
import org.mongodb.morphia.query.Query;

@Slf4j
@OwnedBy(HarnessTeam.CV)
public class CVNGLogServiceImpl implements CVNGLogService {
  @Inject private HPersistence hPersistence;
  @Inject private Clock clock;
  @Inject private CVConfigService cvConfigService;
  @Inject private VerificationTaskService verificationTaskService;
  @Inject private MetricService metricService;

  @Override
  public void save(List<CVNGLogDTO> callLogs) {
    UpdateOptions options = new UpdateOptions();
    options.upsert(true);

    callLogs.forEach(logRecord -> {
      CVNGLogRecord cvngLogRecord = CVNGLog.toCVNGLogRecord(logRecord);
      cvngLogRecord.recordsMetrics(metricService, getTags(logRecord));
      cvngLogRecord.setCreatedAt(clock.instant().toEpochMilli());
      Instant startTime = Instant.ofEpochMilli(logRecord.getStartTime());
      Instant endTime = Instant.ofEpochMilli(logRecord.getEndTime());

      Query<CVNGLog> cvngLogQuery = hPersistence.createQuery(CVNGLog.class)
                                        .filter(CVNGLogKeys.accountId, logRecord.getAccountId())
                                        .filter(CVNGLogKeys.logType, logRecord.getType())
                                        .filter(CVNGLogKeys.traceableType, logRecord.getTraceableType())
                                        .filter(CVNGLogKeys.traceableId, logRecord.getTraceableId())
                                        .filter(CVNGLogKeys.endTime, endTime)
                                        .filter(CVNGLogKeys.startTime, startTime);

      hPersistence.getDatastore(CVNGLog.class)
          .update(cvngLogQuery,
              hPersistence.createUpdateOperations(CVNGLog.class)
                  .setOnInsert(CVNGLogKeys.accountId, logRecord.getAccountId())
                  .setOnInsert(CVNGLogKeys.traceableId, logRecord.getTraceableId())
                  .setOnInsert(CVNGLogKeys.startTime, startTime)
                  .setOnInsert(CVNGLogKeys.endTime, endTime)
                  .setOnInsert(CVNGLogKeys.traceableType, logRecord.getTraceableType())
                  .setOnInsert(CVNGLogKeys.logType, logRecord.getType())
                  .setOnInsert(CVNGLogKeys.validUntil, CVNGLog.builder().build().getValidUntil())
                  .addToSet(CVNGLogKeys.logRecords, cvngLogRecord),
              options);
    });
  }

  @Override
  public PageResponse<CVNGLogDTO> getOnboardingLogs(
      String accountId, String traceableId, CVNGLogType cvngLogType, int offset, int pageSize) {
    List<CVNGLog> cvngLogs = hPersistence.createQuery(CVNGLog.class, excludeAuthority)
                                 .filter(CVNGLogKeys.accountId, accountId)
                                 .filter(CVNGLogKeys.logType, CVNGLogType.API_CALL_LOG)
                                 .filter(CVNGLogKeys.traceableType, TraceableType.ONBOARDING)
                                 .filter(CVNGLogKeys.traceableId, traceableId)
                                 .asList();

    List<CVNGLogDTO> cvngLogDTOs = new ArrayList<>();
    cvngLogs.forEach(log -> {
      Collections.sort(log.getLogRecords(), new CVNGLogRecordComparator());
      cvngLogDTOs.addAll(log.toCVNGLogDTOs());
    });
    return PageUtils.offsetAndLimit(cvngLogDTOs, offset, pageSize);
  }

  @Override
  public PageResponse<CVNGLogDTO> getCVNGLogs(String accountId, String orgIdentifier, String projectIdentifier,
      String serviceIdentifier, String environmentIdentifier, Instant startTime, Instant endTime,
      CVMonitoringCategory monitoringCategory, CVNGLogType cvngLogType, int offset, int pageSize) {
    List<CVNGLog> cvngLogs = hPersistence.createQuery(CVNGLog.class)
                                 .filter(CVNGLogKeys.accountId, accountId)
                                 .filter(CVNGLogKeys.logType, cvngLogType)
                                 .filter(CVNGLogKeys.traceableType, VERIFICATION_TASK)
                                 .field(CVNGLogKeys.traceableId)
                                 .in(cvConfigIds(accountId, orgIdentifier, projectIdentifier, serviceIdentifier,
                                     environmentIdentifier, monitoringCategory))
                                 .field(CVNGLogKeys.endTime)
                                 .greaterThanOrEq(startTime)
                                 .field(CVNGLogKeys.startTime)
                                 .lessThan(endTime)
                                 .asList();
    List<CVNGLogDTO> cvngLogDTOs = new ArrayList<>();
    cvngLogs.forEach(log -> {
      Collections.sort(log.getLogRecords(), new CVNGLogRecordComparator());
      cvngLogDTOs.addAll(log.toCVNGLogDTOs());
    });
    return PageUtils.offsetAndLimit(cvngLogDTOs, offset, pageSize);
  }

  private List<String> cvConfigIds(String accountId, String orgIdentifier, String projectIdentifier,
      String serviceIdentifier, String environmentIdentifier, CVMonitoringCategory monitoringCategory) {
    List<String> cvConfigIds = (cvConfigService.list(accountId, orgIdentifier, projectIdentifier, environmentIdentifier,
                                    serviceIdentifier, monitoringCategory))
                                   .stream()
                                   .map(cvConfig -> cvConfig.getUuid())
                                   .collect(Collectors.toList());
    return verificationTaskService.getServiceGuardVerificationTaskIds(accountId, cvConfigIds);
  }

  private Map<String, String> getTags(CVNGLogDTO cvngLogDTO) {
    Map<String, String> tags = new HashMap<>();
    tags.put(TAG_ACCOUNT_ID, cvngLogDTO.getAccountId());

    if (cvngLogDTO.getType() == CVNGLogType.API_CALL_LOG) {
      if (cvngLogDTO.getTraceableType() == VERIFICATION_TASK) {
        VerificationTask verificationTask = verificationTaskService.get(cvngLogDTO.getTraceableId());
        if (verificationTask != null && verificationTask.getTags() != null) {
          tags.putAll(verificationTask.getTags());
        } else {
          tags.put(TAG_DATA_SOURCE, TAG_UNRECORDED);
          tags.put(TAG_VERIFICATION_TYPE, TAG_UNRECORDED);
        }
      } else if (cvngLogDTO.getTraceableType() == ONBOARDING) {
        tags.put(TAG_DATA_SOURCE, TAG_ONBOARDING);
        tags.put(TAG_VERIFICATION_TYPE, TAG_ONBOARDING);
      }
    }
    return tags;
  }
}
