package software.wings.service.impl.yaml.sync;

import static io.harness.beans.PageRequest.DEFAULT_UNLIMITED;
import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.beans.PageResponse.PageResponseBuilder.aPageResponse;
import static io.harness.beans.SearchFilter.Operator.EQ;
import static io.harness.beans.SearchFilter.Operator.IN;
import static io.harness.beans.SortOrder.OrderType.DESC;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.persistence.HPersistence.upsertReturnNewOptions;
import static java.util.Comparator.comparingInt;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.collections4.ListUtils.emptyIfNull;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.mongodb.morphia.aggregation.Accumulator.accumulator;
import static org.mongodb.morphia.aggregation.Group.first;
import static org.mongodb.morphia.aggregation.Group.grouping;
import static org.mongodb.morphia.aggregation.Projection.projection;
import static software.wings.alerts.AlertStatus.Open;
import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.beans.Base.ACCOUNT_ID_KEY;
import static software.wings.beans.Base.APP_ID_KEY;
import static software.wings.beans.Base.ID_KEY;
import static software.wings.beans.EntityType.ACCOUNT;
import static software.wings.beans.EntityType.APPLICATION;
import static software.wings.beans.SettingAttribute.SettingCategory.CONNECTOR;
import static software.wings.beans.yaml.Change.Builder.aFileChange;
import static software.wings.beans.yaml.YamlConstants.APPLICATIONS_FOLDER;
import static software.wings.beans.yaml.YamlConstants.PATH_DELIMITER;
import static software.wings.beans.yaml.YamlConstants.SETUP_FOLDER;
import static software.wings.service.impl.yaml.sync.GitSyncErrorUtils.getCommitIdOfError;
import static software.wings.settings.SettingValue.SettingVariableTypes.GIT;
import static software.wings.yaml.errorhandling.GitSyncError.GitSyncDirection.GIT_TO_HARNESS;
import static software.wings.yaml.errorhandling.GitSyncError.GitSyncDirection.HARNESS_TO_GIT;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.fabric8.utils.Strings;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.beans.SearchFilter;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.UnexpectedException;
import io.harness.persistence.CreatedAtAware;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.mongodb.morphia.query.FindOptions;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.Sort;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.beans.Application;
import software.wings.beans.EntityType;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.SettingAttributeKeys;
import software.wings.beans.alert.Alert;
import software.wings.beans.alert.Alert.AlertKeys;
import software.wings.beans.alert.AlertType;
import software.wings.beans.alert.GitConnectionErrorAlert;
import software.wings.beans.yaml.Change;
import software.wings.beans.yaml.Change.ChangeType;
import software.wings.beans.yaml.GitFileChange;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.yaml.GitSyncErrorStatus;
import software.wings.service.impl.yaml.GitToHarnessErrorCommitStats;
import software.wings.service.impl.yaml.GitToHarnessErrorCommitStats.GitToHarnessErrorCommitStatsKeys;
import software.wings.service.impl.yaml.service.YamlHelper;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.yaml.YamlGitService;
import software.wings.service.intfc.yaml.sync.GitSyncErrorService;
import software.wings.service.intfc.yaml.sync.GitSyncService;
import software.wings.service.intfc.yaml.sync.YamlService;
import software.wings.utils.AlertsUtils;
import software.wings.utils.Utils;
import software.wings.yaml.errorhandling.GitProcessingError;
import software.wings.yaml.errorhandling.GitSyncError;
import software.wings.yaml.errorhandling.GitSyncError.GitSyncErrorKeys;
import software.wings.yaml.errorhandling.GitToHarnessErrorDetails;
import software.wings.yaml.errorhandling.HarnessToGitErrorDetails;
import software.wings.yaml.gitSync.GitFileActivity;
import software.wings.yaml.gitSync.YamlGitConfig;
import software.wings.yaml.gitSync.YamlGitConfig.YamlGitConfigKeys;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Singleton
@Slf4j
public class GitSyncErrorServiceImpl implements GitSyncErrorService {
  public static final String EMPTY_STR = "";
  public static final Long DEFAULT_COMMIT_TIME = 0L;

  private static final EnumSet<GitFileActivity.Status> TERMINATING_STATUSES =
      EnumSet.of(GitFileActivity.Status.EXPIRED, GitFileActivity.Status.DISCARDED);

  private WingsPersistence wingsPersistence;
  private YamlGitService yamlGitService;
  private YamlHelper yamlHelper;
  private AppService appService;
  private YamlService yamlService;
  private GitSyncService gitSyncService;
  private AlertsUtils alertsUtils;
  private SettingsService settingsService;

  @Inject
  public GitSyncErrorServiceImpl(WingsPersistence wingsPersistence, YamlGitService yamlGitService,
      YamlHelper yamlHelper, AppService appService, YamlService yamlService, GitSyncService gitSyncService,
      AlertsUtils alertsUtils, SettingsService settingsService) {
    this.wingsPersistence = wingsPersistence;
    this.yamlGitService = yamlGitService;
    this.yamlHelper = yamlHelper;
    this.appService = appService;
    this.yamlService = yamlService;
    this.gitSyncService = gitSyncService;
    this.alertsUtils = alertsUtils;
    this.settingsService = settingsService;
  }

