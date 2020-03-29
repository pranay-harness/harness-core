package software.wings.service.impl.yaml;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.beans.PageResponse.PageResponseBuilder.aPageResponse;
import static io.harness.beans.SearchFilter.Operator.EQ;
import static io.harness.beans.SortOrder.OrderType.DESC;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.util.stream.Collectors.toList;
import static org.mongodb.morphia.aggregation.Accumulator.accumulator;
import static org.mongodb.morphia.aggregation.Group.first;
import static org.mongodb.morphia.aggregation.Group.grouping;
import static software.wings.alerts.AlertStatus.Open;
import static software.wings.beans.Base.ACCOUNT_ID_KEY;
import static software.wings.beans.Base.APP_ID_KEY;
import static software.wings.beans.Base.ID_KEY;

import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.beans.SearchFilter;
import io.harness.data.structure.EmptyPredicate;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.Sort;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.beans.Account;
import software.wings.beans.Account.AccountKeys;
import software.wings.beans.Application;
import software.wings.beans.Application.ApplicationKeys;
import software.wings.beans.EntityType;
import software.wings.beans.GitCommit;
import software.wings.beans.GitCommit.GitCommitKeys;
import software.wings.beans.GitConfig;
import software.wings.beans.GitDetail;
import software.wings.beans.GitDetail.GitDetailBuilder;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.SettingAttributeKeys;
import software.wings.beans.alert.Alert;
import software.wings.beans.alert.Alert.AlertKeys;
import software.wings.beans.alert.AlertType;
import software.wings.beans.yaml.GitDiffResult;
import software.wings.beans.yaml.GitFileChange;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.yaml.sync.GitSyncService;
import software.wings.settings.SettingValue;
import software.wings.utils.AlertsUtils;
import software.wings.yaml.errorhandling.GitProcessingError;
import software.wings.yaml.errorhandling.GitSyncError;
import software.wings.yaml.errorhandling.GitSyncError.GitSyncErrorKeys;
import software.wings.yaml.gitSync.GitFileActivity;
import software.wings.yaml.gitSync.GitFileActivity.GitFileActivityBuilder;
import software.wings.yaml.gitSync.GitFileActivity.GitFileActivityKeys;
import software.wings.yaml.gitSync.GitFileActivity.Status;
import software.wings.yaml.gitSync.GitFileActivity.TriggeredBy;
import software.wings.yaml.gitSync.GitFileProcessingSummary;
import software.wings.yaml.gitSync.YamlGitConfig;
import software.wings.yaml.gitSync.YamlGitConfig.YamlGitConfigKeys;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.validation.executable.ValidateOnExecution;

/**
 * git sync service.
 */
@ValidateOnExecution
@Singleton
@Slf4j
public class GitSyncServiceImpl implements GitSyncService {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private AlertsUtils alertsUtils;
  private static final EnumSet<Status> TERMINATING_STATUSES = EnumSet.of(Status.EXPIRED, Status.DISCARDED);

  @Override
  public PageResponse<GitSyncError> fetchErrors(PageRequest<GitSyncError> req) {
    return wingsPersistence.query(GitSyncError.class, req);
  }

  @Override
  public PageResponse<GitFileActivity> fetchGitSyncActivity(PageRequest<GitFileActivity> req) {
    return wingsPersistence.query(GitFileActivity.class, req);
  }

  @Override
  public void deleteGitSyncErrorAndLogFileActivity(List<GitSyncError> gitSyncErrors, Status status, String accountId) {
    wingsPersistence.save(getActivitiesForGitSyncErrors(gitSyncErrors, status));
    if (TERMINATING_STATUSES.contains(status)) {
      final List<String> discardErrorIds =
          gitSyncErrors.stream().map(error -> error.getUuid()).collect(Collectors.toList());
      logger.info(String.format("Marking errors with ids: %s as %s", discardErrorIds, status.name()));
      deleteGitSyncErrors(discardErrorIds, accountId);
    }
  }

  private List<GitFileActivity> getActivitiesForGitSyncErrors(final List<GitSyncError> errors, Status status) {
    if (EmptyPredicate.isEmpty(errors)) {
      return Collections.emptyList();
    }
    return errors.stream()
        .map(error
            -> GitFileActivity.builder()
                   .accountId(error.getAccountId())
                   .commitId(error.getGitCommitId())
                   .filePath(error.getYamlFilePath())
                   .fileContent(error.getYamlContent())
                   .status(status)
                   .triggeredBy(GitFileActivity.TriggeredBy.USER)
                   .build())
        .collect(Collectors.toList());
  }

  private void deleteGitSyncErrors(List<String> errorIds, String accountId) {
    Query query = wingsPersistence.createAuthorizedQuery(GitSyncError.class);
    query.filter(ACCOUNT_ID_KEY, accountId);
    query.field(ID_KEY).in(errorIds);
    wingsPersistence.delete(query);
    alertsUtils.closeAlertIfApplicable(accountId, false);
  }

