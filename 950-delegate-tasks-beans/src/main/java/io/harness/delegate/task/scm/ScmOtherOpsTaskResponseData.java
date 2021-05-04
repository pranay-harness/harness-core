package io.harness.delegate.task.scm;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.product.ci.scm.proto.FileContent;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@OwnedBy(HarnessTeam.DX)
public class ScmOtherOpsTaskResponseData implements DelegateResponseData {
  ScmOtherOpsType scmOtherOpsType;
  FileContent fileContent;
}