  private Query<GitSyncError> addGitToHarnessErrorFilter(Query<GitSyncError> gitSyncErrorQuery) {
    return gitSyncErrorQuery.filter(GitSyncErrorKeys.gitSyncDirection, GIT_TO_HARNESS.name());
  }

  private Query<GitSyncError> addHarnessToGitErrorFilter(Query<GitSyncError> gitSyncErrorQuery) {
    return gitSyncErrorQuery.filter(GitSyncErrorKeys.gitSyncDirection, HARNESS_TO_GIT.name());
  }

  @Override
  public long getTotalGitErrorsCount(String accountId) {
    List<Alert> alerts = getGitConnectionAlert(accountId);
    long connectivityIssueCount = alerts == null ? 0 : alerts.size();
    return getGitSyncErrorCount(accountId) + connectivityIssueCount;
  }

  @Override
  public long getGitSyncErrorCount(String accountId) {
    return wingsPersistence.createQuery(GitSyncError.class).filter(GitSyncError.ACCOUNT_ID_KEY, accountId).count();
  }

  @Override
  public PageResponse<GitToHarnessErrorCommitStats> listGitToHarnessErrorsCommits(
      PageRequest<GitToHarnessErrorCommitStats> req, String accountId, String gitConnectorId, String branchName,
      Integer numberOfErrorsInSummary) {
    // Creating a page request to get the commits
    int limit = isBlank(req.getLimit()) ? DEFAULT_UNLIMITED : Integer.parseInt(req.getLimit());
    int offset = isBlank(req.getOffset()) ? 0 : Integer.parseInt(req.getOffset());

    final Query<GitSyncError> query =
        wingsPersistence.createQuery(GitSyncError.class).filter(ACCOUNT_ID_KEY, accountId);

    if (isNotEmpty(gitConnectorId)) {
      query.filter(GitSyncErrorKeys.gitConnectorId, gitConnectorId);
    }

    if (isNotEmpty(branchName)) {
      query.filter(GitSyncErrorKeys.branchName, branchName);
    }

    addGitToHarnessErrorFilter(query);

    List<GitToHarnessErrorCommitStats> commitWiseErrorDetails = new ArrayList<>();
    wingsPersistence.getDatastore(GitSyncError.class)
        .createAggregation(GitSyncError.class)
        .match(query)
        .unwind(GitSyncErrorKeys.additionalErrorDetails)
        .group(GitSyncErrorKeys.gitCommitId, grouping("failedCount", accumulator("$sum", 1)),
            grouping(GitToHarnessErrorCommitStatsKeys.commitTime, first(GitSyncErrorKeys.commitTime)),
            grouping(GitSyncErrorKeys.gitConnectorId, first(GitSyncErrorKeys.gitConnectorId)),
            grouping(GitSyncErrorKeys.branchName, first(GitSyncErrorKeys.branchName)),
            grouping(GitToHarnessErrorCommitStatsKeys.commitMessage, first(GitSyncErrorKeys.commitMessage)),
            grouping(GitToHarnessErrorCommitStatsKeys.errorsForSummaryView,
                grouping("$push", projection(GitSyncErrorKeys.yamlFilePath, GitSyncErrorKeys.yamlFilePath),
                    projection(GitSyncErrorKeys.failureReason, GitSyncErrorKeys.failureReason))))
        .project(projection("gitCommitId", "_id"), projection("failedCount"),
            projection(GitToHarnessErrorCommitStatsKeys.commitTime), projection(GitSyncErrorKeys.gitConnectorId),
            projection(GitSyncErrorKeys.branchName), projection(GitToHarnessErrorCommitStatsKeys.commitMessage),
            projection(GitToHarnessErrorCommitStatsKeys.errorsForSummaryView))
        .sort(Sort.descending(GitToHarnessErrorCommitStatsKeys.commitTime))
        .limit(limit)
        .skip(offset)
        .aggregate(GitToHarnessErrorCommitStats.class)
        .forEachRemaining(commitWiseErrorDetails::add);

    List<GitToHarnessErrorCommitStats> filteredCommitDetails =
        filterCommitsWithValidConnectorAndPopulateConnectorName(commitWiseErrorDetails, accountId);

    removeExtraErrorsFromCommitDetails(filteredCommitDetails, numberOfErrorsInSummary);

    return aPageResponse()
        .withTotal(filteredCommitDetails.size())
        .withLimit(String.valueOf(limit))
        .withOffset(String.valueOf(offset))
        .withResponse(filteredCommitDetails)
        .build();
  }

  private void removeExtraErrorsFromCommitDetails(
      List<GitToHarnessErrorCommitStats> commitDetails, Integer numberOfErrorsInSummary) {
    if (numberOfErrorsInSummary == null) {
      return;
    }

    for (GitToHarnessErrorCommitStats commitDetail : commitDetails) {
      List<GitSyncError> allErrors = commitDetail.getErrorsForSummaryView();
      if (isNotEmpty(allErrors) && allErrors.size() > numberOfErrorsInSummary) {
        sortErrorsByFileProcessingOrder(allErrors);
        commitDetail.setErrorsForSummaryView(allErrors.subList(0, numberOfErrorsInSummary));
      }
    }
  }

