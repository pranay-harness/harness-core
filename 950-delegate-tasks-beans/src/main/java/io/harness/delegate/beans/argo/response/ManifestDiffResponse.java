package io.harness.delegate.beans.argo.response;

import io.harness.delegate.beans.DelegateMetaInfo;
import io.harness.logging.CommandExecutionStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ManifestDiffResponse {
  private DelegateMetaInfo delegateMetaInfo;
  private CommandExecutionStatus executionStatus;
  private String errorMessage;
}
