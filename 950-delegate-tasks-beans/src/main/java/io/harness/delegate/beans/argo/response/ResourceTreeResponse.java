package io.harness.delegate.beans.argo.response;

import io.harness.argo.beans.ClusterResourceTreeDTO;
import io.harness.delegate.beans.DelegateMetaInfo;
import io.harness.logging.CommandExecutionStatus;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ResourceTreeResponse implements ArgoTaskResponse {
  private DelegateMetaInfo delegateMetaInfo;
  private CommandExecutionStatus executionStatus;
  private String errorMessage;
  private ClusterResourceTreeDTO clusterResourceTree;
}
