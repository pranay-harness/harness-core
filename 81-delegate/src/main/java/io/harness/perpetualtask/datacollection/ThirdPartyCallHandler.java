package io.harness.perpetualtask.datacollection;

import io.harness.datacollection.entity.CallDetails;
import software.wings.delegatetasks.DelegateLogService;
import software.wings.service.impl.ThirdPartyApiCallLog;
import software.wings.service.impl.ThirdPartyApiCallLog.ThirdPartyApiCallField;

import java.util.function.Consumer;

public class ThirdPartyCallHandler implements Consumer<CallDetails> {
  private String accountId;
  private String cvConfigId;
  private DelegateLogService delegateLogService;

  public ThirdPartyCallHandler(String accountId, String cvConfigId, DelegateLogService delegateLogService) {
    this.accountId = accountId;
    this.cvConfigId = cvConfigId;
    this.delegateLogService = delegateLogService;
  }

  @Override
  public void accept(CallDetails callDetails) {
    final ThirdPartyApiCallLog apiCallLog = ThirdPartyApiCallLog.builder().stateExecutionId(cvConfigId).build();
    apiCallLog.addFieldToRequest(ThirdPartyApiCallField.builder()
                                     .name("url")
                                     .type(ThirdPartyApiCallLog.FieldType.URL)
                                     .value(callDetails.getRequest().request().url().toString())
                                     .build());
    apiCallLog.addFieldToResponse(
        callDetails.getResponse().code(), callDetails.getResponse(), ThirdPartyApiCallLog.FieldType.JSON);
    delegateLogService.save(accountId, apiCallLog);
  }
}
