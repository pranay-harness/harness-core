package io.harness.delegate.beans.scm;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.product.ci.scm.proto.CreateFileResponse;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(HarnessTeam.DX)
public class ScmOnDelegateResponse implements DelegateResponseData {
  private CreateFileResponse createFileResponse;
}
