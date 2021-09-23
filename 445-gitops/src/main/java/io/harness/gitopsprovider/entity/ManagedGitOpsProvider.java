package io.harness.gitopsprovider.entity;

import static io.harness.annotations.dev.HarnessTeam.GITOPS;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.connector.GitOpsProviderType;

import lombok.Builder;

@Builder
@OwnedBy(GITOPS)
public class ManagedGitOpsProvider extends GitOpsProvider {
  @Override
  public GitOpsProviderType getGitOpsProviderType() {
    return GitOpsProviderType.MANAGED_ARGO_PROVIDER;
  }
}
