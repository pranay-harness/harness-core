package io.harness.gitopsprovider.mappers;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.connector.gitops.GitOpsProviderDTO;
import io.harness.delegate.beans.connector.gitops.GitOpsProviderResponseDTO;
import io.harness.gitopsprovider.entity.GitOpsProvider;

import javax.validation.constraints.NotNull;

@OwnedBy(HarnessTeam.GITOPS)
public interface GitOpsProviderEntityMapper {
  GitOpsProviderResponseDTO toGitOpsProviderDTO(@NotNull GitOpsProvider gitopsProvider);
  GitOpsProvider toGitOpsProviderEntity(
      @NotNull GitOpsProviderDTO gitopsProviderDTO, @NotNull String accountIdentifier);
}