  private List<GitToHarnessErrorCommitStats> filterCommitsWithValidConnectorAndPopulateConnectorName(
      List<GitToHarnessErrorCommitStats> commitWiseErrorDetails, String accountId) {
    if (isEmpty(commitWiseErrorDetails)) {
      return Collections.emptyList();
    }
    List<String> connectorIdList =
        commitWiseErrorDetails.stream().map(GitToHarnessErrorCommitStats::getGitConnectorId).collect(toList());
    Map<String, String> connetorIdNameMap = getConnectorIdNameMap(connectorIdList, accountId);
    List<GitToHarnessErrorCommitStats> filteredCommitDetails = new ArrayList<>();
    for (GitToHarnessErrorCommitStats commitStats : commitWiseErrorDetails) {
      String gitConnectorId = commitStats.getGitConnectorId();
      if (connetorIdNameMap.containsKey(gitConnectorId)) {
        commitStats.setGitConnectorName(connetorIdNameMap.get(gitConnectorId));
        filteredCommitDetails.add(commitStats);
      }
    }
    return filteredCommitDetails;
  }

  private PageRequest<GitSyncError> addHarnessToGitErrorFilter(PageRequest<GitSyncError> gitSyncErrorPageRequest) {
    gitSyncErrorPageRequest.addFilter(GitSyncErrorKeys.gitSyncDirection, EQ, HARNESS_TO_GIT.name());
    return gitSyncErrorPageRequest;
  }

  private PageRequest<GitSyncError> addGitToHarnessErrorFilter(PageRequest<GitSyncError> gitSyncErrorPageRequest) {
    gitSyncErrorPageRequest.addFilter(GitSyncErrorKeys.gitSyncDirection, EQ, GIT_TO_HARNESS.name());
    return gitSyncErrorPageRequest;
  }

  @Override
  public PageResponse<GitSyncError> fetchErrorsInEachCommits(PageRequest<GitSyncError> req, String gitCommitId,
      String accountId, List<String> includeDataList, String yamlFilePathPattern) {
    int limit = isBlank(req.getLimit()) ? DEFAULT_UNLIMITED : Integer.parseInt(req.getLimit());
    int offset = isBlank(req.getOffset()) ? 0 : Integer.parseInt(req.getOffset());

    Query<GitSyncError> query = wingsPersistence.createQuery(GitSyncError.class).filter(ACCOUNT_ID_KEY, accountId);

    addGitToHarnessErrorFilter(query);
    boolean returnPreviousErrors = false;
    if (isNotEmpty(includeDataList)) {
      returnPreviousErrors = includeDataList.contains("obsoleteErrorFiles");
    }

    if (returnPreviousErrors) {
      query.disableValidation().or(query.criteria(GitSyncErrorKeys.gitCommitId).equal(gitCommitId),
          query.criteria(GitSyncErrorKeys.previousCommitIds).hasAnyOf(Collections.singletonList(gitCommitId)));
    } else {
      query.disableValidation().filter(GitSyncErrorKeys.gitCommitId, gitCommitId);
    }

    if (isNotBlank(yamlFilePathPattern)) {
      query.field(GitSyncErrorKeys.yamlFilePath).containsIgnoreCase(yamlFilePathPattern);
    }
    List<GitSyncError> allGitSyncErrors = query.asList(new FindOptions().skip(offset).limit(limit));

    if (isEmpty(allGitSyncErrors)) {
      // Ideally this case won't happen, but if somehow the number of errors
      // in a commit becomes 0, we will return a empty response
      logger.info("The gitcommitId {} of account {} has zero git sync errors", gitCommitId, accountId);
      return aPageResponse()
          .withTotal(0)
          .withLimit(String.valueOf(limit))
          .withOffset(String.valueOf(offset))
          .withResponse(Collections.emptyList())
          .build();
    }
    List<GitSyncError> filteredGitSyncError =
        allGitSyncErrors.stream().map(error -> getActualErrorForThatCommit(error, gitCommitId)).collect(toList());
    sortErrorsByFileProcessingOrder(filteredGitSyncError);

    return aPageResponse()
        .withTotal(filteredGitSyncError.size())
        .withLimit(String.valueOf(limit))
        .withOffset(String.valueOf(offset))
        .withResponse(filteredGitSyncError)
        .build();
  }

