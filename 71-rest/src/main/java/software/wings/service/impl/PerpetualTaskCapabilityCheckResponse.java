package software.wings.service.impl;

import io.harness.delegate.beans.DelegateMetaInfo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PerpetualTaskCapabilityCheckResponse implements CapabilityCheckResponse {
  private DelegateMetaInfo delegateMetaInfo;
  private boolean ableToExecutePerpetualTask;
}
