package io.harness.gitsync.gitsyncerror.impl;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.CollectionUtils.emptyIfNull;
import static io.harness.ng.core.utils.NGUtils.validate;
import static io.harness.utils.PageUtils.getNGPageResponse;
import static io.harness.utils.PageUtils.getPageRequest;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.ROOT;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.group;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.limit;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.match;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.newAggregation;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.project;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.skip;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.sort;
import static org.springframework.data.mongodb.core.query.Update.update;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.git.YamlGitConfigDTO;
import io.harness.exception.InvalidRequestException;
import io.harness.git.model.GitFileChange;
import io.harness.gitsync.common.service.YamlGitConfigService;
import io.harness.gitsync.gitsyncerror.GitSyncErrorStatus;
import io.harness.gitsync.gitsyncerror.beans.GitSyncError;
import io.harness.gitsync.gitsyncerror.beans.GitSyncError.GitSyncErrorKeys;
import io.harness.gitsync.gitsyncerror.beans.GitSyncErrorAggregateByCommit;
import io.harness.gitsync.gitsyncerror.beans.GitSyncErrorAggregateByCommit.GitSyncErrorAggregateByCommitKeys;
import io.harness.gitsync.gitsyncerror.beans.GitSyncErrorType;
import io.harness.gitsync.gitsyncerror.beans.GitToHarnessErrorDetails;
import io.harness.gitsync.gitsyncerror.beans.HarnessToGitErrorDetails;
import io.harness.gitsync.gitsyncerror.dtos.GitSyncErrorAggregateByCommitDTO;
import io.harness.gitsync.gitsyncerror.dtos.GitSyncErrorDTO;
import io.harness.gitsync.gitsyncerror.remote.GitSyncErrorMapper;
import io.harness.gitsync.gitsyncerror.service.GitSyncErrorService;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;
import io.harness.repositories.gitSyncError.GitSyncErrorRepository;
import io.harness.utils.PageUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.jooq.tools.StringUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.ProjectionOperation;
import org.springframework.data.mongodb.core.aggregation.SortOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Update;

@Singleton
@Slf4j
@OwnedBy(PL)
public class GitSyncErrorServiceImpl implements GitSyncErrorService {
  private final YamlGitConfigService yamlGitConfigService;
  private final GitSyncErrorRepository gitSyncErrorRepository;
  private final ScheduledExecutorService executorService;
  public static final String EMPTY_STR = "";
  public static final Long DEFAULT_COMMIT_TIME = 0L;

  @Inject
  public GitSyncErrorServiceImpl(
      YamlGitConfigService yamlGitConfigService, GitSyncErrorRepository gitSyncErrorRepository) {
    this.yamlGitConfigService = yamlGitConfigService;
    this.gitSyncErrorRepository = gitSyncErrorRepository;
    this.executorService = Executors.newScheduledThreadPool(1);
    executorService.scheduleWithFixedDelay(this::markExpiredErrors, 1, 24, TimeUnit.HOURS);
  }