  @Override
  public PageResponse<GitSyncError> listAllGitToHarnessErrors(
      PageRequest<GitSyncError> req, String accountId, String gitConnectorId, String branchName) {
    if (isNotEmpty(gitConnectorId)) {
      req.addFilter(GitSyncErrorKeys.gitConnectorId, EQ, gitConnectorId);
    }

    if (isNotEmpty(branchName)) {
      req.addFilter(GitSyncErrorKeys.branchName, EQ, branchName);
    }
    addGitToHarnessErrorFilter(req);
    PageResponse<GitSyncError> allErrorsResponse = fetchErrors(req);
    List<GitSyncError> errorsReturnedInRequest = allErrorsResponse.getResponse();
    if (isEmpty(errorsReturnedInRequest)) {
      return allErrorsResponse;
    }
    List<GitSyncError> errorsWithoutPreviousErrorsFields =
        errorsReturnedInRequest.stream().map(error -> removePreviousErrorField(error)).collect(toList());
    List<GitSyncError> filteredErrorsWithConnectorName =
        filterErrorsWithValidConnectorAndPopulateConnectorName(errorsWithoutPreviousErrorsFields, accountId);

    allErrorsResponse.setResponse(filteredErrorsWithConnectorName);
    return allErrorsResponse;
  }

  private GitSyncError removePreviousErrorField(GitSyncError error) {
    GitToHarnessErrorDetails gitToHarnessErrorDetails;
    try {
      gitToHarnessErrorDetails = (GitToHarnessErrorDetails) error.getAdditionalErrorDetails();
    } catch (Exception e) {
      throw new UnexpectedException("The previous errors fields should only be queried for git to harness errros ", e);
    }
    gitToHarnessErrorDetails.setPreviousErrors(Collections.emptyList());
    return error;
  }

  private void sortErrorsByFileProcessingOrder(List<GitSyncError> gitSyncErrors) {
    if (isEmpty(gitSyncErrors)) {
      logger.info("The errors list given for the sorting is empty");
      return;
    }
    final List<String> filePathsSortedInProcessingOrder = getFilePathsSortedInProcessingOrder(gitSyncErrors);
    gitSyncErrors.sort(getProcessingOrderComparator(filePathsSortedInProcessingOrder));
  }

  private Comparator<GitSyncError> getProcessingOrderComparator(List<String> filePathsSortedInProcessingOrder) {
    final Map<String, Integer> filePathToSortedIndexMap = getFilePathToIndexMap(filePathsSortedInProcessingOrder);
    return comparingInt(gitSyncError -> filePathToSortedIndexMap.get(gitSyncError.getYamlFilePath()));
  }

  @NotNull
  private HashMap<String, Integer> getFilePathToIndexMap(@NotNull List<String> filePathsSortedInProcessingOrder) {
    final HashMap<String, Integer> filePathToSortedIndexMap = new HashMap<>();
    for (int index = 0; index < filePathsSortedInProcessingOrder.size(); index++) {
      filePathToSortedIndexMap.put(filePathsSortedInProcessingOrder.get(index), index);
    }
    return filePathToSortedIndexMap;
  }

  private List<String> getFilePathsSortedInProcessingOrder(List<GitSyncError> gitSyncErrors) {
    if (isEmpty(gitSyncErrors)) {
      return Collections.emptyList();
    }
    final List<Change> changesConstructedFromErrors =
        gitSyncErrors.stream().map(this ::convertToChange).collect(toList());
    yamlService.sortByProcessingOrder(changesConstructedFromErrors);

    return changesConstructedFromErrors.stream().map(Change::getFilePath).collect(toList());
  }

  private Change convertToChange(GitSyncError gitSyncError) {
    return aFileChange()
        .withFilePath(gitSyncError.getYamlFilePath())
        .withChangeType(Utils.getEnumFromString(ChangeType.class, gitSyncError.getChangeType()))
        .withAccountId(gitSyncError.getAccountId())
        .withSyncFromGit(true)
        .build();
  }

  private GitSyncError getActualErrorForThatCommit(GitSyncError gitSyncError, String gitCommitId) {
    if (getCommitIdOfError(gitSyncError).equals(gitCommitId)) {
      ((GitToHarnessErrorDetails) gitSyncError.getAdditionalErrorDetails()).setLatestErrorDetailForFile(null);
      // not required by UI so not sending the previous errors list
      ((GitToHarnessErrorDetails) gitSyncError.getAdditionalErrorDetails()).setPreviousErrors(Collections.emptyList());
      return gitSyncError;
    }
    return getPreviousGitSyncError(gitSyncError, gitCommitId);
  }

  private GitSyncError getPreviousGitSyncError(GitSyncError gitSyncError, String gitCommitId) {
    GitToHarnessErrorDetails gitToHarnessErrorDetails =
        (GitToHarnessErrorDetails) gitSyncError.getAdditionalErrorDetails();
    List<GitSyncError> previousGitSyncErrors = gitToHarnessErrorDetails.getPreviousErrors();
    if (isEmpty(previousGitSyncErrors)) {
      throw new UnexpectedException("The given git sync error doesn't contain the previous commits information");
    }

    GitSyncError previousGitSyncError =
        previousGitSyncErrors.stream()
            .filter(error
                -> ((GitToHarnessErrorDetails) error.getAdditionalErrorDetails()).getGitCommitId().equals(gitCommitId))
            .findFirst()
            .orElseThrow(()
                             -> new UnexpectedException(
                                 "The given git sync error doesn't contain the previous commits information"));
    ((GitToHarnessErrorDetails) previousGitSyncError.getAdditionalErrorDetails())
        .populateCommitWithLatestErrorDetails(gitSyncError);
    return previousGitSyncError;
  }

