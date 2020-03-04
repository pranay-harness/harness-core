package software.wings.yaml.gitSync;

import static io.harness.mongo.iterator.MongoPersistenceIterator.SchedulingType.REGULAR;
import static java.time.Duration.ofDays;
import static java.time.Duration.ofMinutes;
import static java.time.Duration.ofSeconds;

import com.google.inject.Inject;

import io.harness.iterator.PersistenceIteratorFactory;
import io.harness.mongo.iterator.MongoPersistenceIterator;
import io.harness.mongo.iterator.MongoPersistenceIterator.Handler;
import software.wings.service.impl.yaml.GitSyncService;
import software.wings.yaml.errorhandling.GitSyncError;
import software.wings.yaml.errorhandling.GitSyncError.GitSyncErrorKeys;
import software.wings.yaml.gitSync.GitFileActivity.Status;

import java.util.Arrays;

public class GitSyncErrorHandler implements Handler<GitSyncError> {
  @Inject private PersistenceIteratorFactory persistenceIteratorFactory;
  @Inject private GitSyncService gitSyncService;

  public void registerIterators() {
    persistenceIteratorFactory.createPumpIteratorWithDedicatedThreadPool(
        PersistenceIteratorFactory.PumpExecutorOptions.builder()
            .name("GitSyncErrorExpiryCheck")
            .poolSize(2)
            .interval(ofMinutes(60))
            .build(),
        GitSyncErrorHandler.class,
        MongoPersistenceIterator.<GitSyncError>builder()
            .clazz(GitSyncError.class)
            .fieldName(GitSyncErrorKeys.nextIteration)
            .targetInterval(ofDays(90))
            .acceptableNoAlertDelay(ofMinutes(60))
            .acceptableExecutionTime(ofSeconds(15))
            .handler(this)
            .schedulingType(REGULAR)
            .redistribute(true));
  }
  @Override
  public void handle(GitSyncError gitSyncError) {
    gitSyncService.updateGitSyncErrorStatus(Arrays.asList(gitSyncError), Status.EXPIRED, gitSyncError.getAccountId());
  }
}