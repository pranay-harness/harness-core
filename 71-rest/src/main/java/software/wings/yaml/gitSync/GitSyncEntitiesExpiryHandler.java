package software.wings.yaml.gitSync;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.mongo.iterator.MongoPersistenceIterator.SchedulingType.REGULAR;
import static java.time.Duration.ofHours;
import static java.time.Duration.ofMinutes;
import static software.wings.beans.Account.AccountKeys;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;

import io.harness.beans.PageRequest.PageRequestBuilder;
import io.harness.beans.PageResponse;
import io.harness.beans.SearchFilter;
import io.harness.iterator.PersistenceIteratorFactory;
import io.harness.mongo.iterator.MongoPersistenceIterator;
import io.harness.mongo.iterator.MongoPersistenceIterator.Handler;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.mongodb.morphia.query.FindOptions;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.Sort;
import software.wings.beans.Account;
import software.wings.beans.GitCommit;
import software.wings.beans.GitFileActivitySummary;
import software.wings.beans.GitFileActivitySummary.GitFileActivitySummaryKeys;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.yaml.sync.GitSyncErrorService;
import software.wings.service.intfc.yaml.sync.GitSyncService;
import software.wings.yaml.errorhandling.GitSyncError;
import software.wings.yaml.errorhandling.GitSyncError.GitSyncErrorKeys;
import software.wings.yaml.gitSync.GitFileActivity.GitFileActivityKeys;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class GitSyncEntitiesExpiryHandler implements Handler<Account> {
  @Inject private PersistenceIteratorFactory persistenceIteratorFactory;
  @Inject private GitSyncService gitSyncService;
  @Inject private AppService appService;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private GitSyncErrorService gitSyncErrorService;

  private static long ONE_MONTH_IN_MILLIS = 2592000000L;
  private static long TWELVE_MONTH_IN_MILLIS = 31104000000L;

  public void registerIterators() {
    persistenceIteratorFactory.createPumpIteratorWithDedicatedThreadPool(
        PersistenceIteratorFactory.PumpExecutorOptions.builder()
            .name("GitSyncEntitiyExpiryCheck")
            .poolSize(1)
            .interval(ofHours(12))
            .build(),
        GitSyncEntitiesExpiryHandler.class,
        MongoPersistenceIterator.<Account>builder()
            .clazz(Account.class)
            .fieldName(AccountKeys.gitSyncExpiryCheckIteration)
            .targetInterval(ofHours(12))
            .acceptableNoAlertDelay(ofMinutes(120))
            .acceptableExecutionTime(ofMinutes(5))
            .handler(this)
            .schedulingType(REGULAR)
            .redistribute(true));
  }

  @Override
  public void handle(Account account) {
    long oneMonthExpiryMillis = System.currentTimeMillis() - ONE_MONTH_IN_MILLIS;
    long twelveMonthExpiryMillis = System.currentTimeMillis() - TWELVE_MONTH_IN_MILLIS;

    handleGitCommitInGitFileActivitySummary(account, twelveMonthExpiryMillis);
    handleGitFileActivity(account, twelveMonthExpiryMillis);
  }

  @VisibleForTesting
  void handleGitCommitInGitFileActivitySummary(Account account, long expiryMillis) {
    List<String> appIds = appService.getAppIdsByAccountId(account.getUuid());
    List<String> expiredCommits =
        appIds.stream()
            .map(appId -> {
              int total = 0;
              String offset = "0";
              String limit = "1000";
              List<String> commitsExpired = new ArrayList<>();
              PageResponse<GitFileActivitySummary> pageResponse;
              do {
                pageResponse = gitSyncService.fetchGitCommits(
                    PageRequestBuilder.aPageRequest()
                        .addFilter(GitFileActivitySummaryKeys.accountId, SearchFilter.Operator.EQ, account.getUuid())
                        .addFilter(GitFileActivitySummaryKeys.lastUpdatedAt, SearchFilter.Operator.LT, expiryMillis)
                        .withLimit(limit)
                        .withOffset(offset)
                        .build(),
                    null, appId, account.getUuid());
                commitsExpired.addAll(pageResponse.getResponse()
                                          .stream()
                                          .map(GitFileActivitySummary::getUuid)
                                          .collect(Collectors.toList()));
                total += pageResponse.getPageSize();
                offset = String.valueOf(total);
              } while (total <= pageResponse.getTotal());
              return commitsExpired;
            })
            .flatMap(List::stream)
            .collect(Collectors.toList());

    if (isNotEmpty(expiredCommits)) {
      boolean deleted = gitSyncService.deleteGitCommits(expiredCommits, account.getUuid());
      if (deleted) {
        logger.info(
            "Deleted expired Git Commits for account {}. Expired Ids are: {} ", account.getUuid(), expiredCommits);
      }
    }
  }

  @VisibleForTesting
  void handleGitFileActivity(Account account, long expiryMillis) {
    List<String> appIds = appService.getAppIdsByAccountId(account.getUuid());

    List<String> expiredActivities =
        appIds.stream()
            .map(appId -> {
              int total = 0;
              String offset = "0";
              String limit = "1000";
              List<String> activitiesExpired = new ArrayList<>();
              PageResponse<GitFileActivity> pageResponse;
              do {
                pageResponse = gitSyncService.fetchGitSyncActivity(
                    PageRequestBuilder.aPageRequest()
                        .addFilter(GitFileActivityKeys.accountId, SearchFilter.Operator.EQ, account.getUuid())
                        .addFilter(GitFileActivityKeys.lastUpdatedAt, SearchFilter.Operator.LT, expiryMillis)
                        .withLimit(limit)
                        .withOffset(offset)
                        .build(),
                    account.getUuid(), appId, false);
                activitiesExpired.addAll(
                    pageResponse.getResponse().stream().map(GitFileActivity::getUuid).collect(Collectors.toList()));
                total += pageResponse.getPageSize();
                offset = String.valueOf(total);
              } while (total <= pageResponse.getTotal());
              return activitiesExpired;
            })
            .flatMap(List::stream)
            .collect(Collectors.toList());

    if (isNotEmpty(expiredActivities)) {
      boolean deleted = gitSyncService.deleteGitActivity(expiredActivities, account.getUuid());
      if (deleted) {
        logger.info("Deleted expired Git File Activity for account {}. Expired Ids are: {} ", account.getUuid(),
            expiredActivities);
      }
    }
  }

  @VisibleForTesting
  void handleGitError(Account account, long expiryMillis) {
    Query<GitSyncError> query =
        wingsPersistence.createQuery(GitSyncError.class).filter(GitSyncErrorKeys.accountId, account.getUuid());
    query.criteria(GitSyncError.CREATED_AT_KEY).lessThan(expiryMillis);
    List<GitSyncError> gitSyncErrorToBeDeleted = query.asList();
    List<String> errorIds = gitSyncErrorToBeDeleted.stream().map(GitSyncError::getUuid).collect(Collectors.toList());
    boolean deleted = gitSyncErrorService.deleteGitSyncErrors(errorIds, account.getUuid());
    if (deleted) {
      logger.info(
          "Deleted expired Git File Activity for account {}. Expired Ids are: {} ", account.getUuid(), errorIds);
    }
  }

  @VisibleForTesting
  void handleGitCommits(Account account, long expiryMillis) {
    boolean deleted;
    do {
      Query<GitCommit> gitCommitsQuery = wingsPersistence.createQuery(GitCommit.class)
                                             .filter(GitCommit.ACCOUNT_ID_KEY, account.getUuid())
                                             .order(Sort.descending(GitCommit.CREATED_AT_KEY));
      gitCommitsQuery.criteria(GitCommit.CREATED_AT_KEY).lessThan(expiryMillis);
      List<GitCommit> gitCommits = gitCommitsQuery.asList(new FindOptions().limit(1000));
      // Removing topmost (as it can be head).
      List<GitCommit> gitCommitsToBeDeleted = new ArrayList<>();
      Set<Pair> connectorBranchSet = new HashSet<>();
      for (GitCommit gitCommit : gitCommits) {
        if (connectorBranchSet.contains(Pair.of(gitCommit.getGitConnectorId(), gitCommit.getBranchName()))) {
          gitCommitsToBeDeleted.add(gitCommit);
        } else {
          // Adding first successful commit to set and hence will delete all the commits post this.
          if (GitCommit.GIT_COMMIT_PROCESSED_STATUS.contains(gitCommit.getStatus())) {
            connectorBranchSet.add(Pair.of(gitCommit.getGitConnectorId(), gitCommit.getBranchName()));
          }
        }
      }
      List<String> gitCommitIds = gitCommitsToBeDeleted.stream().map(GitCommit::getUuid).collect(Collectors.toList());
      deleted = wingsPersistence.delete(wingsPersistence.createQuery(GitCommit.class)
                                            .filter(GitCommit.ACCOUNT_ID_KEY, account.getUuid())
                                            .field(GitCommit.ID_KEY)
                                            .in(gitCommitIds));
      if (deleted) {
        logger.info(
            "Deleted expired Git File Activity for account {}. Expired Ids are: {} ", account.getUuid(), gitCommitIds);
      }
    } while (deleted);
  }
}