package io.harness.gitsync.gitsyncerror.impl;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.CollectionUtils.emptyIfNull;
import static io.harness.ng.core.utils.NGUtils.validate;
import static io.harness.utils.PageUtils.getNGPageResponse;
import static io.harness.utils.PageUtils.getPageRequest;

import static java.util.stream.Collectors.toList;
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
import io.harness.gitsync.common.service.YamlGitConfigService;
import io.harness.gitsync.gitsyncerror.GitSyncErrorStatus;
import io.harness.gitsync.gitsyncerror.beans.GitSyncError;
import io.harness.gitsync.gitsyncerror.beans.GitSyncError.GitSyncErrorKeys;
import io.harness.gitsync.gitsyncerror.beans.GitSyncErrorAggregateByCommit;
import io.harness.gitsync.gitsyncerror.beans.GitSyncErrorAggregateByCommit.GitSyncErrorAggregateByCommitKeys;
import io.harness.gitsync.gitsyncerror.beans.GitSyncErrorType;
import io.harness.gitsync.gitsyncerror.beans.HarnessToGitErrorDetails;
import io.harness.gitsync.gitsyncerror.dtos.GitSyncErrorAggregateByCommitDTO;
import io.harness.gitsync.gitsyncerror.dtos.GitSyncErrorCountDTO;
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
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
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
    Criteria criteria = createGitToHarnessErrorFilterCriteria(
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
            .collect(toList());
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
        .collect(toList());
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
        createGitToHarnessErrorFilterCriteria(accountId, orgIdentifier, projectIdentifier, searchTerm, repoId, branch);
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

  private Criteria createGitToHarnessErrorFilterCriteria(String accountIdentifier, String orgIdentifier,
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

  @Override
  public GitSyncErrorDTO save(GitSyncErrorDTO gitSyncErrorDTO) {
    return save(GitSyncErrorMapper.toGitSyncError(gitSyncErrorDTO, gitSyncErrorDTO.getAccountIdentifier()));
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
  public List<GitSyncErrorDTO> saveAll(List<GitSyncErrorDTO> gitSyncErrorDTOList) {
    List<GitSyncError> gitSyncErrors =
        gitSyncErrorDTOList.stream()
            .map(gitSyncErrorDTO
                -> GitSyncErrorMapper.toGitSyncError(gitSyncErrorDTO, gitSyncErrorDTO.getAccountIdentifier()))
            .collect(toList());
    List<GitSyncError> gitSyncErrorsSaved = new ArrayList<>();
    gitSyncErrorRepository.saveAll(gitSyncErrors).iterator().forEachRemaining(gitSyncErrorsSaved::add);
    return gitSyncErrorsSaved.stream().map(GitSyncErrorMapper::toGitSyncErrorDTO).collect(toList());
  }

  @Override
  public void markOverriddenErrors(String accountId, String repoUrl, String branchName, Set<String> filePaths) {
    Criteria criteria = createActiveErrorsFilterCriteria(accountId, repoUrl, branchName, new ArrayList<>(filePaths));
    Update update = update(GitSyncErrorKeys.status, GitSyncErrorStatus.OVERRIDDEN);
    gitSyncErrorRepository.upsertGitError(criteria, update);
  }

  @Override
  public void markResolvedErrors(
      String accountId, String repoUrl, String branchName, Set<String> filePaths, String commitId) {
    Criteria criteria = createActiveErrorsFilterCriteria(accountId, repoUrl, branchName, new ArrayList<>(filePaths));
    Update update =
        update(GitSyncErrorKeys.status, GitSyncErrorStatus.RESOLVED).set(GitSyncErrorKeys.resolvedByCommitId, commitId);
    gitSyncErrorRepository.upsertGitError(criteria, update);
  }

  private Criteria createActiveErrorsFilterCriteria(
      String accountId, String repoUrl, String branchName, List<String> filePaths) {
    return Criteria.where(GitSyncErrorKeys.accountIdentifier)
        .is(accountId)
        .and(GitSyncErrorKeys.repoUrl)
        .is(repoUrl)
        .and(GitSyncErrorKeys.branchName)
        .is(branchName)
        .and(GitSyncErrorKeys.completeFilePath)
        .in(filePaths)
        .and(GitSyncErrorKeys.status)
        .is(GitSyncErrorStatus.ACTIVE);
  }

  @Override
  public Optional<GitSyncErrorDTO> getGitToHarnessError(
      String accountId, String commitId, String repoUrl, String branchName, String filePath) {
    Criteria criteria = Criteria.where(GitSyncErrorKeys.accountIdentifier)
                            .is(accountId)
                            .and(GitSyncErrorKeys.gitCommitId)
                            .is(commitId)
                            .and(GitSyncErrorKeys.repoUrl)
                            .is(repoUrl)
                            .and(GitSyncErrorKeys.branchName)
                            .is(branchName)
                            .and(GitSyncErrorKeys.completeFilePath)
                            .is(filePath);
    GitSyncError error = gitSyncErrorRepository.find(criteria);
    return Optional.ofNullable(GitSyncErrorMapper.toGitSyncErrorDTO(error));
  }

  private void markExpiredErrors() {
    Criteria criteria =
        Criteria.where(GitSyncErrorKeys.createdAt).lte(OffsetDateTime.now().minusDays(30).toInstant().toEpochMilli());
    Update update = update(GitSyncErrorKeys.status, GitSyncErrorStatus.EXPIRED);
    gitSyncErrorRepository.upsertGitError(criteria, update);
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
                                    .createdAt(System.currentTimeMillis())
                                    .build();
    save(gitSyncError);
  }

  @Override
  public PageResponse<GitSyncErrorDTO> listConnectivityErrors(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String repoIdentifier, String branch, PageRequest pageRequest) {
    Criteria criteria = createConnectivityErrorFilterCriteria(
        accountIdentifier, orgIdentifier, projectIdentifier, repoIdentifier, branch);

    Page<GitSyncError> gitSyncErrors = gitSyncErrorRepository.findAll(criteria, PageUtils.getPageRequest(pageRequest));
    Page<GitSyncErrorDTO> dtos = gitSyncErrors.map(GitSyncErrorMapper::toGitSyncErrorDTO);
    return getNGPageResponse(dtos);
  }

  private Criteria createConnectivityErrorFilterCriteria(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String repoIdentifier, String branch) {
    Criteria criteria = Criteria.where(GitSyncErrorKeys.accountIdentifier)
                            .is(accountIdentifier)
                            .and(GitSyncErrorKeys.errorType)
                            .in(GitSyncErrorType.FULL_SYNC, GitSyncErrorType.CONNECTIVITY_ISSUE);
    Criteria repoBranchCriteria =
        getRepoBranchCriteria(accountIdentifier, orgIdentifier, projectIdentifier, repoIdentifier, branch);

    criteria.andOperator(repoBranchCriteria);
    criteria.and(GitSyncErrorKeys.status)
        .is(GitSyncErrorStatus.ACTIVE)
        .and(GitSyncErrorKeys.createdAt)
        .gt(OffsetDateTime.now().minusDays(30).toInstant().toEpochMilli());
    return criteria;
  }

  @Override
  public GitSyncErrorCountDTO getErrorCount(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      String searchTerm, String repoId, String branch) {
    return GitSyncErrorCountDTO.builder()
        .gitToHarnessErrorCount(
            getGitToHarnessErrorCount(accountIdentifier, orgIdentifier, projectIdentifier, searchTerm, repoId, branch))
        .connectivityErrorCount(
            getConnectivityErrorCount(accountIdentifier, orgIdentifier, projectIdentifier, repoId, branch))
        .build();
  }

  private long getGitToHarnessErrorCount(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      String searchTerm, String repoId, String branch) {
    Criteria criteria = createGitToHarnessErrorFilterCriteria(
        accountIdentifier, orgIdentifier, projectIdentifier, searchTerm, repoId, branch);
    criteria.and(GitSyncErrorKeys.status).is(GitSyncErrorStatus.ACTIVE);
    return gitSyncErrorRepository.count(criteria);
  }

  private long getConnectivityErrorCount(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String repoId, String branch) {
    Criteria criteria =
        createConnectivityErrorFilterCriteria(accountIdentifier, orgIdentifier, projectIdentifier, repoId, branch);
    return gitSyncErrorRepository.count(criteria);
  }
}