  @Override
  public <T extends Change> void upsertGitSyncErrors(
      T failedChange, String errorMessage, boolean fullSyncPath, boolean gitToHarness) {
    if (gitToHarness) {
      upsertGitToHarnessError(failedChange, errorMessage);
    } else {
      upsertHarnessToGitError(failedChange, errorMessage, fullSyncPath);
    }
  }

  private <T extends Change> void upsertHarnessToGitError(T failedChange, String errorMessage, boolean fullSyncPath) {
    Query<GitSyncError> fetchQuery = wingsPersistence.createQuery(GitSyncError.class)
                                         .filter(GitSyncError.ACCOUNT_ID_KEY, failedChange.getAccountId())
                                         .filter(GitSyncErrorKeys.yamlFilePath, failedChange.getFilePath());
    addHarnessToGitErrorFilter(fetchQuery);

    String appId = obtainAppIdFromGitFileChange(failedChange.getAccountId(), failedChange.getFilePath());
    logger.info(String.format("Upsert haress to git issue for file: %s", failedChange.getFilePath()));
    GitFileChange failedGitFileChange = (GitFileChange) failedChange;
    UpdateOperations<GitSyncError> failedUpdateOperations =
        wingsPersistence.createUpdateOperations(GitSyncError.class)
            .setOnInsert(GitSyncError.ID_KEY, generateUuid())
            .set(GitSyncError.ACCOUNT_ID_KEY, failedChange.getAccountId())
            .set(GitSyncErrorKeys.gitSyncDirection, HARNESS_TO_GIT.name())
            .set(GitSyncErrorKeys.yamlFilePath, failedChange.getFilePath())
            .set(GitSyncErrorKeys.changeType, failedChange.getChangeType().name())
            .set(GitSyncErrorKeys.failureReason,
                errorMessage != null ? errorMessage : "Reason could not be captured. Logs might have some info")
            .set(
                GitSyncErrorKeys.additionalErrorDetails, getHarnessToGitErrorDetails(failedGitFileChange, fullSyncPath))
            .set(APP_ID_KEY, appId);
    populateGitDetails(failedUpdateOperations, failedGitFileChange, appId);
    fetchQuery.project(GitSyncError.ID_KEY, true);
    wingsPersistence.upsert(fetchQuery, failedUpdateOperations, upsertReturnNewOptions);
  }

  private <T extends Change> void upsertGitToHarnessError(T failedChange, String errorMessage) {
    Query<GitSyncError> fetchQuery = wingsPersistence.createQuery(GitSyncError.class)
                                         .filter(GitSyncError.ACCOUNT_ID_KEY, failedChange.getAccountId())
                                         .filter(GitSyncErrorKeys.yamlFilePath, failedChange.getFilePath());
    addGitToHarnessErrorFilter(fetchQuery);

    GitFileChange failedGitFileChange = (GitFileChange) failedChange;
    String appId = obtainAppIdFromGitFileChange(failedChange.getAccountId(), failedChange.getFilePath());
    logger.info("Upsert git to harness sync issue for file: [{}]", failedChange.getFilePath());

    UpdateOperations<GitSyncError> failedUpdateOperations =
        wingsPersistence.createUpdateOperations(GitSyncError.class)
            .setOnInsert(GitSyncError.ID_KEY, generateUuid())
            .set(GitSyncError.ACCOUNT_ID_KEY, failedChange.getAccountId())
            .set(GitSyncErrorKeys.gitSyncDirection, GIT_TO_HARNESS.name())
            .set(GitSyncErrorKeys.yamlFilePath, failedChange.getFilePath())
            .set(GitSyncErrorKeys.changeType, failedChange.getChangeType().name())
            .set(GitSyncErrorKeys.failureReason,
                errorMessage != null ? errorMessage : "Reason could not be captured. Logs might have some info")
            .set(APP_ID_KEY, appId);

    populateGitDetails(failedUpdateOperations, failedGitFileChange, appId);

    GitToHarnessErrorDetails gitToHarnessErrorDetails = getGitToHarnessErrorDetails(failedGitFileChange);
    final GitSyncError previousGitSyncError = fetchQuery.get();
    if (previousGitSyncError != null) {
      if (!failedGitFileChange.getChangeFromAnotherCommit()) {
        GitToHarnessErrorDetails oldGitToHarnessErrorDetails =
            (GitToHarnessErrorDetails) previousGitSyncError.getAdditionalErrorDetails();
        // Reading the previous errors of the file
        List<GitSyncError> previousGitSyncErrors =
            new ArrayList<>(emptyIfNull(oldGitToHarnessErrorDetails.getPreviousErrors()));
        List<String> previousCommitIds =
            new ArrayList<>(emptyIfNull(oldGitToHarnessErrorDetails.getPreviousCommitIdsWithError()));
        logger.info("Adding the error with the commitId [{}] to the previous commit list of file [{}]",
            getCommitIdOfError(previousGitSyncError), failedChange.getFilePath());
        // Setting the value of the previous details as empty as this record will not go to the previous list
        previousGitSyncError.setUuid(generateUuid());
        ((GitToHarnessErrorDetails) previousGitSyncError.getAdditionalErrorDetails())
            .setPreviousErrors(Collections.emptyList());
        ((GitToHarnessErrorDetails) previousGitSyncError.getAdditionalErrorDetails())
            .setPreviousCommitIdsWithError(Collections.emptyList());
        // adding the new entry to the list
        previousGitSyncErrors.add(previousGitSyncError);
        previousCommitIds.add(getCommitIdOfError(previousGitSyncError));
        // For the new error updating all its fields with the appropriate values
        gitToHarnessErrorDetails.setPreviousErrors(previousGitSyncErrors);
        gitToHarnessErrorDetails.setPreviousCommitIdsWithError(previousCommitIds);
      }
    } else {
      logger.info("Creating a new error record for the file [{}] in account", failedChange.getFilePath());
      gitToHarnessErrorDetails.setPreviousErrors(Collections.emptyList());
      gitToHarnessErrorDetails.setPreviousCommitIdsWithError(Collections.emptyList());
    }
    failedUpdateOperations.set(GitSyncErrorKeys.additionalErrorDetails, gitToHarnessErrorDetails);
    fetchQuery.project(GitSyncError.ID_KEY, true);
    wingsPersistence.upsert(fetchQuery, failedUpdateOperations, upsertReturnNewOptions);
  }

