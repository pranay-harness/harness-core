package io.harness.cvng.core.services.impl;

import io.harness.cvng.beans.cvnglog.CVNGLogDTO;
import io.harness.cvng.core.entities.ApiCallLog;
import io.harness.cvng.core.entities.CVNGLog;
import io.harness.cvng.core.services.api.CVNGLogService;
import io.harness.persistence.HPersistence;

import com.google.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CVNGLogServiceImpl implements CVNGLogService {
  @Inject private HPersistence hPersistence;

  @Override
  public void save(List<CVNGLogDTO> callLogs) {
    hPersistence.save(callLogs.stream().map(this::toCVNGLog).collect(Collectors.toList()));
  }

  private CVNGLog toCVNGLog(CVNGLogDTO logRecordDTO) {
    switch (logRecordDTO.getType()) {
      case API_CALL_LOG:
        return ApiCallLog.toApiCallLog(logRecordDTO);
      case EXECUTION_LOG:
        throw new UnsupportedOperationException("Log Type: Execution Log. Type not implemented.");
      default:
        throw new IllegalStateException("Log type: NULL. Cannot be null. Skipping the save.");
    }
  }
}
