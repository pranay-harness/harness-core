package io.harness.delegate.task.executioncapability;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.delegate.beans.DelegateMetaInfo;
import io.harness.delegate.beans.DelegateTaskNotifyResponseData;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@TargetModule(HarnessModule._920_DELEGATE_SERVICE_BEANS)
public class BatchCapabilityCheckTaskResponse implements DelegateTaskNotifyResponseData {
  // Not used in this case, but enforced by interface
  private DelegateMetaInfo delegateMetaInfo;

  List<CapabilityCheckDetails> capabilityCheckDetailsList;
}
