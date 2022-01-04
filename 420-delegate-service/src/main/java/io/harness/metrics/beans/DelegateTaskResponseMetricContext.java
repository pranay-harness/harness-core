package io.harness.metrics.beans;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.delegate.beans.NgSetupFields.NG;

import io.harness.beans.DelegateTask;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.metrics.AutoMetricContext;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
public class DelegateTaskResponseMetricContext extends AutoMetricContext {
  public DelegateTaskResponseMetricContext(String accountId, DelegateTaskResponse.ResponseCode responseCode,
      Class<? extends DelegateResponseData> responseDataClass) {
    put("accountId", accountId);
    put("responseCode", responseCode.name());
    put("responseDataClass", responseDataClass.getName());
  }

  public DelegateTaskResponseMetricContext(DelegateTask delegateTask, DelegateTaskResponse response) {
    boolean ng = !isEmpty(delegateTask.getSetupAbstractions())
        && Boolean.parseBoolean(delegateTask.getSetupAbstractions().get(NG));
    put("accountId", response.getAccountId());
    put("responseCode", response.getResponseCode().name());
    put("responseDataClass", response.getResponse().getClass().getName());
    put("taskId", delegateTask.getUuid());
    put("delegateId", delegateTask.getDelegateId());
    put("status", delegateTask.getStatus().name());
    put("taskType", delegateTask.getData().getTaskType());
    put("async", String.valueOf(delegateTask.getData().isAsync()));
    put("ng", String.valueOf(ng));
  }
}
