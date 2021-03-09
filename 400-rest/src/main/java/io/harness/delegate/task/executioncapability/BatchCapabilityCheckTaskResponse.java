package io.harness.delegate.task.executioncapability;

import io.harness.delegate.beans.DelegateMetaInfo;
import io.harness.delegate.beans.DelegateTaskNotifyResponseData;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BatchCapabilityCheckTaskResponse implements DelegateTaskNotifyResponseData {
  // Not used in this case, but enforced by interface
  private DelegateMetaInfo delegateMetaInfo;

  List<CapabilityCheckDetails> capabilityCheckDetailsList;
}
