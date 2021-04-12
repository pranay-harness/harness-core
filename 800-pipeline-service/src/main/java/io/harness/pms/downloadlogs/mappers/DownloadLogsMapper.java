package io.harness.pms.downloadlogs.mappers;

import io.harness.pms.downloadlogs.beans.entity.DownloadLogsEntity;
import io.harness.pms.downloadlogs.beans.resource.DownloadLogsResponseDTO;
import io.harness.yaml.core.timeout.Timeout;

import java.util.Date;
import lombok.experimental.UtilityClass;

@UtilityClass
public class DownloadLogsMapper {
  public DownloadLogsResponseDTO toDownloadLogsResponseDTO(String downloadLink, Date validUntil) {
    return DownloadLogsResponseDTO.builder()
        .downloadLink(downloadLink)
        .validUntil(validUntil)
        .waitTime(findWaitTime(downloadLink))
        .build();
  }

  public DownloadLogsEntity toDownloadLogsEntity(String logKey, Date createdAt, String timeToLive, String accountId,
      String orgIdentifier, String projectIdentifier, String pipelineIdentifier) {
    return DownloadLogsEntity.builder()
        .logKey(logKey)
        .validUntil(generateValidUntil(createdAt, timeToLive))
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

  private Date generateValidUntil(Date createdAt, String timeToLive) {
    long ttlInMillis = Timeout.fromString(timeToLive).getTimeoutInMillis();
    Date validUntil = new Date();
    validUntil.setTime(createdAt.getTime() + ttlInMillis);
    return validUntil;
  }

  private String findWaitTime(String downloadLink) {
    // TODO: Do head call to find wait time
    return "Sample wait time for link to be ready for " + downloadLink;
  }
}