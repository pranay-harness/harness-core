package io.harness.repositories.downloadLogs;

import io.harness.annotation.HarnessRepo;
import io.harness.pms.downloadlogs.beans.entity.DownloadLogsEntity;

import java.util.Optional;
import org.springframework.data.repository.PagingAndSortingRepository;

@HarnessRepo
public interface DownloadLogsRepository extends PagingAndSortingRepository<DownloadLogsEntity, String> {
  Optional<DownloadLogsEntity> findByAccountIdAndLogKey(String accountId, String logKey);
}
