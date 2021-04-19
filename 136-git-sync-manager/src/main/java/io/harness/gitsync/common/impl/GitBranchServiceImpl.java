package io.harness.gitsync.common.impl;

import static io.harness.annotations.dev.HarnessTeam.DX;
import static io.harness.utils.PageUtils.getNGPageResponse;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.IdentifierRef;
import io.harness.connector.impl.ConnectorErrorMessagesHelper;
import io.harness.connector.services.ConnectorService;
import io.harness.delegate.beans.connector.scm.ScmConnector;
import io.harness.exception.InvalidRequestException;
import io.harness.gitsync.common.beans.GitBranch;
import io.harness.gitsync.common.beans.GitBranch.GitBranchKeys;
import io.harness.gitsync.common.beans.YamlGitConfig;
import io.harness.gitsync.common.dtos.GitBranchDTO;
import io.harness.gitsync.common.dtos.GitBranchDTO.SyncedBranchDTOKeys;
import io.harness.gitsync.common.service.GitBranchService;
import io.harness.ng.beans.PageResponse;
import io.harness.product.ci.scm.proto.ListBranchesResponse;
import io.harness.repositories.gitBranches.GitBranchesRepository;
import io.harness.repositories.repositories.yamlGitConfig.YamlGitConfigRepository;
import io.harness.service.ScmClient;
import io.harness.tasks.DecryptGitApiAccessHelper;
import io.harness.utils.IdentifierRefHelper;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.Criteria;

@Singleton
@Slf4j
@OwnedBy(DX)
public class GitBranchServiceImpl implements GitBranchService {
  private final DecryptGitApiAccessHelper decryptGitApiAccessHelper;
  private final ScmClient scmClient;
  private final ConnectorService connectorService;
  private final YamlGitConfigRepository yamlGitConfigRepository;
  private final ConnectorErrorMessagesHelper connectorErrorMessagesHelper;
  private final GitBranchesRepository gitBranchesRepository;

  @Inject
  public GitBranchServiceImpl(DecryptGitApiAccessHelper decryptGitApiAccessHelper, ScmClient scmClient,
      @Named("connectorDecoratorService") ConnectorService connectorService,
      YamlGitConfigRepository yamlGitConfigRepository, ConnectorErrorMessagesHelper connectorErrorMessagesHelper,
      GitBranchesRepository gitBranchesRepository) {
    this.decryptGitApiAccessHelper = decryptGitApiAccessHelper;
    this.scmClient = scmClient;
    this.connectorService = connectorService;
    this.yamlGitConfigRepository = yamlGitConfigRepository;
    this.connectorErrorMessagesHelper = connectorErrorMessagesHelper;
    this.gitBranchesRepository = gitBranchesRepository;
  }

  @Override
  public List<String> listBranchesForRepoByConnector(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String connectorIdentifier, String repoURL) {
    ScmConnector scmConnector =
        connectorService.get(accountIdentifier, orgIdentifier, projectIdentifier, connectorIdentifier)
            .map(connectorResponseDTO
                -> decryptGitApiAccessHelper.decryptScmApiAccess(
                    (ScmConnector) connectorResponseDTO.getConnector().getConnectorConfig(), accountIdentifier,
                    projectIdentifier, orgIdentifier))
            .orElseThrow(()
                             -> new InvalidRequestException(connectorErrorMessagesHelper.createConnectorNotFoundMessage(
                                 accountIdentifier, orgIdentifier, projectIdentifier, connectorIdentifier)));
    scmConnector.setUrl(repoURL);
    ListBranchesResponse listBranchesResponse = scmClient.listBranches(scmConnector);
    return listBranchesResponse.getBranchesList();
  }

  @Override
  public List<String> listBranchesForRepoByGitSyncConfig(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String yamlGitConfigIdentifier) {
    YamlGitConfig yamlGitConfig =
        yamlGitConfigRepository
            .findByAccountIdAndOrgIdentifierAndProjectIdentifierAndIdentifier(
                accountIdentifier, orgIdentifier, projectIdentifier, yamlGitConfigIdentifier)
            .orElseThrow(()
                             -> new InvalidRequestException(
                                 "No Git Config exists with the identifier" + yamlGitConfigIdentifier));
    IdentifierRef identifierRef = IdentifierRefHelper.getIdentifierRef(yamlGitConfig.getGitConnectorRef(),
        yamlGitConfig.getAccountId(), yamlGitConfig.getOrgIdentifier(), yamlGitConfig.getProjectIdentifier());
    return listBranchesForRepoByConnector(identifierRef.getAccountIdentifier(), identifierRef.getOrgIdentifier(),
        identifierRef.getProjectIdentifier(), identifierRef.getIdentifier(), yamlGitConfig.getRepo());
  }

  @Override
  public PageResponse<GitBranchDTO> listBranchesWithStatus(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String yamlGitConfigIdentifier, int page, int size, String searchTerm) {
    Page<GitBranch> syncedBranchPage = gitBranchesRepository.findAll(
        getCriteria(accountIdentifier, orgIdentifier, projectIdentifier, yamlGitConfigIdentifier, searchTerm),
        PageRequest.of(page, size,
            Sort.by(
                Sort.Order.asc(SyncedBranchDTOKeys.branchSyncStatus), Sort.Order.asc(SyncedBranchDTOKeys.branchName))));
    final List<GitBranchDTO> gitBranchDTOList = buildEntityDtoFromPage(syncedBranchPage);
    return getNGPageResponse(syncedBranchPage, gitBranchDTOList);
  }

  private List<GitBranchDTO> buildEntityDtoFromPage(Page<GitBranch> gitBranchPage) {
    return gitBranchPage.get().map(this::buildSyncedBranchDTO).collect(Collectors.toList());
  }

  private GitBranchDTO buildSyncedBranchDTO(GitBranch entity) {
    return GitBranchDTO.builder()
        .branchName(entity.getBranchName())
        .branchSyncStatus(entity.getBranchSyncStatus())
        .build();
  }

  private Criteria getCriteria(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      String yamlGitConfigIdentifier, String searchTerm) {
    return Criteria.where(GitBranchKeys.accountIdentifier)
        .is(accountIdentifier)
        .and(GitBranchKeys.projectIdentifier)
        .is(projectIdentifier)
        .and(GitBranchKeys.orgIdentifier)
        .is(orgIdentifier)
        .and(GitBranchKeys.yamlGitConfigIdentifier)
        .is(yamlGitConfigIdentifier)
        .and(GitBranchKeys.branchName)
        .regex(searchTerm, "i");
  }
}