  private String obtainAppIdFromGitFileChange(String accountId, String yamlFilePath) {
    String appId = GLOBAL_APP_ID;

    // Fetch appName from yamlPath, e.g. Setup/Applications/App1/Services/S1/index.yaml -> App1,
    // Setup/Artifact Servers/server.yaml -> null
    String appName = yamlHelper.getAppName(yamlFilePath);
    if (StringUtils.isNotBlank(appName)) {
      Application app = appService.getAppByName(accountId, appName);
      if (app != null) {
        appId = app.getUuid();
      }
    }

    return appId;
  }

  private HarnessToGitErrorDetails getHarnessToGitErrorDetails(GitFileChange failedChange, boolean fullSyncPath) {
    return HarnessToGitErrorDetails.builder().fullSyncPath(fullSyncPath).build();
  }

  private GitToHarnessErrorDetails getGitToHarnessErrorDetails(GitFileChange failedGitFileChange) {
    String failedCommitId = failedGitFileChange.getCommitId() != null ? failedGitFileChange.getCommitId() : "";
    if (failedCommitId.equals("")) {
      logger.info("Unexpected behaviour: The git commitId is null for the git to harness error");
    }
    return GitToHarnessErrorDetails.builder()
        .gitCommitId(failedCommitId)
        .yamlContent(failedGitFileChange.getFileContent())
        .commitTime(failedGitFileChange.getCommitTimeMs())
        .commitMessage(failedGitFileChange.getCommitMessage())
        .build();
  }

  private <T extends Change> void populateGitDetails(
      UpdateOperations<GitSyncError> failedUpdateOperations, GitFileChange failedGitFileChange, String appId) {
    final YamlGitConfig yamlGitConfig = failedGitFileChange.getYamlGitConfig() != null
        ? failedGitFileChange.getYamlGitConfig()
        : fetchYamlGitConfig(appId, failedGitFileChange.getAccountId());

    if (yamlGitConfig != null) {
      final String gitConnectorId = Strings.emptyIfNull(yamlGitConfig.getGitConnectorId());
      final String branchName = Strings.emptyIfNull(yamlGitConfig.getBranchName());
      failedUpdateOperations.set(GitSyncErrorKeys.gitConnectorId, gitConnectorId);
      failedUpdateOperations.set(GitSyncErrorKeys.branchName, branchName);
      failedUpdateOperations.set(GitSyncErrorKeys.yamlGitConfigId, yamlGitConfig.getUuid());
    }
  }

  private YamlGitConfig fetchYamlGitConfig(String appId, String accountId) {
    if (isNotEmpty(appId) && isNotEmpty(accountId)) {
      final String entityId = GLOBAL_APP_ID.equals(appId) ? accountId : appId;
      final EntityType entityType = GLOBAL_APP_ID.equals(appId) ? ACCOUNT : APPLICATION;
      return yamlGitService.get(accountId, entityId, entityType);
    }
    return null;
  }

  @Override
  public PageResponse<GitSyncError> fetchErrors(PageRequest<GitSyncError> req) {
    return wingsPersistence.query(GitSyncError.class, req);
  }

  @Override
  public PageResponse<GitSyncError> fetchHarnessToGitErrors(
      PageRequest<GitSyncError> req, String accountId, String gitConnectorId, String branchName) {
    req.addFilter(GitSyncErrorKeys.accountId, EQ, accountId);
    if (isNotEmpty(gitConnectorId)) {
      req.addFilter(GitSyncErrorKeys.gitConnectorId, EQ, gitConnectorId);
    }

    if (isNotEmpty(branchName)) {
      req.addFilter(GitSyncErrorKeys.branchName, EQ, branchName);
    }
    addHarnessToGitErrorFilter(req);

    PageResponse<GitSyncError> response = fetchErrors(req);
    List<GitSyncError> filteredErrors =
        filterErrorsWithValidConnectorAndPopulateConnectorName(response.getResponse(), accountId);
    return aPageResponse()
        .withTotal(response.getTotal())
        .withResponse(filteredErrors)
        .withLimit(response.getLimit())
        .withOffset(response.getOffset())
        .build();
  }

