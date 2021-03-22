package io.harness.delegate.task.cvng;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.beans.DecryptableEntity;
import io.harness.cvng.DataCollectionRequest;
import io.harness.cvng.OnboardingTaskParameters;
import io.harness.cvng.OnboardingTaskResponse;
import io.harness.datacollection.DataCollectionDSLService;
import io.harness.datacollection.entity.RuntimeParameters;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.task.AbstractDelegateRunnableTask;
import io.harness.delegate.task.TaskParameters;
import io.harness.security.encryption.SecretDecryptionService;
import io.harness.serializer.JsonUtils;

import com.google.inject.Inject;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import org.apache.commons.lang3.NotImplementedException;

public class OnboardingTaskNG extends AbstractDelegateRunnableTask {
  @Inject private SecretDecryptionService secretDecryptionService;
  @Inject private Clock clock;
  //  @Inject private DelegateLogService delegateLogService;
  @Inject private DataCollectionDSLService dataCollectionDSLService;

  public OnboardingTaskNG(DelegateTaskPackage delegateTaskPackage, ILogStreamingTaskClient logStreamingTaskClient,
      Consumer<DelegateTaskResponse> consumer, BooleanSupplier preExecute) {
    super(delegateTaskPackage, logStreamingTaskClient, consumer, preExecute);
  }

  @Override
  public DelegateResponseData run(Object[] parameters) {
    throw new NotImplementedException("not implemented");
  }

  @Override
  public DelegateResponseData run(TaskParameters parameters) {
    OnboardingTaskParameters onboardingTaskParameters = (OnboardingTaskParameters) parameters;
    DataCollectionRequest dataCollectionRequest = onboardingTaskParameters.getDataCollectionRequest();
    List<DecryptableEntity> decryptableEntities =
        dataCollectionRequest.getConnectorConfigDTO().getDecryptableEntities();
    if (isNotEmpty(decryptableEntities)) {
      decryptableEntities.forEach(decryptableEntity
          -> secretDecryptionService.decrypt(decryptableEntity, onboardingTaskParameters.getEncryptedDataDetails()));
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
    String jsonResponse = JsonUtils.asJson(dataCollectionDSLService.execute(dsl, runtimeParameters,
        callDetails
        -> {
            //      if (dataCollectionRequest.getTracingId() != null) {
            //        final ApiCallLogDTO cvngLogDTO = ApiCallLogDTO.builder()
            //                                             .traceableId(dataCollectionRequest.getTracingId())
            //                                             .traceableType(TraceableType.ONBOARDING)
            //                                             .accountId(onboardingTaskParameters.getAccountId())
            //                                             .startTime(dataCollectionRequest.getStartTime(now))
            //                                             .endTime(dataCollectionRequest.getEndTime(now))
            //                                             .requestTime(OffsetDateTime.now().toInstant())
            //                                             .responseTime(OffsetDateTime.now().toInstant())
            //                                             .build();
            //        cvngLogDTO.addFieldToRequest(ApiCallLogDTO.ApiCallLogDTOField.builder()
            //                                         .name("url")
            //                                         .type(ApiCallLogDTO.FieldType.URL)
            //                                         .value(callDetails.getRequest().request().url().toString())
            //                                         .build());
            //
            //        cvngLogDTO.addFieldToResponse(callDetails.getResponse().code(),
            //            (callDetails.getResponse() != null && callDetails.getResponse().body() != null)
            //                ? callDetails.getResponse().body()
            //                : callDetails.getResponse(),
            //            ApiCallLogDTO.FieldType.JSON);
            //        //        delegateLogService.save(onboardingTaskParameters.getAccountId(), cvngLogDTO);
            //      }
        }));
    return OnboardingTaskResponse.builder().jsonResponse(jsonResponse).build();
  }
}
