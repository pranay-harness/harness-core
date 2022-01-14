package io.harness.delegate.task.stepstatus;

import io.harness.delegate.beans.DelegateMetaInfo;
import io.harness.delegate.beans.DelegateTaskNotifyResponseData;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StepStatusTaskResponseData implements DelegateTaskNotifyResponseData {
  private DelegateMetaInfo delegateMetaInfo;
  private StepStatus stepStatus;
}
