package io.harness.gitopsprovider.services.impl;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.connector.GitOpsProviderType;
import io.harness.delegate.beans.connector.gitops.GitOpsProviderDTO;
import io.harness.delegate.beans.connector.gitops.GitOpsProviderResponseDTO;
import io.harness.gitopsprovider.entity.GitOpsProvider;
import io.harness.gitopsprovider.entity.GitOpsProvider.GitOpsProviderKeys;
import io.harness.gitopsprovider.mappers.GitOpsProviderEntityMapper;
import io.harness.gitopsprovider.services.GitopsProviderService;
import io.harness.ng.core.common.beans.NGTag;
import io.harness.repositories.GitOpsProviderRepository;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Map;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

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
      Pageable pageable, String accountIdentifier, String orgIdentifier, String projectIdentifier, String searchTerm) {
    Criteria criteria = getCriteria(accountIdentifier, orgIdentifier, projectIdentifier);
    if (isNotEmpty(searchTerm)) {
      applySearchFilter(criteria, searchTerm);
    }
    Page<GitOpsProvider> gitOpsProviders =
        gitopsProviderRepository.findAll(criteria, pageable, projectIdentifier, orgIdentifier, accountIdentifier);

    return gitOpsProviders.map(gitOpsProvider -> toResponseDTO(gitOpsProvider));
  }

  private Criteria getCriteria(String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    Criteria criteria = new Criteria();
    criteria.and(GitOpsProviderKeys.accountIdentifier).is(accountIdentifier);
    criteria.and(GitOpsProviderKeys.orgIdentifier).is(orgIdentifier);
    criteria.and(GitOpsProviderKeys.projectIdentifier).is(projectIdentifier);
    return criteria;
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

  private void applySearchFilter(Criteria criteria, String searchTerm) {
    if (StringUtils.isNotBlank(searchTerm)) {
      Criteria criteriaWithSearchTerm = this.getSearchTermFilter(searchTerm);
      criteria.andOperator(new Criteria[] {criteriaWithSearchTerm});
    }
  }
  private Criteria getSearchTermFilter(String searchTerm) {
    if (StringUtils.isNotBlank(searchTerm)) {
      Criteria tagCriteria = this.createCriteriaForSearchingTag(searchTerm);
      return (new Criteria())
          .orOperator(new Criteria[] {Criteria.where(GitOpsProviderKeys.name).regex(searchTerm, "i"),
              Criteria.where(GitOpsProviderKeys.identifier).regex(searchTerm, "i"),
              Criteria.where(GitOpsProviderKeys.description).regex(searchTerm, "i"), tagCriteria});
    } else {
      return null;
    }
  }

  private Criteria createCriteriaForSearchingTag(String searchTerm) {
    String keyToBeSearched = searchTerm;
    String valueToBeSearched = "";
    if (searchTerm.contains(":")) {
      String[] split = searchTerm.split(":");
      keyToBeSearched = split[0];
      valueToBeSearched = split.length >= 2 ? split[1] : "";
    }

    return Criteria.where(GitOpsProviderKeys.tags)
        .is(NGTag.builder().key(keyToBeSearched).value(valueToBeSearched).build());
  }
}
