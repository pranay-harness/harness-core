/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.core.services.api;

import io.harness.cvng.beans.CVMonitoringCategory;
import io.harness.cvng.beans.cvnglog.CVNGLogDTO;
import io.harness.cvng.beans.cvnglog.CVNGLogType;
import io.harness.ng.beans.PageResponse;

import java.time.Instant;
import java.util.List;

public interface CVNGLogService {
  void save(List<CVNGLogDTO> callLogs);
  PageResponse<CVNGLogDTO> getOnboardingLogs(
      String accountId, String traceableId, CVNGLogType cvngLogType, int offset, int pageSize);

  PageResponse<CVNGLogDTO> getCVNGLogs(String accountId, String orgIdentifier, String projectIdentifier,
      String serviceIdentifier, String environmentIdentifier, Instant startTime, Instant endTime,
      CVMonitoringCategory monitoringCategory, CVNGLogType cvngLogType, int offset, int pageSize);
}