  @Override
  public List<GitDetail> fetchRepositories(String accountId) {
    final List<EntityType> supportedEntityTypes = Arrays.asList(EntityType.APPLICATION, EntityType.ACCOUNT);
    final Set<YamlGitConfig> yamlGitConfigIds = wingsPersistence.createQuery(YamlGitConfig.class)
                                                    .filter(ACCOUNT_ID_KEY, accountId)
                                                    .filter(YamlGitConfigKeys.enabled, Boolean.TRUE)
                                                    .field(YamlGitConfig.ENTITY_TYPE_KEY)
                                                    .in(supportedEntityTypes)
                                                    .asList()
                                                    .stream()
                                                    .collect(Collectors.toSet());

    if (EmptyPredicate.isEmpty(yamlGitConfigIds)) {
      return Collections.emptyList();
    }

    final Set<String> entityIds =
        yamlGitConfigIds.stream().map(yamlGitConfig -> yamlGitConfig.getEntityId()).collect(Collectors.toSet());
    final Set<String> gitConnectorIds =
        yamlGitConfigIds.stream().map(yamlGitConfig -> yamlGitConfig.getGitConnectorId()).collect(Collectors.toSet());

    // app id to app name mapping for entity type APPLICATION
    final Map<String, String> entityIdToEntityNameMapping =
        wingsPersistence.createQuery(Application.class)
            .filter(ACCOUNT_ID_KEY, accountId)
            .field(APP_ID_KEY)
            .in(entityIds)
            .project(ApplicationKeys.appId, true)
            .project(ApplicationKeys.name, true)
            .asList()
            .stream()
            .collect(Collectors.toMap(Application::getUuid, Application::getName));

    // account id to account name mapping for entity type ACCOUNT
    final Account account = wingsPersistence.createQuery(Account.class)
                                .filter(AccountKeys.uuid, accountId)
                                .project(AccountKeys.uuid, true)
                                .project(AccountKeys.accountName, true)
                                .get();
    if (account != null) {
      entityIdToEntityNameMapping.put(account.getUuid(), account.getAccountName());
    }

    // git connector id to git config mapping
    final Map<String, SettingValue> settingIdToValueMapping =
        wingsPersistence.createQuery(SettingAttribute.class)
            .filter(ACCOUNT_ID_KEY, accountId)
            .field(ID_KEY)
            .in(gitConnectorIds)
            .project(SettingAttributeKeys.uuid, true)
            .project(SettingAttributeKeys.value, true)
            .asList()
            .stream()
            .collect(Collectors.toMap(SettingAttribute::getUuid, SettingAttribute::getValue));

    if (EmptyPredicate.isEmpty(entityIdToEntityNameMapping) || EmptyPredicate.isEmpty(settingIdToValueMapping)) {
      return Collections.emptyList();
    }

    final List<GitDetail> gitDetails =
        yamlGitConfigIds.stream()
            .map(yamlGitConfig -> {
              final SettingValue settingValue = settingIdToValueMapping.get(yamlGitConfig.getGitConnectorId());
              if (settingValue instanceof GitConfig) {
                final GitConfig gitConfig = (GitConfig) settingValue;
                final GitDetailBuilder builder = GitDetail.builder()
                                                     .entityType(yamlGitConfig.getEntityType())
                                                     .branchName(yamlGitConfig.getBranchName())
                                                     .repositoryUrl(gitConfig.getRepoUrl())
                                                     .yamlGitConfigId(yamlGitConfig.getUuid());
                final String entityName = entityIdToEntityNameMapping.get(yamlGitConfig.getEntityId());
                if (entityName != null) {
                  builder.entityName(entityName);
                } else {
                  return null;
                }
                return builder.build();
              }
              return null;
            })
            .collect(Collectors.toList());
    Iterables.removeIf(gitDetails, Predicates.isNull());
    return gitDetails;
  }

  @Override
  public PageResponse<GitCommit> fetchGitCommits(
      PageRequest<GitCommit> pageRequest, Boolean gitToHarness, String accountId) {
    pageRequest.addFilter(GitCommitKeys.accountId, SearchFilter.Operator.HAS, accountId);
    if (gitToHarness != null) {
      pageRequest.addFilter("yamlChangeSet.gitToHarness", SearchFilter.Operator.HAS, gitToHarness);
    }
    return wingsPersistence.query(GitCommit.class, pageRequest);
  }

  private TriggeredBy getTriggeredBy(boolean isGitToHarness, boolean isFullSync) {
    if (isGitToHarness) {
      return TriggeredBy.GIT;
    } else {
      if (isFullSync) {
        return TriggeredBy.FULL_SYNC;
      } else {
        return TriggeredBy.USER;
      }
    }
  }

