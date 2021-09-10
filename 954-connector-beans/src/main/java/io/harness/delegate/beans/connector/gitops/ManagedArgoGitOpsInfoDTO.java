package io.harness.delegate.beans.connector.gitops;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.connector.GitOpsProviderType;

@OwnedBy(HarnessTeam.GITOPS)
public class ManagedArgoGitOpsInfoDTO extends GitOpsInfoDTO {
  @Override
  public GitOpsProviderType getGitProviderType() {
    return GitOpsProviderType.MANAGED_ARGO_PROVIDER;
  }
}
