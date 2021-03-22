package io.harness.cvng;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.DecryptableEntity;
import io.harness.cvng.beans.cvnglog.ApiCallLogDTO;
import io.harness.cvng.beans.cvnglog.ApiCallLogDTO.ApiCallLogDTOField;
import io.harness.cvng.beans.cvnglog.TraceableType;
import io.harness.datacollection.DataCollectionDSLService;
import io.harness.datacollection.entity.RuntimeParameters;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.security.encryption.SecretDecryptionService;
import io.harness.serializer.JsonUtils;

import software.wings.delegatetasks.DelegateLogService;
import software.wings.service.intfc.cvng.CVNGDataCollectionDelegateService;

import com.google.inject.Inject;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;

@TargetModule(Module._420_DELEGATE_AGENT)
public class CVNGDataCollectionDelegateServiceImpl implements CVNGDataCollectionDelegateService {
  @Inject private DataCollectionDSLService dataCollectionDSLService;
  @Inject private SecretDecryptionService secretDecryptionService;
  @Inject private Clock clock;
  @Inject private DelegateLogService delegateLogService;
  @Override
  public String getDataCollectionResult(
      String accountId, DataCollectionRequest dataCollectionRequest, List<EncryptedDataDetail> encryptedDataDetails) {
    if (isNotEmpty(dataCollectionRequest.getConnectorConfigDTO().getDecryptableEntities())) {
      DecryptableEntity decryptableEntity =
          dataCollectionRequest.getConnectorConfigDTO().getDecryptableEntities().get(0);
      secretDecryptionService.decrypt(decryptableEntity, encryptedDataDetails);
    }
    String dsl = dataCollectionRequest.getDSL();
    Instant now = clock.instant();
    final RuntimeParameters runtimeParameters = RuntimeParameters.builder()
                                                    .baseUrl(dataCollectionRequest.getBaseUrl())
                                                    .commonHeaders(dataCollectionRequest.collectionHeaders())
                                                    .commonOptions(dataCollectionRequest.collectionParams())
                                                    .otherEnvVariables(dataCollectionRequest.fetchDslEnvVariables())
                                                    .endTime(dataCollectionRequest.getEndTime(now))
                                                    .startTime(dataCollectionRequest.getStartTime(now))
                                                    .build();
    return JsonUtils.asJson(dataCollectionDSLService.execute(dsl, runtimeParameters, callDetails -> {
      // TODO: write unit test case for this lambda expression.
      if (dataCollectionRequest.getTracingId() != null) {
        final ApiCallLogDTO cvngLogDTO = ApiCallLogDTO.builder()
                                             .traceableId(dataCollectionRequest.getTracingId())
                                             .traceableType(TraceableType.ONBOARDING)
                                             .accountId(accountId)
                                             .startTime(dataCollectionRequest.getStartTime(now))
                                             .endTime(dataCollectionRequest.getEndTime(now))
                                             .requestTime(OffsetDateTime.now().toInstant())
                                             .responseTime(OffsetDateTime.now().toInstant())
                                             .build();
        cvngLogDTO.addFieldToRequest(ApiCallLogDTOField.builder()
                                         .name("url")
                                         .type(ApiCallLogDTO.FieldType.URL)
                                         .value(callDetails.getRequest().request().url().toString())
                                         .build());

        cvngLogDTO.addFieldToResponse(callDetails.getResponse().code(),
            (callDetails.getResponse() != null && callDetails.getResponse().body() != null)
                ? callDetails.getResponse().body()
                : callDetails.getResponse(),
            ApiCallLogDTO.FieldType.JSON);
        delegateLogService.save(accountId, cvngLogDTO);
      }
    }));
  }
}
