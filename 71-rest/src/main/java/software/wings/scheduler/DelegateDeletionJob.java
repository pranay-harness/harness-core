package software.wings.scheduler;

import static software.wings.common.Constants.ACCOUNT_ID_KEY;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import io.harness.beans.PageRequest.PageRequestBuilder;
import io.harness.beans.SearchFilter.Operator;
import io.harness.persistence.UuidAware;
import io.harness.scheduler.PersistentScheduler;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.TriggerBuilder;
import software.wings.beans.Account;
import software.wings.beans.Delegate;
import software.wings.beans.Delegate.DelegateKeys;
import software.wings.beans.Delegate.Status;
import software.wings.dl.WingsPersistence;
import software.wings.features.DelegatesFeature;
import software.wings.features.api.UsageLimitedFeature;
import software.wings.helpers.ext.mail.EmailData;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.EmailNotificationService;

import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@DisallowConcurrentExecution
@Slf4j
public class DelegateDeletionJob implements Job {
  private static final String GROUP = "RESTRICT_DELEGATE_COUNT_FOR_ACCOUNT_GROUP";
  private static final String PREFERRED_DELEGATE_TO_RETAIN_KEY = "DELEGATE_TO_RETAIN";
  private static final int MAX_RETRIES = 2;

  @Inject private AccountService accountService;
  @Inject private DelegateService delegateService;
  @Inject private EmailNotificationService emailNotificationService;
  @Inject private WingsPersistence wingsPersistence;
  @Inject @Named(DelegatesFeature.FEATURE_NAME) private UsageLimitedFeature delegatesFeature;

  public static void addWithDelay(
      PersistentScheduler jobScheduler, String accountId, String preferredDelegateToRetain, int delayInMinutes) {
    long startTime = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(delayInMinutes);
    addInternal(jobScheduler, accountId, preferredDelegateToRetain, new Date(startTime));
  }

  private static void addInternal(
      PersistentScheduler jobScheduler, String accountId, String preferredDelegateToRetain, Date triggerStartTime) {
    JobDetail job = JobBuilder.newJob(DelegateDeletionJob.class)
                        .withIdentity(accountId, GROUP)
                        .usingJobData(ACCOUNT_ID_KEY, accountId)
                        .usingJobData(PREFERRED_DELEGATE_TO_RETAIN_KEY, preferredDelegateToRetain)
                        .build();

    TriggerBuilder triggerBuilder =
        TriggerBuilder.newTrigger().withIdentity(accountId, GROUP).startAt(triggerStartTime);
    if (triggerStartTime != null) {
      triggerBuilder.startAt(triggerStartTime);
    }

    jobScheduler.ensureJob__UnderConstruction(job, triggerBuilder.build());
  }

  @Override
  public void execute(JobExecutionContext jobExecutionContext) {
    String accountId = (String) jobExecutionContext.getJobDetail().getJobDataMap().get(ACCOUNT_ID_KEY);
    String preferredDelegateToRetain =
        (String) jobExecutionContext.getJobDetail().getJobDataMap().get(PREFERRED_DELEGATE_TO_RETAIN_KEY);

    int retryCount = 0;
    while (true) {
      try {
        Optional<String> delegateToRetain = selectDelegateToRetain(accountId, preferredDelegateToRetain);

        if (delegateToRetain.isPresent()) {
          logger.info("Deleting all delegates for account : {} except {}", accountId, delegateToRetain.get());

          retainOnlySelectedDelegatesAndDeleteRest(accountId, Collections.singletonList(delegateToRetain.get()));

          logger.info("Deleted all delegates for account : {} except {}", accountId, delegateToRetain.get());
        } else {
          logger.info("No delegate found to retain for account : {}", accountId);
        }

        break;
      } catch (Exception ex) {
        if (retryCount >= MAX_RETRIES) {
          logger.error("Couldn't delete delegates for account: {}. Current Delegate Count : {}", accountId,
              getDelegates(accountId).size(), ex);
          break;
        }
        retryCount++;
      }
    }

    int numDelegates = getDelegates(accountId).size();
    if (numDelegates > delegatesFeature.getMaxUsageAllowedForAccount(accountId)) {
      sendEmailAboutDelegatesOverUsage(accountId, numDelegates);
    }
  }

  private void retainOnlySelectedDelegatesAndDeleteRest(String accountId, List<String> delegatesToRetain)
      throws InterruptedException {
    Query<Delegate> query = wingsPersistence.createQuery(Delegate.class)
                                .filter(DelegateKeys.accountId, accountId)
                                .field(DelegateKeys.delegateName)
                                .notIn(delegatesToRetain);

    UpdateOperations<Delegate> updateOps =
        wingsPersistence.createUpdateOperations(Delegate.class).set(DelegateKeys.status, Status.DELETED);
    wingsPersistence.update(query, updateOps);

    // Waiting for 2 minutes to ensure shutdown msg reach delegates before removing their entries from DB
    TimeUnit.MINUTES.sleep(2);

    delegateService.retainOnlySelectedDelegatesAndDeleteRest(accountId, delegatesToRetain);
  }

  private void sendEmailAboutDelegatesOverUsage(String accountId, int numDelegates) {
    Account account = accountService.get(accountId);
    String body = String.format(
        "Account is using more than [%d] delegates. Account Id : [%s], Company Name : [%s], Account Name : [%s], Delegate Count : [%d]",
        delegatesFeature.getMaxUsageAllowedForAccount(accountId), accountId, account.getCompanyName(),
        account.getAccountName(), numDelegates);
    String subject = String.format(
        "Found account with more than %d delegates", delegatesFeature.getMaxUsageAllowedForAccount(accountId));

    emailNotificationService.send(EmailData.builder()
                                      .hasHtml(false)
                                      .body(body)
                                      .subject(subject)
                                      .to(Lists.newArrayList("support@harness.io"))
                                      .build());
  }

  private Optional<String> selectDelegateToRetain(String accountId, String preferredDelegateToRetain) {
    if (delegateService.get(accountId, preferredDelegateToRetain, true) != null) {
      return Optional.of(preferredDelegateToRetain);
    }

    return getDelegates(accountId)
        .stream()
        .max(Comparator.comparingLong(Delegate::getLastHeartBeat))
        .map(UuidAware::getUuid);
  }

  private List<Delegate> getDelegates(String accountId) {
    return delegateService
        .list(PageRequestBuilder.aPageRequest().addFilter(DelegateKeys.accountId, Operator.EQ, accountId).build())
        .getResponse();
  }
}