  @Override
  public PageResponse<GitSyncErrorAggregateByCommitDTO> listGitToHarnessErrorsGroupedByCommits(PageRequest pageRequest,
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String searchTerm, String repoId,
      String branch, Integer numberOfErrorsInSummary) {
    Pageable pageable = getPageRequest(pageRequest);
    Criteria criteria = createGitSyncErrorFilterCriteria(
        accountIdentifier, orgIdentifier, projectIdentifier, searchTerm, repoId, branch);
    criteria.and(GitSyncErrorKeys.status).in(GitSyncErrorStatus.ACTIVE, GitSyncErrorStatus.RESOLVED);
    GroupOperation groupOperation = group(GitSyncErrorKeys.gitCommitId)
                                        .count()
                                        .as(GitSyncErrorAggregateByCommitKeys.failedCount)
                                        .first(GitSyncErrorKeys.gitCommitId)
                                        .as(GitSyncErrorAggregateByCommitKeys.gitCommitId)
                                        .first(GitSyncErrorKeys.commitMessage)
                                        .as(GitSyncErrorAggregateByCommitKeys.commitMessage)
                                        .first(GitSyncErrorKeys.branchName)
                                        .as(GitSyncErrorAggregateByCommitKeys.branchName)
                                        .first(GitSyncErrorKeys.createdAt)
                                        .as(GitSyncErrorAggregateByCommitKeys.createdAt)
                                        .push(ROOT)
                                        .as(GitSyncErrorAggregateByCommitKeys.errorsForSummaryView);
    ProjectionOperation projectOperation = project()
                                               .andInclude(GitSyncErrorAggregateByCommitKeys.gitCommitId)
                                               .andInclude(GitSyncErrorAggregateByCommitKeys.createdAt)
                                               .andInclude(GitSyncErrorAggregateByCommitKeys.commitMessage)
                                               .andInclude(GitSyncErrorAggregateByCommitKeys.branchName)
                                               .andInclude(GitSyncErrorAggregateByCommitKeys.failedCount)
                                               .andExpression(GitSyncErrorAggregateByCommitKeys.errorsForSummaryView)
                                               .slice(numberOfErrorsInSummary)
                                               .as(GitSyncErrorAggregateByCommitKeys.errorsForSummaryView);
    SortOperation sortOperation = sort(Sort.Direction.DESC, GitSyncErrorAggregateByCommitKeys.createdAt);

    Aggregation aggregation = newAggregation(match(criteria), groupOperation, projectOperation, sortOperation,
        skip(pageable.getOffset()), limit(pageable.getPageSize()));
    List<GitSyncErrorAggregateByCommit> gitSyncErrorAggregateByCommitList =
        gitSyncErrorRepository.aggregate(aggregation, GitSyncErrorAggregateByCommit.class).getMappedResults();
    List<GitSyncErrorAggregateByCommitDTO> gitSyncErrorAggregateByCommitDTOList =
        emptyIfNull(gitSyncErrorAggregateByCommitList)
            .stream()
            .map(GitSyncErrorMapper::toGitSyncErrorAggregateByCommitDTO)
            .collect(Collectors.toList());
    Set<String> repoUrls = emptyIfNull(gitSyncErrorAggregateByCommitDTOList)
                               .stream()
                               .map(gitSyncErrorAggregateByCommitDTO
                                   -> gitSyncErrorAggregateByCommitDTO.getErrorsForSummaryView().get(0).getRepoUrl())
                               .collect(Collectors.toSet());
    Map<String, String> repoIds = getRepoId(repoUrls, accountIdentifier, orgIdentifier, projectIdentifier);
    gitSyncErrorAggregateByCommitDTOList.stream()
        .map(gitSyncErrorAggregateByCommitDTO -> {
          String repoUrl = gitSyncErrorAggregateByCommitDTO.getErrorsForSummaryView().get(0).getRepoUrl();
          gitSyncErrorAggregateByCommitDTO.setRepoId(repoIds.get(repoUrl));
          return gitSyncErrorAggregateByCommitDTO;
        })
        .collect(Collectors.toList());
    Page<GitSyncErrorAggregateByCommitDTO> page =
        new PageImpl<>(gitSyncErrorAggregateByCommitDTOList, pageable, gitSyncErrorAggregateByCommitDTOList.size());
    return getNGPageResponse(page);
  }

  private Map<String, String> getRepoId(Set<String> repoUrls, String accountId, String orgId, String projectId) {
    return repoUrls.stream().collect(Collectors.toMap(repoUrl
        -> repoUrl,
        repoUrl -> yamlGitConfigService.getByProjectIdAndRepo(accountId, orgId, projectId, repoUrl).getIdentifier()));
  }

  @Override
  public PageResponse<GitSyncErrorDTO> listAllGitToHarnessErrors(PageRequest pageRequest, String accountId,
      String orgIdentifier, String projectIdentifier, String searchTerm, String repoId, String branch) {
    Criteria criteria =
        createGitSyncErrorFilterCriteria(accountId, orgIdentifier, projectIdentifier, searchTerm, repoId, branch);
    criteria.and(GitSyncErrorKeys.status).is(GitSyncErrorStatus.ACTIVE);
    Page<GitSyncErrorDTO> gitSyncErrorPage =
        gitSyncErrorRepository.findAll(criteria, PageUtils.getPageRequest(pageRequest))
            .map(GitSyncErrorMapper::toGitSyncErrorDTO);

    Set<String> repoUrls = new HashSet<>();
    gitSyncErrorPage.forEach(gitSyncErrorDTO -> { repoUrls.add(gitSyncErrorDTO.getRepoUrl()); });
    Map<String, String> repoIds = getRepoId(repoUrls, accountId, orgIdentifier, projectIdentifier);
    gitSyncErrorPage.forEach(
        gitSyncErrorDTO -> { gitSyncErrorDTO.setRepoId(repoIds.get(gitSyncErrorDTO.getRepoUrl())); });
    return getNGPageResponse(gitSyncErrorPage);
  }

