/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.core.services.api;

import io.harness.cvng.core.beans.DSConfig;
import io.harness.cvng.core.beans.MonitoringSourceDTO;
import io.harness.cvng.core.beans.MonitoringSourceImportStatus;
import io.harness.ng.beans.PageResponse;

import java.util.List;

public interface DSConfigService {
  List<DSConfig> list(String accountId, String connectorIdentifier, String productName);
  void create(DSConfig dsConfig);
  void update(String identifier, DSConfig dsConfig);
  void delete(String accountId, String orgIdentifier, String projectIdentifier, String monitoringSourceIdentifier);
  DSConfig getMonitoringSource(String accountId, String orgIdentifier, String projectIdentifier, String identifier);
  PageResponse<MonitoringSourceDTO> listMonitoringSources(
      String accountId, String orgIdentifier, String projectIdentifier, int limit, int offset, String filter);
  List<String> getAvailableMonitoringSources(String accountId, String orgIdentifier, String projectIdentifier);
  MonitoringSourceImportStatus getMonitoringSourceImportStatus(
      String accountId, String orgIdentifier, String projectIdentifier, String identifier);
}
