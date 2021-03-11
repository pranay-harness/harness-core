package software.wings.service.impl;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
import io.harness.delegate.beans.DelegateMetaInfo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@TargetModule(Module._420_DELEGATE_SERVICE)
public class PerpetualTaskCapabilityCheckResponse implements CapabilityCheckResponse {
  private DelegateMetaInfo delegateMetaInfo;
  private boolean ableToExecutePerpetualTask;
}
