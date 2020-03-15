package software.wings.yaml.gitSync;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.mongo.iterator.MongoPersistenceIterator.SchedulingType.REGULAR;
import static java.time.Duration.ofMinutes;
import static java.time.Duration.ofSeconds;
import static software.wings.beans.Account.AccountKeys;

import com.google.inject.Inject;

import io.harness.beans.PageRequest.PageRequestBuilder;
import io.harness.beans.PageResponse;
import io.harness.beans.SearchFilter;
import io.harness.iterator.PersistenceIteratorFactory;
import io.harness.mongo.iterator.MongoPersistenceIterator;
import io.harness.mongo.iterator.MongoPersistenceIterator.Handler;
import software.wings.beans.Account;
import software.wings.service.impl.yaml.GitSyncService;
import software.wings.yaml.errorhandling.GitSyncError;
import software.wings.yaml.gitSync.GitFileActivity.Status;

import java.util.ArrayList;
import java.util.List;

public class GitSyncErrorHandler implements Handler<Account> {
  @Inject private PersistenceIteratorFactory persistenceIteratorFactory;
  @Inject private GitSyncService gitSyncService;
  private static long ONE_MONTH_IN_MILLIS = 2592000000L;

  public void registerIterators() {
    persistenceIteratorFactory.createPumpIteratorWithDedicatedThreadPool(
        PersistenceIteratorFactory.PumpExecutorOptions.builder()
            .name("GitSyncErrorExpiryCheck")
            .poolSize(1)
            .interval(ofMinutes(60))
            .build(),
        GitSyncErrorHandler.class,
        MongoPersistenceIterator.<Account>builder()
            .clazz(Account.class)
            .fieldName(AccountKeys.gitSyncExpiryCheckIteration)
            .targetInterval(ofMinutes(60))
            .acceptableNoAlertDelay(ofMinutes(120))
            .acceptableExecutionTime(ofSeconds(15))
            .handler(this)
            .schedulingType(REGULAR)
            .redistribute(true));
  }
  @Override
  public void handle(Account account) {
    PageResponse<GitSyncError> pageResponse;
    int total = 0;
    String offset = "0";
    String limit = "100";
    long expiryMillis = System.currentTimeMillis() - ONE_MONTH_IN_MILLIS;
    List<GitSyncError> errorsToBeDeleted = new ArrayList<>();
    do {
      pageResponse = gitSyncService.fetchErrors(PageRequestBuilder.aPageRequest()
                                                    .addFilter("accountId", SearchFilter.Operator.EQ, account.getUuid())
                                                    .addFilter("createdAt", SearchFilter.Operator.LT, expiryMillis)
                                                    .withLimit(limit)
                                                    .withOffset(offset)
                                                    .build());
      errorsToBeDeleted.addAll(pageResponse.getResponse());
      total += pageResponse.getPageSize();
      offset = String.valueOf(total);
    } while (total <= pageResponse.getTotal());

    if (isNotEmpty(errorsToBeDeleted)) {
      gitSyncService.updateGitSyncErrorStatus(errorsToBeDeleted, Status.EXPIRED, account.getUuid());
    }
  }
}