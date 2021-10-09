package io.harness.delegate.task.scm;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.DelegateResponseData;

import lombok.Builder;
import lombok.Data;

@OwnedBy(HarnessTeam.PIPELINE)
@Data
@Builder
public class ScmPathFilterEvaluationTaskResponse implements DelegateResponseData {
  // TRUE if for each condition there exists a file that matches it, FALSE if there exists a
  // condition where there does not exist a file that matches it
  boolean matched;
  String errorMessage;
}
