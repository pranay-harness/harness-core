package io.harness.cvng;

import io.harness.beans.DecryptableEntity;
import io.harness.cvng.beans.DataCollectionRequest;
import io.harness.datacollection.DataCollectionDSLService;
import io.harness.datacollection.entity.RuntimeParameters;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.security.encryption.SecretDecryptionService;
import io.harness.serializer.JsonUtils;

import software.wings.delegatetasks.DelegateLogService;
import software.wings.service.impl.ThirdPartyApiCallLog;
import software.wings.service.intfc.cvng.CVNGDataCollectionDelegateService;

import com.google.inject.Inject;
import java.time.Clock;
import java.time.Instant;
import java.util.List;

public class CVNGDataCollectionDelegateServiceImpl implements CVNGDataCollectionDelegateService {
  @Inject private DataCollectionDSLService dataCollectionDSLService;
  @Inject private SecretDecryptionService secretDecryptionService;
  @Inject private Clock clock;
  @Inject private DelegateLogService delegateLogService;
  @Override
  public String getDataCollectionResult(
      String accountId, DataCollectionRequest dataCollectionRequest, List<EncryptedDataDetail> encryptedDataDetails) {
    if (dataCollectionRequest.getConnectorConfigDTO() instanceof DecryptableEntity) {
      secretDecryptionService.decrypt(
          (DecryptableEntity) dataCollectionRequest.getConnectorConfigDTO(), encryptedDataDetails);
    }
    String dsl = dataCollectionRequest.getDSL();
    Instant now = clock.instant();
    final RuntimeParameters runtimeParameters = RuntimeParameters.builder()
                                                    .baseUrl(dataCollectionRequest.getBaseUrl())
                                                    .commonHeaders(dataCollectionRequest.collectionHeaders())
                                                    .commonOptions(dataCollectionRequest.collectionParams())
                                                    .otherEnvVariables(dataCollectionRequest.getDslEnvVariables())
                                                    .endTime(dataCollectionRequest.getEndTime(now))
                                                    .startTime(dataCollectionRequest.getStartTime(now))
                                                    .build();
    return JsonUtils.asJson(dataCollectionDSLService.execute(dsl, runtimeParameters, callDetails -> {
      final ThirdPartyApiCallLog apiCallLog =
          ThirdPartyApiCallLog.builder().stateExecutionId(dataCollectionRequest.getTracingId()).build();
      apiCallLog.addFieldToRequest(ThirdPartyApiCallLog.ThirdPartyApiCallField.builder()
                                       .name("url")
                                       .type(ThirdPartyApiCallLog.FieldType.URL)
                                       .value(callDetails.getRequest().request().url().toString())
                                       .build());
      apiCallLog.addFieldToResponse(callDetails.getResponse().code(),
          (callDetails.getResponse() != null && callDetails.getResponse().body() != null)
              ? callDetails.getResponse().body()
              : callDetails.getResponse(),
          ThirdPartyApiCallLog.FieldType.JSON);
      delegateLogService.save(accountId, apiCallLog);
    }));
  }
}
