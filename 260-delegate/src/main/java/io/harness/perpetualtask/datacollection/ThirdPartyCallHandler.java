package io.harness.perpetualtask.datacollection;

import io.harness.datacollection.entity.CallDetails;

import software.wings.delegatetasks.DelegateLogService;
import software.wings.service.impl.ThirdPartyApiCallLog;
import software.wings.service.impl.ThirdPartyApiCallLog.ThirdPartyApiCallField;

import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ThirdPartyCallHandler implements Consumer<CallDetails> {
  private String accountId;
  private String requestUuid;
  private DelegateLogService delegateLogService;

  public ThirdPartyCallHandler(String accountId, String requestUuid, DelegateLogService delegateLogService) {
    this.accountId = accountId;
    this.requestUuid = requestUuid;
    this.delegateLogService = delegateLogService;
  }

  @Override
  public void accept(CallDetails callDetails) {
    final ThirdPartyApiCallLog apiCallLog = ThirdPartyApiCallLog.builder().stateExecutionId(requestUuid).build();
    apiCallLog.addFieldToRequest(ThirdPartyApiCallField.builder()
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
  }
}