  @Override
  public void logActivityForGitOperation(List<GitFileChange> changeList, Status status, boolean isGitToHarness,
      boolean isFullSync, String message, String commitId) {
    try {
      final List<GitFileActivity> activities = changeList.stream()
                                                   .map(change
                                                       -> buildBaseGitFileActivity(change, commitId)
                                                              .status(status)
                                                              .errorMessage(message)
                                                              .triggeredBy(getTriggeredBy(isGitToHarness, isFullSync))
                                                              .build())
                                                   .collect(Collectors.toList());

      wingsPersistence.save(activities);
    } catch (Exception ex) {
      logger.error(String.format("Error while saving activities: %s", ex));
    }
  }

  @Override
  public void logActivityForFiles(
      final String commitId, final List<String> fileNames, Status status, String message, String accountId) {
    if (EmptyPredicate.isEmpty(fileNames)) {
      return;
    }
    try {
      UpdateOperations<GitFileActivity> op = wingsPersistence.createUpdateOperations(GitFileActivity.class)
                                                 .set(GitFileActivityKeys.status, status)
                                                 .set(GitFileActivityKeys.errorMessage, message);
      wingsPersistence.update(wingsPersistence.createQuery(GitFileActivity.class)
                                  .filter(ACCOUNT_ID_KEY, accountId)
                                  .filter(GitFileActivityKeys.processingCommitId, commitId)
                                  .field(GitFileActivityKeys.filePath)
                                  .in(fileNames),
          op);
    } catch (Exception ex) {
      logger.error(String.format("Error while saving activities for commitId: %s", "commitId"));
    }
  }

  @Override
  public void logActivityForSkippedFiles(
      List<GitFileChange> changeList, GitDiffResult gitDiffResult, String message, String accountId) {
    List<GitFileChange> completeChangeList = new ArrayList<>(gitDiffResult.getGitFileChanges());
    completeChangeList = ListUtils.removeAll(completeChangeList, changeList);
    if (EmptyPredicate.isNotEmpty(completeChangeList)) {
      logActivityForFiles(gitDiffResult.getCommitId(),
          completeChangeList.stream().map(gitFileChange -> gitFileChange.getFilePath()).collect(Collectors.toList()),
          Status.SKIPPED, message, accountId);
    }
  }

  private GitFileActivityBuilder buildBaseGitFileActivity(GitFileChange change, String commitId) {
    String commitIdToPersist = StringUtils.isEmpty(commitId) ? change.getCommitId() : commitId;
    String processingCommitIdToPersist = StringUtils.isEmpty(commitId) ? change.getProcessingCommitId() : commitId;
    final Boolean changeFromAnotherCommit = change.getChangeFromAnotherCommit();
    return GitFileActivity.builder()
        .accountId(change.getAccountId())
        .commitId(commitIdToPersist)
        .processingCommitId(processingCommitIdToPersist)
        .filePath(change.getFilePath())
        .fileContent(change.getFileContent())
        .changeType(change.getChangeType())
        .changeFromAnotherCommit(changeFromAnotherCommit != null
                ? changeFromAnotherCommit
                : !processingCommitIdToPersist.equalsIgnoreCase(commitIdToPersist));
  }

  public void addFileProcessingSummaryToGitCommit(
      final String commitId, final String accountId, final List<GitFileChange> gitFileChanges) {
    try {
      final Map<Status, Long> statusToCountMap =
          wingsPersistence.createQuery(GitFileActivity.class)
              .filter(ACCOUNT_ID_KEY, accountId)
              .filter(GitFileActivityKeys.processingCommitId, commitId)
              .project(GitFileActivityKeys.status, true)
              .asList()
              .stream()
              .map(gitFileActivity -> gitFileActivity.getStatus())
              .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

      final Long totalCount = statusToCountMap.values().stream().reduce(0L, Long::sum);

      final Long otherCount =
          gitFileChanges.stream()
              .filter(gitFileChange -> Boolean.TRUE.equals(gitFileChange.getChangeFromAnotherCommit()))
              .count();

      final GitFileProcessingSummary gitFileProcessingSummary =
          GitFileProcessingSummary.builder()
              .failureCount(statusToCountMap.getOrDefault(Status.FAILED, 0L))
              .successCount(statusToCountMap.getOrDefault(Status.SUCCESS, 0L))
              .skippedCount(statusToCountMap.getOrDefault(Status.SKIPPED, 0L))
              .queuedCount(statusToCountMap.getOrDefault(Status.QUEUED, 0L))
              .totalCount(totalCount)
              .originalCount(totalCount - otherCount)
              .otherCount(otherCount)
              .build();

      wingsPersistence.update(wingsPersistence.createQuery(GitCommit.class)
                                  .filter(ACCOUNT_ID_KEY, accountId)
                                  .filter(GitCommitKeys.commitId, commitId),
          wingsPersistence.createUpdateOperations(GitCommit.class)
              .set(GitCommitKeys.fileProcessingSummary, gitFileProcessingSummary));
    } catch (Exception ex) {
      logger.error(String.format("Error while saving git file processing summary for commitId: %s", commitId), ex);
    }
  }