  private List<GitSyncError> filterErrorsWithValidConnectorAndPopulateConnectorName(
      List<GitSyncError> gitSyncErrors, String accountId) {
    if (isEmpty(gitSyncErrors)) {
      return Collections.emptyList();
    }
    List<String> connectorIdList = gitSyncErrors.stream().map(error -> error.getGitConnectorId()).collect(toList());
    Map<String, String> connetorIdNameMap = getConnectorIdNameMap(connectorIdList, accountId);
    List<GitSyncError> filteredErrors = new ArrayList<>();

    for (GitSyncError error : gitSyncErrors) {
      String gitConnectorId = error.getGitConnectorId();
      if (connetorIdNameMap.containsKey(gitConnectorId)) {
        error.setGitConnectorName(connetorIdNameMap.get(gitConnectorId));
        filteredErrors.add(error);
      }
    }
    return filteredErrors;
  }

  @Override
  public List<GitSyncError> getActiveGitToHarnessSyncErrors(
      String accountId, String gitConnectorId, String branchName, long fromTimestamp) {
    //  get all app ids sharing same repo and branch name
    final Set<String> allowedAppIdSet = appIdsSharingRepoBranch(accountId, gitConnectorId, branchName);
    if (isEmpty(allowedAppIdSet)) {
      return Collections.emptyList();
    }
    final Query<GitSyncError> query = wingsPersistence.createQuery(GitSyncError.class)
                                          .filter(ACCOUNT_ID_KEY, accountId)
                                          .field(CreatedAtAware.CREATED_AT_KEY)
                                          .greaterThan(fromTimestamp);

    addGitToHarnessErrorFilter(query);
    addGitRepositoryFilter(query, gitConnectorId, branchName);
    addActiveErrorFilter(query);

    final List<GitSyncError> gitSyncErrorList = emptyIfNull(query.asList());

    return gitSyncErrorList.stream()
        .filter(gitSyncError -> errorOfAllowedApp(gitSyncError, allowedAppIdSet) || isNewAppError(gitSyncError))
        .collect(toList());
  }
  private Query<GitSyncError> addActiveErrorFilter(Query<GitSyncError> query) {
    query.or(query.criteria(GitSyncErrorKeys.status).doesNotExist(),
        query.criteria(GitSyncErrorKeys.status).equal(GitSyncErrorStatus.ACTIVE));
    return query;
  }

  private Query<GitSyncError> addGitRepositoryFilter(
      Query<GitSyncError> query, String gitConnectorId, String branchName) {
    return query.filter(GitSyncErrorKeys.branchName, branchName)
        .filter(GitSyncErrorKeys.gitConnectorId, gitConnectorId);
  }

  private boolean isNewAppError(GitSyncError error) {
    return GLOBAL_APP_ID.equals(error.getAppId())
        && error.getYamlFilePath().startsWith(SETUP_FOLDER + PATH_DELIMITER + APPLICATIONS_FOLDER);
  }
  private boolean errorOfAllowedApp(GitSyncError error, Set<String> allowedAppIdSet) {
    return allowedAppIdSet.contains(error.getAppId());
  }

  private Set<String> appIdsSharingRepoBranch(String accountId, String gitConnectorId, String branchName) {
    final List<YamlGitConfig> yamlGitConfigList = wingsPersistence.createQuery(YamlGitConfig.class)
                                                      .project(YamlGitConfigKeys.entityId, true)
                                                      .project(YamlGitConfigKeys.entityType, true)
                                                      .filter(ACCOUNT_ID_KEY, accountId)
                                                      .filter(YamlGitConfigKeys.enabled, Boolean.TRUE)
                                                      .filter(YamlGitConfigKeys.gitConnectorId, gitConnectorId)
                                                      .filter(YamlGitConfigKeys.branchName, branchName)
                                                      .asList();

    return emptyIfNull(yamlGitConfigList)
        .stream()
        .map(
            yamlGitConfig -> yamlGitConfig.getEntityType() == APPLICATION ? yamlGitConfig.getEntityId() : GLOBAL_APP_ID)
        .collect(Collectors.toSet());
  }

  @Override
  public void deleteGitSyncErrorAndLogFileActivity(
      List<String> gitSyncErrorsIds, GitFileActivity.Status status, String accountId) {
    if (isEmpty(gitSyncErrorsIds)) {
      throw new InvalidRequestException("No git sync error Id provided in the discard api");
    }
    PageRequest<GitSyncError> req = aPageRequest()
                                        .addFieldsIncluded("_id")
                                        .addFilter(ACCOUNT_ID_KEY, SearchFilter.Operator.EQ, accountId)
                                        .addFilter("_id", IN, gitSyncErrorsIds.toArray())
                                        .build();

    List<GitSyncError> gitSyncErrors = wingsPersistence.query(GitSyncError.class, req).getResponse();
    if (isEmpty(gitSyncErrors)) {
      logger.info(" None of the errors provided for the discard are latest git sync error");
      return;
    }
    wingsPersistence.save(gitSyncService.getActivitiesForGitSyncErrors(gitSyncErrors, status));
    if (TERMINATING_STATUSES.contains(status)) {
      final List<String> discardErrorIds =
          gitSyncErrors.stream().map(error -> error.getUuid()).collect(Collectors.toList());
      logger.info(String.format("Marking errors with ids: %s as %s", discardErrorIds, status.name()));
      deleteGitSyncErrors(discardErrorIds, accountId);
    }
  }

