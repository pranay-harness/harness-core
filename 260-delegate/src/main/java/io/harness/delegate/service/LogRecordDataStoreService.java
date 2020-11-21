package io.harness.delegate.service;

import io.harness.cvng.beans.LogRecordDTO;
import io.harness.datacollection.entity.LogDataRecord;
import io.harness.rest.RestResponse;
import io.harness.verificationclient.CVNextGenServiceClient;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import retrofit2.Response;

@Singleton
@Slf4j
public class LogRecordDataStoreService {
  @Inject private CVNextGenServiceClient cvNextGenServiceClient;

  public void save(String accountId, String verificationTaskId, List<LogDataRecord> logRecords) {
    // TODO: find a way to implement retry and time limiting. The timeout should be implemented using retrofit config
    //  and not using TimeLimiter class as it has potential for memory leak. Also the exceptions needs to propagate from
    //  cv-nextgen to delegate.
    try {
      Response<RestResponse<Void>> response =
          cvNextGenServiceClient
              .saveLogRecords(accountId,
                  logRecords.stream()
                      .map(logDataRecord -> toLogRecordDTO(accountId, verificationTaskId, logDataRecord))
                      .collect(Collectors.toList()))
              .execute();
      if (!response.isSuccessful()) {
        throw new IllegalStateException("Request not successful");
      }
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  private LogRecordDTO toLogRecordDTO(String accountId, String verificationTaskId, LogDataRecord logDataRecord) {
    return LogRecordDTO.builder()
        .accountId(accountId)
        .host(logDataRecord.getHostname())
        .verificationTaskId(verificationTaskId)
        .timestamp(logDataRecord.getTimestamp())
        .log(logDataRecord.getLog())
        .build();
  }
}
