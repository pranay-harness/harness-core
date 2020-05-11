package software.wings.service.impl.yaml;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.util.stream.Collectors.toList;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.security.UserGroup.ACCOUNT_ID_KEY;
import static software.wings.service.impl.yaml.sync.GitSyncErrorUtils.getCommitIdOfError;
import static software.wings.service.impl.yaml.sync.GitSyncErrorUtils.getCommitMessageOfError;
import static software.wings.service.impl.yaml.sync.GitSyncErrorUtils.getYamlContentOfError;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.beans.SearchFilter;
import io.harness.data.structure.EmptyPredicate;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.beans.GitCommit;
import software.wings.beans.GitCommit.GitCommitKeys;
import software.wings.beans.GitDetail;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.SettingAttributeKeys;
import software.wings.beans.yaml.GitDiffResult;
import software.wings.beans.yaml.GitFileChange;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.GitConfigHelperService;
import software.wings.service.impl.yaml.gitsync.ChangeSetDTO;
import software.wings.service.impl.yaml.gitsync.ChangesetInformation;
import software.wings.service.impl.yaml.gitsync.QueuedChangesetInformation;
import software.wings.service.impl.yaml.gitsync.RunningChangesetInformation;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.yaml.YamlChangeSetService;
import software.wings.service.intfc.yaml.YamlGitService;
import software.wings.service.intfc.yaml.sync.GitSyncErrorService;
import software.wings.service.intfc.yaml.sync.GitSyncService;
import software.wings.service.intfc.yaml.sync.YamlGitConfigService;
import software.wings.utils.AlertsUtils;
import software.wings.yaml.errorhandling.GitSyncError;
import software.wings.yaml.gitSync.GitFileActivity;
import software.wings.yaml.gitSync.GitFileActivity.GitFileActivityBuilder;
import software.wings.yaml.gitSync.GitFileActivity.GitFileActivityKeys;
import software.wings.yaml.gitSync.GitFileActivity.Status;
import software.wings.yaml.gitSync.GitFileActivity.TriggeredBy;
import software.wings.yaml.gitSync.GitFileProcessingSummary;
import software.wings.yaml.gitSync.YamlChangeSet;
import software.wings.yaml.gitSync.YamlChangeSet.YamlChangeSetKeys;
import software.wings.yaml.gitSync.YamlGitConfig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
  @Inject private GitSyncErrorService gitSyncErrorService;
  @Inject private SettingsService settingsService;
  @Inject private GitConfigHelperService gitConfigHelperService;
  @Inject private YamlGitConfigService yamlGitConfigService;
  @Inject private YamlGitService yamlGitService;
  @Inject private YamlChangeSetService yamlChangeSetService;

  private static final String UNKNOWN_GIT_CONNECTOR = "Unknown Git Connector";

  @Override
  public PageResponse<GitFileActivity> fetchGitSyncActivity(PageRequest<GitFileActivity> req, String accountId) {
    PageResponse<GitFileActivity> response = wingsPersistence.query(GitFileActivity.class, req);
    List<GitFileActivity> gitFileActivities = response.getResponse();
    List<GitFileActivity> fileHistoryWithValidConnectorName =
        populateConnectorNameInFileHistory(gitFileActivities, accountId);
    response.setResponse(fileHistoryWithValidConnectorName);
    return response;
  }

  private List<GitFileActivity> populateConnectorNameInFileHistory(
      List<GitFileActivity> gitFileActivities, String accountId) {
    if (isEmpty(gitFileActivities)) {
      return gitFileActivities;
    }
    List<String> connectorIdList =
        gitFileActivities.stream().map(commit -> commit.getGitConnectorId()).collect(toList());
    Map<String, String> connectorIdNameMap = gitConfigHelperService.getConnectorIdNameMap(connectorIdList, accountId);
    return gitFileActivities.stream()
        .map(fileActivity -> {
          String connectorName =
              connectorIdNameMap.getOrDefault(fileActivity.getGitConnectorId(), UNKNOWN_GIT_CONNECTOR);
          fileActivity.setConnectorName(connectorName);
          return fileActivity;
        })
        .collect(toList());
  }

  public List<GitFileActivity> getActivitiesForGitSyncErrors(final List<GitSyncError> errors, Status status) {
    if (EmptyPredicate.isEmpty(errors)) {
      return Collections.emptyList();
    }
    return errors.stream()
        .map(error
            -> GitFileActivity.builder()
                   .accountId(error.getAccountId())
                   .commitId(getCommitIdOfError(error))
                   .filePath(error.getYamlFilePath())
                   .fileContent(getYamlContentOfError(error))
                   .status(status)
                   .commitMessage(getCommitMessageOfError(error))
                   .gitConnectorId(error.getGitConnectorId())
                   .branchName(error.getBranchName())
                   .triggeredBy(GitFileActivity.TriggeredBy.USER)
                   .build())
        .collect(toList());
  }

  @Override
  public List<GitDetail> fetchRepositoriesAccessibleToUser(String accountId) {
    List<YamlGitConfig> yamlGitConfigs = yamlGitConfigService.getYamlGitConfigAccessibleToUserWithEntityName(accountId);

    if (EmptyPredicate.isEmpty(yamlGitConfigs)) {
      return Collections.emptyList();
    }

    // SettingAttribute collection
    final Set<String> gitConnectorIds =
        yamlGitConfigs.stream().map(yamlGitConfig -> yamlGitConfig.getGitConnectorId()).collect(Collectors.toSet());
    Map<String, String> gitConnectorIdURLMap = getGitConnectorIdNameMap(gitConnectorIds, accountId);
    return createGitDetails(yamlGitConfigs, gitConnectorIdURLMap);
  }

  private Map<String, String> getGitConnectorIdNameMap(Set<String> gitConnectorIds, String accountId) {
    List<SettingAttribute> gitConnectors = wingsPersistence.createQuery(SettingAttribute.class)
                                               .filter(ACCOUNT_ID_KEY, accountId)
                                               .field(ID_KEY)
                                               .in(gitConnectorIds)
                                               .project(SettingAttributeKeys.uuid, true)
                                               .project(SettingAttributeKeys.name, true)
                                               .asList();
    if (isEmpty(gitConnectors)) {
      return Collections.emptyMap();
    }
    return gitConnectors.stream().collect(Collectors.toMap(SettingAttribute::getUuid, SettingAttribute::getName));
  }

  private List<GitDetail> createGitDetails(
      List<YamlGitConfig> yamlGitConfigs, Map<String, String> gitConnectorIdNameMap) {
    if (isEmpty(yamlGitConfigs)) {
      return Collections.emptyList();
    }
    return yamlGitConfigs.stream()
        .map(yamlGitConfig -> createGitDetail(yamlGitConfig, gitConnectorIdNameMap))
        .collect(Collectors.toList());
  }

  private GitDetail createGitDetail(YamlGitConfig yamlGitConfig, Map<String, String> gitConnectorIdNameMap) {
    String gitConnectorName =
        getConnectorNameFromConnectorIdMap(yamlGitConfig.getGitConnectorId(), gitConnectorIdNameMap);
    return buildGitDetail(yamlGitConfig, gitConnectorName);
  }

  private String getConnectorNameFromConnectorIdMap(String gitConnectorId, Map<String, String> gitConnectorIdNameMap) {
    return gitConnectorIdNameMap.getOrDefault(gitConnectorId, UNKNOWN_GIT_CONNECTOR);
  }

  private GitDetail buildGitDetail(YamlGitConfig yamlGitConfig, String gitConnectorName) {
    return GitDetail.builder()
        .branchName(yamlGitConfig.getBranchName())
        .entityName(yamlGitConfig.getEntityName())
        .entityType(yamlGitConfig.getEntityType())
        .gitConnectorId(yamlGitConfig.getGitConnectorId())
        .connectorName(gitConnectorName)
        .yamlGitConfigId(yamlGitConfig.getUuid())
        .appId(yamlGitConfig.getAppId())
        .build();
  }

  @Override
  public PageResponse<GitCommit> fetchGitCommits(
      PageRequest<GitCommit> pageRequest, Boolean gitToHarness, String accountId) {
    pageRequest.addFilter(GitCommitKeys.accountId, SearchFilter.Operator.HAS, accountId);
    if (gitToHarness != null) {
      pageRequest.addFilter("yamlChangeSet.gitToHarness", SearchFilter.Operator.HAS, gitToHarness);
    }
    /* Only these attributes are sufficient for UI consumption, removing other attributes from api response since it was
     * bloating the api response*/
    pageRequest.addFieldsIncluded(GitCommitKeys.accountId);
    pageRequest.addFieldsIncluded(GitCommitKeys.commitId);
    pageRequest.addFieldsIncluded(GitCommitKeys.commitMessage);
    pageRequest.addFieldsIncluded(GitCommitKeys.createdAt);
    pageRequest.addFieldsIncluded(GitCommitKeys.fileProcessingSummary);
    pageRequest.addFieldsIncluded(GitCommitKeys.yamlGitConfigIds);
    pageRequest.addFieldsIncluded(GitCommitKeys.gitConnectorId);
    pageRequest.addFieldsIncluded(GitCommitKeys.branchName);
    pageRequest.addFieldsIncluded(GitCommitKeys.status);
    pageRequest.addFieldsIncluded(GitCommitKeys.yamlChangeSet.concat(".").concat(YamlChangeSetKeys.gitToHarness));
    PageResponse<GitCommit> pageResponse = wingsPersistence.query(GitCommit.class, pageRequest);
    List<GitCommit> gitCommits = pageResponse.getResponse();
    List<GitCommit> gitCommitsWithValidConnectorName = populateConnectorNameInGitCommits(gitCommits, accountId);
    pageResponse.setResponse(gitCommitsWithValidConnectorName);
    return pageResponse;
  }

  private List<GitCommit> populateConnectorNameInGitCommits(List<GitCommit> gitCommits, String accountId) {
    if (isEmpty(gitCommits)) {
      return gitCommits;
    }
    List<String> connectorIdList = gitCommits.stream().map(commit -> commit.getGitConnectorId()).collect(toList());
    Map<String, String> connetorIdNameMap = gitConfigHelperService.getConnectorIdNameMap(connectorIdList, accountId);
    return gitCommits.stream()
        .map(gitCommit -> {
          String connectorName = connetorIdNameMap.getOrDefault(gitCommit.getGitConnectorId(), UNKNOWN_GIT_CONNECTOR);
          gitCommit.setConnectorName(connectorName);
          return gitCommit;
        })
        .collect(toList());
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
                                                              .commitMessage(change.getCommitMessage())
                                                              .build())
                                                   .collect(toList());

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
    if (isNotEmpty(completeChangeList)) {
      logActivityForFiles(gitDiffResult.getCommitId(),
          completeChangeList.stream().map(gitFileChange -> gitFileChange.getFilePath()).collect(toList()),
          Status.SKIPPED, message, accountId);
    }
  }

  private GitFileActivityBuilder buildBaseGitFileActivity(GitFileChange change, String commitId) {
    YamlGitConfig gitConfig = change.getYamlGitConfig();
    String commitIdToPersist = StringUtils.isEmpty(commitId) ? change.getCommitId() : commitId;
    String processingCommitIdToPersist = StringUtils.isEmpty(commitId) ? change.getProcessingCommitId() : commitId;
    final Boolean changeFromAnotherCommit = change.getChangeFromAnotherCommit();
    return GitFileActivity.builder()
        .accountId(change.getAccountId())
        .commitId(commitIdToPersist)
        .processingCommitId(processingCommitIdToPersist)
        .filePath(change.getFilePath())
        .fileContent(change.getFileContent())
        .commitMessage(change.getCommitMessage())
        .changeType(change.getChangeType())
        .gitConnectorId(gitConfig.getGitConnectorId())
        .branchName(gitConfig.getBranchName())
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

  @Override
  public List<ChangeSetDTO> getCommitsWhichAreBeingProcessed(String accountId, String appId, int displayCount) {
    YamlGitConfig yamlGitConfig = yamlGitService.fetchYamlGitConfig(appId, accountId);
    if (yamlGitConfig == null) {
      return null;
    }
    final List<YamlChangeSet.Status> processingStatuses =
        Arrays.asList(YamlChangeSet.Status.QUEUED, YamlChangeSet.Status.RUNNING);

    List<YamlChangeSet> changeSetsWithProcessingStatus =
        yamlChangeSetService.getChangeSetsWithStatus(accountId, appId, yamlGitConfig, displayCount, processingStatuses);

    return changeSetsWithProcessingStatus.stream()
        .map(changeSet -> {
          if (YamlChangeSet.Status.QUEUED.equals(changeSet.getStatus())) {
            return makeDTOForQueuedChangeSet(changeSet, yamlGitConfig, accountId);
          } else {
            return makeDTOForRunningChangeSet(changeSet, yamlGitConfig, accountId);
          }
        })
        .collect(Collectors.toList());
  }

  private ChangeSetDTO makeDTOForRunningChangeSet(
      YamlChangeSet yamlChangeSet, YamlGitConfig yamlGitConfig, String accountId) {
    String gitConnectorName =
        getGitConnectorNameFromId(yamlChangeSet.getGitSyncMetadata().getGitConnectorId(), accountId);
    GitDetail gitDetail = buildGitDetail(yamlGitConfig, gitConnectorName);
    RunningChangesetInformation runningChangesetInformation = RunningChangesetInformation.builder()
                                                                  .startedRunningAt(yamlChangeSet.getLastUpdatedAt())
                                                                  .queuedAt(yamlChangeSet.getCreatedAt())
                                                                  .build();
    return buildChangesetDTO(runningChangesetInformation, gitDetail, yamlChangeSet.isGitToHarness(),
        YamlChangeSet.Status.RUNNING, yamlChangeSet.getUuid());
  }

  private ChangeSetDTO makeDTOForQueuedChangeSet(
      YamlChangeSet yamlChangeSet, YamlGitConfig yamlGitConfig, String accountId) {
    String gitConnectorName =
        getGitConnectorNameFromId(yamlChangeSet.getGitSyncMetadata().getGitConnectorId(), accountId);
    GitDetail gitDetail = buildGitDetail(yamlGitConfig, gitConnectorName);
    QueuedChangesetInformation queuedChangesetInformation =
        QueuedChangesetInformation.builder().queuedAt(yamlChangeSet.getCreatedAt()).build();
    return buildChangesetDTO(queuedChangesetInformation, gitDetail, yamlChangeSet.isGitToHarness(),
        YamlChangeSet.Status.QUEUED, yamlChangeSet.getUuid());
  }

  private ChangeSetDTO buildChangesetDTO(ChangesetInformation changesetInformation, GitDetail gitDetail,
      boolean gitToHarness, YamlChangeSet.Status status, String id) {
    return ChangeSetDTO.builder()
        .changesetInformation(changesetInformation)
        .gitDetail(gitDetail)
        .gitToHarness(gitToHarness)
        .status(status)
        .changeSetId(id)
        .build();
  }

  private String getGitConnectorNameFromId(String gitConnectorId, String accountId) {
    return wingsPersistence.createQuery(SettingAttribute.class)
        .filter(ACCOUNT_ID_KEY, accountId)
        .filter(ID_KEY, gitConnectorId)
        .project(SettingAttributeKeys.uuid, true)
        .project(SettingAttributeKeys.name, true)
        .get()
        .getName();
  }
}