  private void deleteGitSyncErrors(List<String> errorIds, String accountId) {
    Query query = wingsPersistence.createQuery(GitSyncError.class);
    query.filter(ACCOUNT_ID_KEY, accountId);
    query.field(ID_KEY).in(errorIds);
    wingsPersistence.delete(query);
    alertsUtils.closeAlertIfApplicable(accountId, false);
  }

  private GitProcessingError getGitProcessingError(Alert alert) {
    GitConnectionErrorAlert alertData = (GitConnectionErrorAlert) alert.getAlertData();
    if (alertData == null) {
      throw new UnexpectedException("The alert data is null in the  alert with id " + alert.getUuid());
    }
    return GitProcessingError.builder()
        .message(alert.getTitle())
        .accountId(alert.getAccountId())
        .createdAt(alert.getCreatedAt())
        .branchName(alertData.getBranchName())
        .gitConnectorId(alertData.getGitConnectorId())
        .build();
  }

  private Map<String, String> getConnectorIdNameMap(List<String> connectorIds, String accountId) {
    if (isEmpty(connectorIds)) {
      return Collections.emptyMap();
    }
    PageRequest<SettingAttribute> settingAttributeQuery = aPageRequest()
                                                              .addFilter(SettingAttributeKeys.accountId, EQ, accountId)
                                                              .addFilter(SettingAttributeKeys.category, EQ, CONNECTOR)
                                                              .addFilter(SettingAttributeKeys.valueType, EQ, GIT)
                                                              .addFilter(ID_KEY, IN, connectorIds.toArray())
                                                              .build();
    List<SettingAttribute> settingAttributeList = settingsService.list(settingAttributeQuery, null, null);
    if (isEmpty(settingAttributeList)) {
      return Collections.emptyMap();
    }
    return settingAttributeList.stream().collect(toMap(SettingAttribute::getUuid, SettingAttribute::getName));
  }

  private GitProcessingError getGitProcessingErrorWithConnectorName(
      GitProcessingError gitProcessingError, Map<String, String> connectorNameIdMap) {
    if (gitProcessingError == null) {
      return null;
    }
    String gitConnectorId = gitProcessingError.getGitConnectorId();
    if (isBlank(gitProcessingError.getGitConnectorId()) || !connectorNameIdMap.containsKey(gitConnectorId)) {
      gitProcessingError.setConnectorName(null);
      gitProcessingError.setBranchName(null);
    } else {
      gitProcessingError.setConnectorName(connectorNameIdMap.get(gitConnectorId));
    }
    return gitProcessingError;
  }

  private void populateTheConnectorName(
      List<GitProcessingError> gitProcessingErrors, Map<String, String> connectorNameIdMap) {
    if (isEmpty(gitProcessingErrors)) {
      return;
    }
    // We need to filter because maybe someone deleted the connector and
    // alert still uses that gitconnectorId
    gitProcessingErrors.forEach(e -> getGitProcessingErrorWithConnectorName(e, connectorNameIdMap));
  }

  private List<Alert> getGitConnectionAlert(String accountId) {
    PageRequest<Alert> gitAlertRequest = aPageRequest()
                                             .addFilter(AlertKeys.accountId, EQ, accountId)
                                             .addFilter(AlertKeys.type, EQ, AlertType.GitConnectionError)
                                             .addFilter(AlertKeys.status, EQ, Open)
                                             .addOrder(AlertKeys.createdAt, DESC)
                                             .build();
    return wingsPersistence.query(Alert.class, gitAlertRequest).getResponse();
  }

  @Override
  public PageResponse<GitProcessingError> fetchGitConnectivityIssues(
      PageRequest<GitProcessingError> req, String accountId) {
    List<Alert> gitProcessingAlerts = getGitConnectionAlert(accountId);
    if (isEmpty(gitProcessingAlerts)) {
      return aPageResponse().withTotal(0).withResponse(Collections.emptyList()).build();
    }
    List<GitProcessingError> gitProcessingErrors =
        gitProcessingAlerts.stream().map(this ::getGitProcessingError).collect(toList());
    List<String> connectorIds =
        gitProcessingErrors.stream().map(GitProcessingError::getGitConnectorId).distinct().collect(toList());
    Map<String, String> connectorNameIdMap = getConnectorIdNameMap(connectorIds, accountId);
    populateTheConnectorName(gitProcessingErrors, connectorNameIdMap);
    return aPageResponse().withTotal(gitProcessingErrors.size()).withResponse(gitProcessingErrors).build();
  }
}
