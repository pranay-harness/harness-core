package io.harness.cvng.core.services.impl;

import static io.harness.persistence.HQuery.excludeAuthority;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cvng.beans.CVMonitoringCategory;
import io.harness.cvng.beans.cvnglog.CVNGLogDTO;
import io.harness.cvng.beans.cvnglog.CVNGLogType;
import io.harness.cvng.beans.cvnglog.TraceableType;
import io.harness.cvng.core.entities.CVNGLog;
import io.harness.cvng.core.entities.CVNGLog.CVNGLogKeys;
import io.harness.cvng.core.entities.cvnglogs.CVNGLogRecord;
import io.harness.cvng.core.entities.cvnglogs.CVNGLogRecord.CVNGLogRecordComparator;
import io.harness.cvng.core.services.api.CVConfigService;
import io.harness.cvng.core.services.api.CVNGLogService;
import io.harness.ng.beans.PageResponse;
import io.harness.persistence.HPersistence;
import io.harness.utils.PageUtils;

import com.google.inject.Inject;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
  @Inject private VerificationTaskServiceImpl verificationTaskService;

  @Override
  public void save(List<CVNGLogDTO> callLogs) {
    UpdateOptions options = new UpdateOptions();
    options.upsert(true);

    callLogs.forEach(logRecord -> {
      CVNGLogRecord cvngLogRecord = CVNGLog.toCVNGLogRecord(logRecord);
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
                                 .filter(CVNGLogKeys.traceableType, TraceableType.VERIFICATION_TASK)
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
}
