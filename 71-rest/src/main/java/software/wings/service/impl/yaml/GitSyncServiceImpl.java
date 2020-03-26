package software.wings.service.impl.yaml;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.beans.PageResponse.PageResponseBuilder.aPageResponse;
import static io.harness.beans.SearchFilter.Operator.EQ;
import static io.harness.beans.SortOrder.OrderType.DESC;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static org.mongodb.morphia.aggregation.Accumulator.accumulator;
import static org.mongodb.morphia.aggregation.Group.first;
import static org.mongodb.morphia.aggregation.Group.grouping;
import static software.wings.alerts.AlertStatus.Open;
import static software.wings.beans.Base.ACCOUNT_ID_KEY;
import static software.wings.beans.Base.APP_ID_KEY;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.beans.SearchFilter;
import io.harness.data.structure.EmptyPredicate;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.Sort;
import software.wings.beans.Application;
import software.wings.beans.EntityType;
import software.wings.beans.GitCommit;
import software.wings.beans.GitCommit.GitCommitKeys;
import software.wings.beans.alert.Alert;
import software.wings.beans.alert.Alert.AlertKeys;
import software.wings.beans.alert.AlertType;
import software.wings.beans.yaml.Change;
import software.wings.dl.WingsPersistence;
import software.wings.exception.YamlProcessingException;
import software.wings.service.intfc.AppService;
import software.wings.utils.AlertsUtils;
import software.wings.yaml.errorhandling.GitProcessingError;
import software.wings.yaml.errorhandling.GitSyncError;
import software.wings.yaml.errorhandling.GitSyncError.GitSyncErrorKeys;
import software.wings.yaml.gitSync.GitFileActivity;
import software.wings.yaml.gitSync.GitFileActivity.GitFileActivityBuilder;
import software.wings.yaml.gitSync.GitFileActivity.Status;
import software.wings.yaml.gitSync.GitFileProcessingSummary;
import software.wings.yaml.gitSync.YamlGitConfig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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
  @Inject private AppService appService;

  @Override
  public void updateGitSyncErrorStatus(List<GitSyncError> gitSyncErrors, Status status, String accountId) {
    try {
      if (Arrays.asList(Status.EXPIRED, Status.DISCARDED).contains(status)) {
        deleteExpiredGitSyncErrors(
            gitSyncErrors.stream().map(error -> error.getUuid()).collect(Collectors.toList()), accountId);
      }
      wingsPersistence.save(getActivitiesForGitSyncErrors(gitSyncErrors, status));
    } catch (Exception ex) {
      logger.error("Error while discarding git sync error", ex);
    }
  }

  private List<GitFileActivity> getActivitiesForGitSyncErrors(final List<GitSyncError> errors, Status status) {
    if (isEmpty(errors)) {
      return Collections.EMPTY_LIST;
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

  private void deleteExpiredGitSyncErrors(List<String> errorIds, String accountId) {
    Query query = wingsPersistence.createAuthorizedQuery(GitSyncError.class);
    query.filter("accountId", accountId);
    query.field("_id").in(errorIds);
    wingsPersistence.delete(query);
    alertsUtils.closeAlertIfApplicable(accountId, false);
  }

  @Override
  public List<Application> fetchRepositories(String accountId) {
    // TODO Add changes for GLOBAL_APP_ID
    final List<YamlGitConfig> yamlGitConfigList = wingsPersistence.createQuery(YamlGitConfig.class)
                                                      .filter(ACCOUNT_ID_KEY, accountId)
                                                      .filter("enabled", Boolean.TRUE)
                                                      .filter(YamlGitConfig.ENTITY_TYPE_KEY, EntityType.APPLICATION)
                                                      .asList();

    if (isEmpty(yamlGitConfigList)) {
      return Collections.EMPTY_LIST;
    }

    Map<String, YamlGitConfig> yamlGitConfigMap =
        yamlGitConfigList.stream().collect(Collectors.toMap(YamlGitConfig::getEntityId, identity()));

    final List<Application> applicationsWithYamlGitConfigEnabled =
        wingsPersistence.createQuery(Application.class)
            .filter(ACCOUNT_ID_KEY, accountId)
            .field(APP_ID_KEY)
            .in(yamlGitConfigList.stream()
                    .map(yamlGitConfig -> yamlGitConfig.getEntityId())
                    .collect(Collectors.toList()))
            .asList();

    if (isEmpty(applicationsWithYamlGitConfigEnabled)) {
      return Collections.EMPTY_LIST;
    }

    applicationsWithYamlGitConfigEnabled.forEach(app -> { app.setYamlGitConfig(yamlGitConfigMap.get(app.getUuid())); });
    return applicationsWithYamlGitConfigEnabled;
  }

  @Override
  public PageResponse<GitCommit> fetchGitCommits(PageRequest<GitCommit> pageRequest, String accountId) {
    pageRequest.addFilter(GitCommitKeys.accountId, SearchFilter.Operator.HAS, accountId);
    return wingsPersistence.query(GitCommit.class, pageRequest);
  }

  @Override
  public GitFileProcessingSummary logFileActivityAndGenerateProcessingSummary(List<Change> changeList,
      Map<String, YamlProcessingException.ChangeWithErrorMsg> failedYamlFileChangeMap, Status status,
      String errorMessage) {
    try {
      List<Change> changeListCopy = new ArrayList<>(changeList);
      List<GitFileActivity> activities = new LinkedList<>();
      if (EmptyPredicate.isEmpty(failedYamlFileChangeMap)) {
        // if files got skipped before git processing, mark them as FAILURE
        if (status == Status.SKIPPED) {
          activities =
              changeListCopy.stream()
                  .map(change
                      -> buildBaseGitFileActivity(change).status(Status.FAILED).errorMessage(errorMessage).build())
                  .collect(Collectors.toList());
        }
      } else {
        // mark all files in failedYamlFileChangeMap as FAILURE
        activities.addAll(failedYamlFileChangeMap.entrySet()
                              .stream()
                              .map(e -> {
                                final Change change = e.getValue().getChange();
                                // keep removing FAILURE files from original list of files, to obtain remaining
                                // COMPLETED ones
                                changeListCopy.remove(change);
                                return buildBaseGitFileActivity(change)
                                    .status(Status.FAILED)
                                    .errorMessage(e.getValue().getErrorMsg())
                                    .build();
                              })
                              .collect(Collectors.toList()));
        // files successfully processed
        if (isNotEmpty(changeListCopy)) {
          activities.addAll(changeListCopy.stream()
                                .map(change -> buildBaseGitFileActivity(change).status(Status.SUCCESS).build())
                                .collect(Collectors.toList()));
        }
      }
      if (isNotEmpty(activities)) {
        wingsPersistence.save(activities);
      }
      return getFileProcessingSummary(activities);
    } catch (Exception ex) {
      logger.error("Error while logging activities", ex);
    }
    return null;
  }

  private GitFileActivityBuilder buildBaseGitFileActivity(Change change) {
    return GitFileActivity.builder()
        .accountId(change.getAccountId())
        .commitId(change.getCommitId())
        .filePath(change.getFilePath())
        .fileContent(change.getFileContent())
        .triggeredBy(change.isSyncFromGit() ? GitFileActivity.TriggeredBy.GIT : GitFileActivity.TriggeredBy.FULL_SYNC);
  }

  @Override
  public PageResponse<GitSyncError> fetchErrors(PageRequest<GitSyncError> req) {
    return wingsPersistence.query(GitSyncError.class, req);
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

  @Override
  public PageResponse<GitFileActivity> fetchGitSyncActivity(PageRequest<GitFileActivity> req) {
    return wingsPersistence.query(GitFileActivity.class, req);
  }

  public GitFileProcessingSummary getFileProcessingSummary(final List<GitFileActivity> activities) {
    return GitFileProcessingSummary.builder()
        .totalCount(activities.size())
        .failureCount(activities.stream().filter(activity -> activity.getStatus() == Status.FAILED).count())
        .successCount(activities.stream().filter(activity -> activity.getStatus() == Status.SUCCESS).count())
        .build();
  }
}