  private Criteria createGitSyncErrorFilterCriteria(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String searchTerm, String repoId, String branch) {
    // when no filter is chosen - take all repos and their default branches
    Criteria criteria = Criteria.where(GitSyncErrorKeys.accountIdentifier)
                            .is(accountIdentifier)
                            .and(GitSyncErrorKeys.errorType)
                            .is(GitSyncErrorType.GIT_TO_HARNESS);
    Criteria repoBranchCriteria =
        getRepoBranchCriteria(accountIdentifier, orgIdentifier, projectIdentifier, repoId, branch);
    criteria.andOperator(repoBranchCriteria)
        .and(GitSyncErrorKeys.createdAt)
        .gt(OffsetDateTime.now().minusDays(30).toInstant().toEpochMilli());
    if (isNotBlank(searchTerm)) {
      criteria.orOperator(Criteria.where(GitSyncErrorKeys.gitCommitId).regex(searchTerm, "i"),
          Criteria.where(GitSyncErrorKeys.completeFilePath).regex(searchTerm, "i"),
          Criteria.where(GitSyncErrorKeys.entityType).regex(searchTerm, "i"));
    }
    return criteria;
  }

  @Override
  public PageResponse<GitSyncErrorDTO> listGitToHarnessErrorsForCommit(PageRequest pageRequest, String commitId,
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String repoId, String branch) {
    String repoUrl = yamlGitConfigService.get(projectIdentifier, orgIdentifier, accountIdentifier, repoId).getRepo();
    Criteria criteria = Criteria.where(GitSyncErrorKeys.accountIdentifier)
                            .is(accountIdentifier)
                            .and(GitSyncErrorKeys.errorType)
                            .is(GitSyncErrorType.GIT_TO_HARNESS)
                            .and(GitSyncErrorKeys.repoUrl)
                            .is(repoUrl)
                            .and(GitSyncErrorKeys.branchName)
                            .is(branch)
                            .and(GitSyncErrorKeys.gitCommitId)
                            .is(commitId)
                            .and(GitSyncErrorKeys.status)
                            .in(GitSyncErrorStatus.ACTIVE, GitSyncErrorStatus.RESOLVED);
    Page<GitSyncError> gitSyncErrorPage =
        gitSyncErrorRepository.findAll(criteria, PageUtils.getPageRequest(pageRequest));
    return getNGPageResponse(gitSyncErrorPage.map(GitSyncErrorMapper::toGitSyncErrorDTO));
  }

  private GitSyncErrorDTO save(GitSyncError gitSyncError) {
    try {
      validate(gitSyncError);
      GitSyncError savedError = gitSyncErrorRepository.save(gitSyncError);
      return GitSyncErrorMapper.toGitSyncErrorDTO(savedError);
    } catch (DuplicateKeyException ex) {
      throw new InvalidRequestException("A git sync for this commitId and File already exists.");
    }
  }

  @Override
  public GitSyncErrorDTO save(GitSyncErrorDTO gitSyncErrorDTO) {
    return save(GitSyncErrorMapper.toGitSyncError(gitSyncErrorDTO, gitSyncErrorDTO.getAccountIdentifier()));
  }

  private void markExpiredErrors() {
    Criteria criteria =
        Criteria.where(GitSyncErrorKeys.createdAt).lte(OffsetDateTime.now().minusDays(30).toInstant().toEpochMilli());
    Update update = update(GitSyncErrorKeys.status, GitSyncErrorStatus.EXPIRED);
    gitSyncErrorRepository.upsertGitError(criteria, update);
  }

  @Override
  public void upsertGitSyncErrors(
      GitFileChange failedChange, String errorMessage, boolean fullSyncPath, YamlGitConfigDTO yamlGitConfig) {
    upsertGitToHarnessError(failedChange, errorMessage, yamlGitConfig);
  }

  private void upsertGitToHarnessError(
      GitFileChange failedGitFileChange, String errorMessage, YamlGitConfigDTO yamlGitConfig) {
    log.info("Upsert git to harness sync issue for file: [{}]", failedGitFileChange.getFilePath());

    /*GitToHarnessErrorDetails gitToHarnessErrorDetails = getGitToHarnessErrorDetails(failedGitFileChange);
    final GitSyncError previousGitSyncError =
        gitSyncErrorRepository.findByAccountIdentifierAndCompleteFilePathAndErrorType(
            failedGitFileChange.getAccountId(), failedGitFileChange.getFilePath(), GitSyncErrorType.GIT_TO_HARNESS);
    addPreviousCommitDetailsToErrorDetails(failedGitFileChange, gitToHarnessErrorDetails, previousGitSyncError);
    gitSyncErrorRepository.upsertGitError(failedGitFileChange.getAccountId(), failedGitFileChange.getFilePath(),
        GIT_TO_HARNESS, errorMessage != null ? errorMessage : "Reason could not be captured. Logs might have some info",
        false, failedGitFileChange.getChangeType(), gitToHarnessErrorDetails, yamlGitConfig.getGitConnectorRef(),
        yamlGitConfig.getRepo(), yamlGitConfig.getBranch(),
        GitFileLocationHelper.getRootPathSafely(failedGitFileChange.getFilePath()), yamlGitConfig.getIdentifier(),
        yamlGitConfig.getProjectIdentifier(), yamlGitConfig.getOrganizationIdentifier());*/
  }

