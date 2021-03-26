package io.harness.perpetualtask.datacollection;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.cvng.beans.cvnglog.ApiCallLogDTO;
import io.harness.cvng.beans.cvnglog.ApiCallLogDTO.ApiCallLogDTOField;
import io.harness.cvng.beans.cvnglog.TraceableType;
import io.harness.datacollection.entity.CallDetails;

import software.wings.delegatetasks.DelegateLogService;
import software.wings.service.impl.ThirdPartyApiCallLog;
import software.wings.service.impl.ThirdPartyApiCallLog.ThirdPartyApiCallField;

import com.hazelcast.util.Preconditions;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@TargetModule(HarnessModule._930_DELEGATE_TASKS)
public class ThirdPartyCallHandler implements Consumer<CallDetails> {
  private String accountId;
  private String requestUuid;
  private DelegateLogService delegateLogService;
  private Instant startTime;
  private Instant endTime;

  public ThirdPartyCallHandler(
      String accountId, String requestUuid, DelegateLogService delegateLogService, Instant startTime, Instant endTime) {
    this.accountId = accountId;
    this.requestUuid = requestUuid;
    this.delegateLogService = delegateLogService;
    this.startTime = startTime;
    this.endTime = endTime;
  }

  @Override
  public void accept(CallDetails callDetails) {
    Preconditions.checkNotNull(callDetails.getRequest(), "Call Details request is null.");
    final ThirdPartyApiCallLog apiCallLog = ThirdPartyApiCallLog.builder().stateExecutionId(requestUuid).build();
    apiCallLog.addFieldToRequest(ThirdPartyApiCallField.builder()
                                     .name("url")
                                     .type(ThirdPartyApiCallLog.FieldType.URL)
                                     .value(callDetails.getRequest().request().url().toString())
                                     .build());
    if (callDetails.getResponse() != null) {
      apiCallLog.addFieldToResponse(
          callDetails.getResponse().code(), callDetails.getResponse().body(), ThirdPartyApiCallLog.FieldType.JSON);
    }

    delegateLogService.save(accountId, apiCallLog);

    final ApiCallLogDTO cvngLogDTO = ApiCallLogDTO.builder()
                                         .traceableId(requestUuid)
                                         .traceableType(TraceableType.VERIFICATION_TASK)
                                         .accountId(accountId)
                                         .startTime(startTime)
                                         .endTime(endTime)
                                         .requestTime(OffsetDateTime.now().toInstant())
                                         .responseTime(OffsetDateTime.now().toInstant())
                                         .build();
    cvngLogDTO.addFieldToRequest(ApiCallLogDTOField.builder()
                                     .name("url")
                                     .type(ApiCallLogDTO.FieldType.URL)
                                     .value(callDetails.getRequest().request().url().toString())
                                     .build());

    if (callDetails.getResponse() != null) {
      cvngLogDTO.addFieldToResponse(
          callDetails.getResponse().code(), callDetails.getResponse().body(), ApiCallLogDTO.FieldType.JSON);
    }

    delegateLogService.save(accountId, cvngLogDTO);
  }
}
