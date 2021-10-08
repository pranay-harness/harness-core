package io.harness.gitopsprovider.mappers;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.connector.gitops.ConnectedArgoGitOpsInfoDTO;
import io.harness.delegate.beans.connector.gitops.GitOpsProviderDTO;
import io.harness.delegate.beans.connector.gitops.GitOpsProviderResponseDTO;
import io.harness.gitopsprovider.entity.ConnectedArgoProvider;
import io.harness.gitopsprovider.entity.GitOpsProvider;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

@Singleton
@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject }))
@OwnedBy(HarnessTeam.GITOPS)
public class ConnectedGitOpsProviderEntityMapper extends AbstractGitOpsProviderEntityMapper {
  @Override
  public GitOpsProviderResponseDTO toGitOpsProviderDTO(@NotNull GitOpsProvider gitopsProvider) {
    ConnectedArgoProvider argoProvider = (ConnectedArgoProvider) gitopsProvider;
    GitOpsProviderResponseDTO responseDTO = new GitOpsProviderResponseDTO();
    responseDTO.setInfoDTO(ConnectedArgoGitOpsInfoDTO.builder().adapterUrl(argoProvider.getAdapterUrl()).build());
    setDtoFields(gitopsProvider, responseDTO);
    return responseDTO;
  }

  @Override
  public GitOpsProvider toGitOpsProviderEntity(
      @NotNull GitOpsProviderDTO gitopsProviderDTO, @NotNull String accountIdentifier) {
    final ConnectedArgoProvider entity =
        ConnectedArgoProvider.builder()
            .adapterUrl(((ConnectedArgoGitOpsInfoDTO) gitopsProviderDTO.getInfoDTO()).getAdapterUrl())
            .build();
    setEntityFields(gitopsProviderDTO, entity, accountIdentifier);
    return entity;
  }
}
