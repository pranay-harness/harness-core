package io.harness.delegate.beans.argo.response;

import io.harness.argo.beans.ManifestDiff;
import io.harness.delegate.beans.DelegateMetaInfo;
import io.harness.logging.CommandExecutionStatus;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ManifestDiffResponse implements ArgoTaskResponse{
  private DelegateMetaInfo delegateMetaInfo;
  private CommandExecutionStatus executionStatus;
  private String errorMessage;
  private List<ManifestDiff> manifestDiffList;
}
