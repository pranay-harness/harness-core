package io.harness.cvng.core.services.api;

import io.harness.cvng.beans.LogRecordDTO;

import java.util.List;

public interface LogRecordService { void save(List<LogRecordDTO> logRecords); }
