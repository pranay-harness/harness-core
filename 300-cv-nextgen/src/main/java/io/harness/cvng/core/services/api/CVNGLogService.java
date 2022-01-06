/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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
