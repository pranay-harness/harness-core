package io.harness.pms.downloadlogs.service;

import static java.lang.String.format;

import io.harness.PipelineServiceConfiguration;
import io.harness.exception.InvalidRequestException;
import io.harness.pms.downloadlogs.beans.entity.DownloadLogsEntity;
import io.harness.repositories.downloadLogs.DownloadLogsRepository;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
public class DownloadLogsServiceImpl implements DownloadLogsService {
  @Inject private DownloadLogsRepository downloadLogsRepository;
  @Inject private PipelineServiceConfiguration pipelineServiceConfiguration;

  private static final String LOG_KEY_STARTING_FORMAT_STRING = "accountId:%s/orgId:%s/projectId:%s/pipelineId:%s/";

  private static final String DOWNLOAD_LINK_FORMAT_STRING = "%s/api/downloadLogs/%s";

  @Override
  public DownloadLogsEntity create(DownloadLogsEntity downloadLogsEntity) {
    Optional<DownloadLogsEntity> downloadLogsEntityOptional =
        downloadLogsRepository.findByLogKey(downloadLogsEntity.getLogKey());
    return downloadLogsEntityOptional.orElseGet(() -> downloadLogsRepository.save(downloadLogsEntity));
  }

  @Override
  public DownloadLogsEntity get(String accountId, String logKey) {
    Optional<DownloadLogsEntity> downloadLogsEntityOptional = downloadLogsRepository.findByLogKey(logKey);
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

  @Override
  public void verifyLogKey(String logKey, String accountId, String orgId, String projectId, String pipelineId) {
    String expectedLogKeyStarting = format(LOG_KEY_STARTING_FORMAT_STRING, accountId, orgId, projectId, pipelineId);
    if (!logKey.startsWith(expectedLogKeyStarting)) {
      throw new InvalidRequestException("The log key given by user does not match the access credentials");
    }
  }

  @Override
  public String generateDownloadURL(String downloadId) {
    return format(DOWNLOAD_LINK_FORMAT_STRING, pipelineServiceConfiguration.getPipelineServiceBaseUrl(), downloadId);
  }
}
