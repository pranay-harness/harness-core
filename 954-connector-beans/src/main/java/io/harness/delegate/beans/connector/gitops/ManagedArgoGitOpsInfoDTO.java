package io.harness.delegate.beans.connector.gitops;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.connector.GitOpsProviderType;

import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(HarnessTeam.GITOPS)
public class ManagedArgoGitOpsInfoDTO extends GitOpsInfoDTO {
  @NotNull private String namespace;

  @Override
  public GitOpsProviderType getGitProviderType() {
    return GitOpsProviderType.MANAGED_ARGO_PROVIDER;
  }
}