  @Override
  public PageResponse<GitToHarnessErrorCommitStats> fetchGitToHarnessErrors(
      PageRequest<GitToHarnessErrorCommitStats> req, String accountId, String yamlGitConfigId) {
    // Creating a page request to get the commits
    Integer limit = req.getLimit() == null ? Integer.MAX_VALUE : Integer.parseInt(req.getLimit());
    Integer offset = req.getOffset() == null ? 0 : Integer.parseInt(req.getOffset());

    Query<GitSyncError> query = wingsPersistence.createQuery(GitSyncError.class)
                                    .filter(ACCOUNT_ID_KEY, accountId)
                                    .filter("gitCommitId != ", "");

    if (isNotEmpty(yamlGitConfigId)) {
      query.filter(GitSyncErrorKeys.yamlGitConfigId, yamlGitConfigId);
    }

    List<GitToHarnessErrorCommitStats> commitWiseErrorMessages = new ArrayList<>();
    wingsPersistence.getDatastore(GitSyncError.class)
        .createAggregation(GitSyncError.class)
        .match(query)
        .group(GitSyncErrorKeys.gitCommitId, grouping("failedCount", accumulator("$sum", 1)),
            grouping(GitSyncError.CREATED_AT_KEY, first(GitSyncError.CREATED_AT_KEY)))
        .sort(Sort.descending(GitSyncError.CREATED_AT_KEY))
        .limit(limit)
        .skip(offset)
        .aggregate(GitToHarnessErrorCommitStats.class)
        .forEachRemaining(gitToHarnessErrors -> commitWiseErrorMessages.add(gitToHarnessErrors));
    return aPageResponse().withTotal(commitWiseErrorMessages.size()).withResponse(commitWiseErrorMessages).build();
  }

  @Override
  public PageResponse<GitSyncError> fetchErrorsInEachCommits(
      PageRequest<GitSyncError> req, String gitCommitId, String accountId) {
    // Creating a page request to get the commits
    req.addFilter("gitCommitId", EQ, gitCommitId);
    req.addFilter(ACCOUNT_ID_KEY, EQ, accountId);
    return wingsPersistence.query(GitSyncError.class, req);
  }

  private GitProcessingError getGitProcessingError(Alert alert) {
    return GitProcessingError.builder()
        .message(alert.getTitle())
        .accountId(alert.getAccountId())
        .createdAt(alert.getCreatedAt())
        .build();
  }

  @Override
  public PageResponse<GitProcessingError> fetchGitProcessingErrors(
      PageRequest<GitProcessingError> req, String accountId) {
    PageRequest<Alert> gitAlertRequest = aPageRequest()
                                             .withLimit(req.getLimit())
                                             .withOffset(req.getOffset())
                                             .addFilter(AlertKeys.accountId, EQ, accountId)
                                             .addFilter(AlertKeys.type, EQ, AlertType.GitConnectionError)
                                             .addFilter(AlertKeys.status, EQ, Open)
                                             .addOrder(GitCommitKeys.createdAt, DESC)
                                             .build();
    List<Alert> gitProcessingAlerts = wingsPersistence.query(Alert.class, gitAlertRequest).getResponse();
    if (isEmpty(gitProcessingAlerts)) {
      return aPageResponse().withTotal(0).withResponse(Collections.emptyList()).build();
    }
    List<GitProcessingError> gitProcessingErrors =
        gitProcessingAlerts.stream().map(this ::getGitProcessingError).collect(toList());
    return aPageResponse().withTotal(gitProcessingErrors.size()).withResponse(gitProcessingErrors).build();
  }

  public void markRemainingFilesAsSkipped(String commitId, String accountId) {
    try {
      logger.warn(String.format("Few files still in QUEUED status after git processing of commitId: %s", commitId));
      UpdateOperations<GitFileActivity> op = wingsPersistence.createUpdateOperations(GitFileActivity.class)
                                                 .set(GitFileActivityKeys.status, Status.SKIPPED);
      wingsPersistence.update(wingsPersistence.createQuery(GitFileActivity.class)
                                  .filter(ACCOUNT_ID_KEY, accountId)
                                  .filter(GitFileActivityKeys.processingCommitId, commitId)
                                  .filter(GitFileActivityKeys.status, Status.QUEUED),
          op);
    } catch (Exception ex) {
      logger.error(String.format("Error while saving activities for commitId: %s", "commitId"), ex);
    }
  }
}
