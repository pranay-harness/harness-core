package io.harness.cvng.core.services.impl;

import io.harness.cvng.beans.cvnglog.CVNGLogDTO;
import io.harness.cvng.core.entities.CVNGLog;
import io.harness.cvng.core.entities.CVNGLog.CVNGLogKeys;
import io.harness.cvng.core.services.api.CVNGLogService;
import io.harness.persistence.HPersistence;

import com.google.inject.Inject;
import java.time.Clock;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.UpdateOptions;
import org.mongodb.morphia.query.Query;

@Slf4j
public class CVNGLogServiceImpl implements CVNGLogService {
  @Inject private HPersistence hPersistence;
  @Inject private Clock clock;

  @Override
  public void save(List<CVNGLogDTO> callLogs) {
    UpdateOptions options = new UpdateOptions();
    options.upsert(true);

    callLogs.forEach(logRecord -> {
      Query<CVNGLog> cvngLogQuery = hPersistence.createQuery(CVNGLog.class)
                                        .filter(CVNGLogKeys.accountId, logRecord.getAccountId())
                                        .filter(CVNGLogKeys.logType, logRecord.getType())
                                        .filter(CVNGLogKeys.traceableType, logRecord.getTraceableType())
                                        .filter(CVNGLogKeys.traceableId, logRecord.getTraceableId())
                                        .filter(CVNGLogKeys.endTime, logRecord.getStartTime())
                                        .filter(CVNGLogKeys.startTime, logRecord.getEndTime());

      hPersistence.getDatastore(CVNGLog.class)
          .update(cvngLogQuery,
              hPersistence.createUpdateOperations(CVNGLog.class)
                  .setOnInsert(CVNGLogKeys.accountId, logRecord.getAccountId())
                  .setOnInsert(CVNGLogKeys.traceableId, logRecord.getTraceableId())
                  .setOnInsert(CVNGLogKeys.startTime, logRecord.getStartTime())
                  .setOnInsert(CVNGLogKeys.endTime, logRecord.getEndTime())
                  .setOnInsert(CVNGLogKeys.traceableType, logRecord.getTraceableType())
                  .setOnInsert(CVNGLogKeys.logType, logRecord.getType())
                  .setOnInsert(CVNGLogKeys.createdAt, clock.instant().toEpochMilli())
                  .setOnInsert(CVNGLogKeys.lastUpdatedAt, clock.instant().toEpochMilli())
                  .setOnInsert(CVNGLogKeys.validUntil, CVNGLog.builder().build().getValidUntil())
                  .addToSet(CVNGLogKeys.logRecords, CVNGLog.toCVNGLogRecord(logRecord)),
              options);
    });
  }
}
