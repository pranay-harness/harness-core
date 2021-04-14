package io.harness.pms.downloadlogs.mappers;

import io.harness.exception.InvalidArgumentsException;
import io.harness.pms.downloadlogs.beans.entity.DownloadLogsEntity;
import io.harness.pms.downloadlogs.beans.resource.DownloadLogsResponseDTO;
import io.harness.yaml.core.timeout.Timeout;

import java.time.Instant;
import java.util.Date;
import lombok.experimental.UtilityClass;

@UtilityClass
public class DownloadLogsMapper {
  public DownloadLogsResponseDTO toDownloadLogsResponseDTO(String downloadLink, Date validUntil) {
    return DownloadLogsResponseDTO.builder()
        .downloadLink(downloadLink)
        .validUntil(validUntil)
        .estimatedWaitTime(findEstimatedWaitTime(downloadLink))
        .build();
  }

  public DownloadLogsEntity toDownloadLogsEntity(String logKey, String timeToLive, String accountId,
      String orgIdentifier, String projectIdentifier, String pipelineIdentifier) {
    return DownloadLogsEntity.builder()
        .logKey(logKey)
        .validUntil(generateValidUntil(timeToLive))
        .accountId(accountId)
        .orgIdentifier(orgIdentifier)
        .projectIdentifier(projectIdentifier)
        .pipelineIdentifier(pipelineIdentifier)
        .build();
  }

  public DownloadLogsEntity toDownloadLogsEntity(DownloadLogsEntity downloadLogsEntity) {
    return DownloadLogsEntity.builder()
        .logKey(downloadLogsEntity.getLogKey())
        .validUntil(downloadLogsEntity.getValidUntil())
        .accountId(downloadLogsEntity.getAccountId())
        .orgIdentifier(downloadLogsEntity.getOrgIdentifier())
        .projectIdentifier(downloadLogsEntity.getProjectIdentifier())
        .pipelineIdentifier(downloadLogsEntity.getPipelineIdentifier())
        .build();
  }

  private Date generateValidUntil(String timeToLive) {
    Timeout ttlTimeout = Timeout.fromString(timeToLive);
    if (ttlTimeout == null) {
      throw new InvalidArgumentsException("Empty timeout has been given by user");
    }
    long ttlInMillis = ttlTimeout.getTimeoutInMillis();
    return Date.from(Instant.now().plusMillis(ttlInMillis));
  }

  private String findEstimatedWaitTime(String downloadLink) {
    // TODO: Do head call to find wait time
    return "Sample wait time for link to be ready for " + downloadLink;
  }
}