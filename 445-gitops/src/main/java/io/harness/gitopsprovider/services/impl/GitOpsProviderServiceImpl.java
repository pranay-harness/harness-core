package io.harness.gitopsprovider.services.impl;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.connector.GitOpsProviderType;
import io.harness.delegate.beans.connector.gitops.GitOpsProviderDTO;
import io.harness.delegate.beans.connector.gitops.GitOpsProviderResponseDTO;
import io.harness.gitopsprovider.entity.GitOpsProvider;
import io.harness.gitopsprovider.mappers.GitOpsProviderEntityMapper;
import io.harness.gitopsprovider.services.GitopsProviderService;
import io.harness.repositories.GitOpsProviderRepository;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Map;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Singleton
@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject }))
@Slf4j
@OwnedBy(HarnessTeam.GITOPS)
public class GitOpsProviderServiceImpl implements GitopsProviderService {
  private final GitOpsProviderRepository gitopsProviderRepository;
  @Inject private final Map<GitOpsProviderType, GitOpsProviderEntityMapper> gitopsProviderEntityMapperBinding;

  @Override
  public Optional<GitOpsProviderResponseDTO> get(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String connectorIdentifier) {
    GitOpsProvider gitopsProvider =
        gitopsProviderRepository.get(connectorIdentifier, projectIdentifier, orgIdentifier, accountIdentifier);
    return gitopsProvider != null ? Optional.of(toResponseDTO(gitopsProvider)) : Optional.empty();
  }

  @Override
  public Page<GitOpsProviderResponseDTO> list(
      Pageable pageable, String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    Page<GitOpsProvider> gitOpsProviders =
        gitopsProviderRepository.findAll(pageable, projectIdentifier, orgIdentifier, accountIdentifier);

    return gitOpsProviders.map(gitOpsProvider -> toResponseDTO(gitOpsProvider));
  }

  @Override
  public GitOpsProviderResponseDTO create(GitOpsProviderDTO gitopsProviderDTO, String accountIdentifier) {
    final GitOpsProviderEntityMapper entityMapper =
        gitopsProviderEntityMapperBinding.get(gitopsProviderDTO.getInfoDTO().getGitProviderType());
    GitOpsProvider gitopsProvider = entityMapper.toGitOpsProviderEntity(gitopsProviderDTO, accountIdentifier);
    final GitOpsProvider savedEntity = gitopsProviderRepository.save(gitopsProvider);
    return entityMapper.toGitOpsProvider(savedEntity);
  }

  @Override
  public GitOpsProviderResponseDTO update(GitOpsProviderDTO gitopsProviderDTO, String accountIdentifier) {
    final GitOpsProviderEntityMapper entityMapper =
        gitopsProviderEntityMapperBinding.get(gitopsProviderDTO.getInfoDTO().getGitProviderType());
    GitOpsProvider gitopsProvider = entityMapper.toGitOpsProviderEntity(gitopsProviderDTO, accountIdentifier);
    final GitOpsProvider savedEntity = gitopsProviderRepository.update(gitopsProvider);
    return entityMapper.toGitOpsProvider(savedEntity);
  }

  @Override
  public boolean delete(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String connectorIdentifier) {
    gitopsProviderRepository.delete(connectorIdentifier, projectIdentifier, orgIdentifier, accountIdentifier);
    return false;
  }

  private GitOpsProviderResponseDTO toResponseDTO(GitOpsProvider gitopsProvider) {
    return gitopsProviderEntityMapperBinding.get(gitopsProvider.getGitOpsProviderType())
        .toGitOpsProvider(gitopsProvider);
  }
}
