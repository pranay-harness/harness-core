package io.harness.pms.downloadlogs.service;

import static java.lang.String.format;

import io.harness.exception.DuplicateFieldException;
import io.harness.exception.InvalidRequestException;
import io.harness.pms.downloadlogs.beans.entity.DownloadLogsEntity;
import io.harness.repositories.downloadLogs.DownloadLogsRepository;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;

@Singleton
@Slf4j
public class DownloadLogsServiceImpl implements DownloadLogsService {
  @Inject private DownloadLogsRepository downloadLogsRepository;

  private static final String DUP_KEY_EXP_FORMAT_STRING =
      "Download link for log key  [%s] under Account [%s] already exists";

  @Override
  public DownloadLogsEntity create(DownloadLogsEntity downloadLogsEntity) {
    try {
      return downloadLogsRepository.save(downloadLogsEntity);
    } catch (DuplicateKeyException ex) {
      throw new DuplicateFieldException(
          format(DUP_KEY_EXP_FORMAT_STRING, downloadLogsEntity.getLogKey(), downloadLogsEntity.getAccountId()));
    }
  }

  @Override
  public DownloadLogsEntity get(String accountId, String logKey) {
    Optional<DownloadLogsEntity> downloadLogsEntityOptional =
        downloadLogsRepository.findByAccountIdAndLogKey(accountId, logKey);
    if (downloadLogsEntityOptional.isPresent()) {
      return downloadLogsEntityOptional.get();
    }
    throw new InvalidRequestException("No download link exists for given log key. Please generate a new one");
  }

  @Override
  public DownloadLogsEntity getByDownloadId(String downloadId) {
    Optional<DownloadLogsEntity> downloadLogsEntityOptional = downloadLogsRepository.findById(downloadId);
    if (downloadLogsEntityOptional.isPresent()) {
      return downloadLogsEntityOptional.get();
    }
    throw new InvalidRequestException("No download link exists for given downloadId. Please generate a new one");
  }
}
