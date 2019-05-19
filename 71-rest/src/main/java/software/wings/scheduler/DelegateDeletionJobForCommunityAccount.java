package software.wings.scheduler;

import static software.wings.common.Constants.ACCOUNT_ID_KEY;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import io.harness.beans.PageRequest.PageRequestBuilder;
import io.harness.beans.SearchFilter.Operator;
import io.harness.persistence.UuidAware;
import io.harness.scheduler.PersistentScheduler;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.TriggerBuilder;
import software.wings.beans.Account;
import software.wings.beans.Delegate;
import software.wings.beans.Delegate.DelegateKeys;
import software.wings.helpers.ext.mail.EmailData;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.EmailNotificationService;

import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@DisallowConcurrentExecution
@Slf4j
public class DelegateDeletionJobForCommunityAccount implements Job {
  private static final String GROUP = "RESTRICT_DELEGATE_COUNT_FOR_COMMUNITY_ACCOUNT_GROUP";
  private static final String PREFERRED_DELEGATE_TO_RETAIN_KEY = "DELEGATE_TO_RETAIN";
  private static final int MAX_DELEGATES_ALLOWED_FOR_COMMUNITY_ACCOUNT = 1;

  @Inject private AccountService accountService;
  @Inject private DelegateService delegateService;
  @Inject private EmailNotificationService emailNotificationService;

  public static void addWithDelay(
      PersistentScheduler jobScheduler, String accountId, String preferredDelegateToRetain, int delayInMinutes) {
    // Add some randomness in the trigger start time to avoid overloading quartz by firing jobs at the same time.
    long startTime = System.currentTimeMillis() + new Random().nextInt((int) TimeUnit.MINUTES.toMillis(delayInMinutes));
    addInternal(jobScheduler, accountId, preferredDelegateToRetain, new Date(startTime));
  }

  private static void addInternal(
      PersistentScheduler jobScheduler, String accountId, String preferredDelegateToRetain, Date triggerStartTime) {
    JobDetail job = JobBuilder.newJob(DelegateDeletionJobForCommunityAccount.class)
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

    if (!accountService.isCommunityAccount(accountId)) {
      return;
    }

    try {
      Optional<String> delegateToRetain = selectDelegateToRetain(accountId, preferredDelegateToRetain);

      if (delegateToRetain.isPresent()) {
        delegateService.retainOnlySelectedDelegatesAndDeleteRest(
            accountId, Collections.singletonList(delegateToRetain.get()));
        logger.info("Deleted all delegates for account : {} except {}", accountId, delegateToRetain.get());
      }
    } catch (Exception ex) {
      logger.error("Couldn't delete delegates for account: {}. Current Delegate Count : {}", accountId,
          getDelegates(accountId).size(), ex);
    }

    /*int numDelegates = getDelegates(accountId).size();
    if (numDelegates > MAX_DELEGATES_ALLOWED_FOR_COMMUNITY_ACCOUNT) {
      sendEmailAboutDelegatesRestrictionViolation(accountId, numDelegates);
    }*/
  }

  private void sendEmailAboutDelegatesRestrictionViolation(String accountId, int numDelegates) {
    Account account = accountService.get(accountId);
    String body = String.format(
        "Community Account is using more than 1 delegate. Account Id : [%s], Company Name : [%s], Account Name : [%s], Delegate Count : [%d]",
        accountId, account.getCompanyName(), account.getAccountName(), numDelegates);
    String subject = "Found Community account with more than 1 delegate";

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
