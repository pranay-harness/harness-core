package io.harness.gitopsprovider.services;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.connector.gitops.GitOpsProviderDTO;
import io.harness.delegate.beans.connector.gitops.GitOpsProviderResponseDTO;

import java.util.Optional;
import javax.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@OwnedBy(HarnessTeam.GITOPS)
public interface GitopsProviderCrudService {
  Optional<GitOpsProviderResponseDTO> get(@NotNull String accountIdentifier, String orgIdentifier,
      @NotNull String projectIdentifier, @NotNull String connectorIdentifier);

  Page<GitOpsProviderResponseDTO> list(
      Pageable pageable, @NotNull String accountIdentifier, String orgIdentifier, @NotNull String projectIdentifier);

  GitOpsProviderResponseDTO create(@NotNull GitOpsProviderDTO connector, @NotNull String accountIdentifier);

  GitOpsProviderResponseDTO update(@NotNull GitOpsProviderDTO connector, @NotNull String accountIdentifier);

  boolean delete(@NotNull String accountIdentifier, String orgIdentifier, String projectIdentifier,
      @NotNull String connectorIdentifier);
}