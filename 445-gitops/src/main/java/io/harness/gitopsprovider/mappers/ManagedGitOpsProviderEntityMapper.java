package io.harness.gitopsprovider.mappers;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.connector.gitops.GitOpsProviderDTO;
import io.harness.delegate.beans.connector.gitops.GitOpsProviderResponseDTO;
import io.harness.gitopsprovider.entity.GitOpsProvider;

@OwnedBy(HarnessTeam.GITOPS)
public class ManagedGitOpsProviderEntityMapper implements GitOpsProviderEntityMapper {
  @Override
  public GitOpsProviderResponseDTO toGitOpsProvider(GitOpsProvider gitopsProvider) {
    return null;
  }

  @Override
  public GitOpsProvider toGitOpsProviderEntity(GitOpsProviderDTO gitopsProviderDTO, String accountIdentifier) {
    return null;
  }
}
