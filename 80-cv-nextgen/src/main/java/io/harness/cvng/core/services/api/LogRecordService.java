package io.harness.cvng.core.services.api;

import io.harness.cvng.core.entities.LogRecord;

import java.util.List;

public interface LogRecordService { void save(List<LogRecord> logRecords); }
