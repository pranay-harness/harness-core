package io.harness.pms.downloadlogs.service;

import io.harness.pms.downloadlogs.beans.entity.DownloadLogsEntity;

public interface DownloadLogsService {
  DownloadLogsEntity create(DownloadLogsEntity downloadLogsEntity);

  DownloadLogsEntity get(String accountId, String logKey);

  DownloadLogsEntity getByDownloadId(String downloadId);

  void verifyLogKey(String logKey, String accountId, String orgId, String projectId, String pipelineId);
}
