package software.wings.service.impl;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.delegate.beans.DelegateMetaInfo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@TargetModule(HarnessModule._955_DELEGATE_BEANS)
public class PerpetualTaskCapabilityCheckResponse implements CapabilityCheckResponse {
  private DelegateMetaInfo delegateMetaInfo;
  private boolean ableToExecutePerpetualTask;
}