  private GitToHarnessErrorDetails getGitToHarnessErrorDetails(GitFileChange failedGitFileChange) {
    String failedCommitId = failedGitFileChange.getCommitId() != null ? failedGitFileChange.getCommitId() : "";
    if (failedCommitId.equals("")) {
      log.info("Unexpected behaviour: The git commitId is null for the git to harness error");
    }
    return GitToHarnessErrorDetails.builder()
        .gitCommitId(failedCommitId)
        .yamlContent(failedGitFileChange.getFileContent())
        .commitMessage(failedGitFileChange.getCommitMessage())
        .build();
  }

  @Override
  public List<GitSyncError> getActiveGitToHarnessSyncErrors(String accountId, String gitConnectorId, String repoName,
      String branchName, String rootFolder, long fromTimestamp) {
    return null; // gitSyncErrorRepository.getActiveGitSyncError(
                 // accountId, fromTimestamp, GIT_TO_HARNESS, gitConnectorId, repoName, branchName, rootFolder);
  }

  @Override
  public boolean deleteGitSyncErrors(List<String> errorIds, String accountId) {
    return gitSyncErrorRepository.deleteByIds(errorIds).wasAcknowledged();
  }

  @Override
  public void recordConnectivityError(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      GitSyncErrorType gitSyncErrorType, String repoUrl, String branch, String errorMessage) {
    GitSyncError gitSyncError = GitSyncError.builder()
                                    .accountIdentifier(accountIdentifier)
                                    .errorType(gitSyncErrorType)
                                    .repoUrl(repoUrl)
                                    .branchName(branch)
                                    .failureReason(errorMessage)
                                    .status(GitSyncErrorStatus.ACTIVE)
                                    .additionalErrorDetails(HarnessToGitErrorDetails.builder()
                                                                .orgIdentifier(orgIdentifier)
                                                                .projectIdentifier(projectIdentifier)
                                                                .build())
                                    .build();
    save(gitSyncError);
  }

  Criteria getRepoBranchCriteria(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String repoIdentifier, String branch) {
    List<Pair<String, String>> repoBranchList = new ArrayList<>();
    if (StringUtils.isEmpty(repoIdentifier)) {
      List<YamlGitConfigDTO> yamlGitConfigDTOS =
          yamlGitConfigService.list(projectIdentifier, orgIdentifier, accountIdentifier);
      repoBranchList = emptyIfNull(yamlGitConfigDTOS)
                           .stream()
                           .map(yamlGitConfigDTO -> {
                             String repo = yamlGitConfigDTO.getRepo();
                             String defaultBranch = yamlGitConfigDTO.getBranch();
                             return Pair.of(repo, defaultBranch);
                           })
                           .collect(Collectors.toList());
    } else { // when repo filter is applied
      YamlGitConfigDTO yamlGitConfigDTO =
          yamlGitConfigService.get(projectIdentifier, orgIdentifier, accountIdentifier, repoIdentifier);
      branch = StringUtils.isEmpty(branch) ? yamlGitConfigDTO.getBranch() : branch;
      repoBranchList.add(Pair.of(yamlGitConfigDTO.getRepo(), branch));
    }
    Criteria repoBranchCriteria = new Criteria();
    for (Pair<String, String> repoBranch : repoBranchList) {
      repoBranchCriteria.orOperator(Criteria.where(GitSyncErrorKeys.repoUrl)
                                        .is(repoBranch.getLeft())
                                        .and(GitSyncErrorKeys.branchName)
                                        .is(repoBranch.getRight()));
    }
    return repoBranchCriteria;
  }

  @Override
  public PageResponse<GitSyncErrorDTO> listConnectivityErrors(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String repoIdentifier, String branch, PageRequest pageRequest) {
    Criteria criteria = Criteria.where(GitSyncErrorKeys.accountIdentifier)
                            .is(accountIdentifier)
                            .and(GitSyncErrorKeys.errorType)
                            .in(GitSyncErrorType.FULL_SYNC, GitSyncErrorType.CONNECTIVITY_ISSUE);

    Criteria repoBranchCriteria =
        getRepoBranchCriteria(accountIdentifier, orgIdentifier, projectIdentifier, repoIdentifier, branch);

    criteria.andOperator(repoBranchCriteria);
    criteria.and(GitSyncErrorKeys.status).is(GitSyncErrorStatus.ACTIVE);

    Page<GitSyncError> gitSyncErrors = gitSyncErrorRepository.findAll(criteria, PageUtils.getPageRequest(pageRequest));
    Page<GitSyncErrorDTO> dtos = gitSyncErrors.map(GitSyncErrorMapper::toGitSyncErrorDTO);
    return getNGPageResponse(dtos);
  }
}
