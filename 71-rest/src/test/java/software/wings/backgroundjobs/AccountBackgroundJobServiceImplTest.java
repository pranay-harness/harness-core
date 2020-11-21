package software.wings.backgroundjobs;

import static io.harness.rule.OwnerRule.VOJIN;

import static software.wings.beans.Account.Builder.anAccount;
import static software.wings.common.Constants.HARNESS_NAME;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.JOB_NAME;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import io.harness.category.element.UnitTests;
import io.harness.perpetualtask.PerpetualTaskService;
import io.harness.perpetualtask.PerpetualTaskState;
import io.harness.perpetualtask.internal.PerpetualTaskRecord;
import io.harness.rule.Owner;
import io.harness.scheduler.PersistentScheduler;

import software.wings.WingsBaseTest;
import software.wings.beans.Account;
import software.wings.beans.AccountStatus;
import software.wings.service.intfc.AccountService;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.util.Arrays;
import java.util.UUID;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.quartz.JobKey;
import org.quartz.SchedulerException;

public class AccountBackgroundJobServiceImplTest extends WingsBaseTest {
  @InjectMocks @Inject private AccountBackgroundJobServiceImpl accountBackgroundJobService;
  @Mock private AccountService accountService;
  @Mock private PerpetualTaskService perpetualTaskService;
  @Mock @Named("BackgroundJobScheduler") private PersistentScheduler persistentScheduler;

  private static final String GROUP_NAME_1 = "GROUP_NAME_1";
  private static final String GROUP_NAME_2 = "GROUP_NAME_2";
  private static final String PERPETUAL_TASK_UUID_1 = UUID.randomUUID().toString();
  private static final String PERPETUAL_TASK_UUID_2 = UUID.randomUUID().toString();

  @Test
  @Owner(developers = VOJIN)
  @Category(UnitTests.class)
  public void testManageBackgroundJobs_ActiveToExpired() throws SchedulerException {
    JobKey jobKey1 = new JobKey(JOB_NAME, GROUP_NAME_1);
    JobKey jobKey2 = new JobKey(JOB_NAME, GROUP_NAME_2);
    PerpetualTaskRecord perpetualTaskRecord1 =
        PerpetualTaskRecord.builder().accountId(ACCOUNT_ID).uuid(PERPETUAL_TASK_UUID_1).build();
    PerpetualTaskRecord perpetualTaskRecord2 =
        PerpetualTaskRecord.builder().accountId(ACCOUNT_ID).uuid(PERPETUAL_TASK_UUID_2).build();

    Account account = anAccount()
                          .withCompanyName(HARNESS_NAME)
                          .withAccountName(HARNESS_NAME)
                          .withUuid(ACCOUNT_ID)
                          .withBackgroundJobsDisabled(false)
                          .build();

    when(persistentScheduler.getAllJobKeysForAccount(ACCOUNT_ID)).thenReturn(Arrays.asList(jobKey1, jobKey2));
    when(perpetualTaskService.listAllTasksForAccount(ACCOUNT_ID))
        .thenReturn(Arrays.asList(perpetualTaskRecord1, perpetualTaskRecord2));
    when(accountService.getAccountStatus(ACCOUNT_ID)).thenReturn(AccountStatus.EXPIRED);
    when(accountService.get(ACCOUNT_ID)).thenReturn(account);

    accountBackgroundJobService.manageBackgroundJobsForAccount(ACCOUNT_ID);

    verify(persistentScheduler, times(0)).pauseAllQuartzJobsForAccount(ACCOUNT_ID);
    verify(perpetualTaskService, times(0)).pauseTask(ACCOUNT_ID, PERPETUAL_TASK_UUID_1);
    verify(perpetualTaskService, times(0)).pauseTask(ACCOUNT_ID, PERPETUAL_TASK_UUID_2);
  }

  @Test
  @Owner(developers = VOJIN)
  @Category(UnitTests.class)
  public void testManageBackgroundJobs_ExpiredToActive() throws SchedulerException {
    JobKey jobKey1 = new JobKey(JOB_NAME, GROUP_NAME_1);
    JobKey jobKey2 = new JobKey(JOB_NAME, GROUP_NAME_2);
    PerpetualTaskRecord perpetualTaskRecord1 = PerpetualTaskRecord.builder()
                                                   .accountId(ACCOUNT_ID)
                                                   .uuid(PERPETUAL_TASK_UUID_1)
                                                   .state(PerpetualTaskState.TASK_PAUSED)
                                                   .build();
    PerpetualTaskRecord perpetualTaskRecord2 = PerpetualTaskRecord.builder()
                                                   .accountId(ACCOUNT_ID)
                                                   .uuid(PERPETUAL_TASK_UUID_2)
                                                   .state(PerpetualTaskState.TASK_UNASSIGNED)
                                                   .build();
    Account account = anAccount()
                          .withCompanyName(HARNESS_NAME)
                          .withAccountName(HARNESS_NAME)
                          .withUuid(ACCOUNT_ID)
                          .withBackgroundJobsDisabled(true)
                          .build();

    when(persistentScheduler.getAllJobKeysForAccount(ACCOUNT_ID)).thenReturn(Arrays.asList(jobKey1, jobKey2));
    when(perpetualTaskService.listAllTasksForAccount(ACCOUNT_ID))
        .thenReturn(Arrays.asList(perpetualTaskRecord1, perpetualTaskRecord2));
    when(accountService.getAccountStatus(ACCOUNT_ID)).thenReturn(AccountStatus.ACTIVE);
    when(accountService.get(ACCOUNT_ID)).thenReturn(account);

    accountBackgroundJobService.manageBackgroundJobsForAccount(ACCOUNT_ID);

    verify(persistentScheduler, times(1)).resumeAllQuartzJobsForAccount(ACCOUNT_ID);
    verify(perpetualTaskService, times(1)).resumeTask(ACCOUNT_ID, PERPETUAL_TASK_UUID_1);
  }

  @Test
  @Owner(developers = VOJIN)
  @Category(UnitTests.class)
  public void testManageBackgroundJobs_Active() throws SchedulerException {
    JobKey jobKey1 = new JobKey(JOB_NAME, GROUP_NAME_1);
    JobKey jobKey2 = new JobKey(JOB_NAME, GROUP_NAME_2);
    PerpetualTaskRecord perpetualTaskRecord1 = PerpetualTaskRecord.builder()
                                                   .accountId(ACCOUNT_ID)
                                                   .uuid(PERPETUAL_TASK_UUID_1)
                                                   .state(PerpetualTaskState.TASK_ASSIGNED)
                                                   .build();
    PerpetualTaskRecord perpetualTaskRecord2 = PerpetualTaskRecord.builder()
                                                   .accountId(ACCOUNT_ID)
                                                   .uuid(PERPETUAL_TASK_UUID_2)
                                                   .state(PerpetualTaskState.TASK_ASSIGNED)
                                                   .build();
    Account account = anAccount()
                          .withCompanyName(HARNESS_NAME)
                          .withAccountName(HARNESS_NAME)
                          .withUuid(ACCOUNT_ID)
                          .withBackgroundJobsDisabled(false)
                          .build();

    when(persistentScheduler.getAllJobKeysForAccount(ACCOUNT_ID)).thenReturn(Arrays.asList(jobKey1, jobKey2));
    when(perpetualTaskService.listAllTasksForAccount(ACCOUNT_ID))
        .thenReturn(Arrays.asList(perpetualTaskRecord1, perpetualTaskRecord2));
    when(accountService.getAccountStatus(ACCOUNT_ID)).thenReturn(AccountStatus.ACTIVE);
    when(accountService.get(ACCOUNT_ID)).thenReturn(account);

    accountBackgroundJobService.manageBackgroundJobsForAccount(ACCOUNT_ID);

    verifyZeroInteractions(persistentScheduler);
    verifyZeroInteractions(perpetualTaskService);
  }

  @Test
  @Owner(developers = VOJIN)
  @Category(UnitTests.class)
  public void testManageBackgroundJobs_Expired() throws SchedulerException {
    JobKey jobKey1 = new JobKey(JOB_NAME, GROUP_NAME_1);
    JobKey jobKey2 = new JobKey(JOB_NAME, GROUP_NAME_2);
    PerpetualTaskRecord perpetualTaskRecord1 = PerpetualTaskRecord.builder()
                                                   .accountId(ACCOUNT_ID)
                                                   .uuid(PERPETUAL_TASK_UUID_1)
                                                   .state(PerpetualTaskState.TASK_ASSIGNED)
                                                   .build();
    PerpetualTaskRecord perpetualTaskRecord2 = PerpetualTaskRecord.builder()
                                                   .accountId(ACCOUNT_ID)
                                                   .uuid(PERPETUAL_TASK_UUID_2)
                                                   .state(PerpetualTaskState.TASK_ASSIGNED)
                                                   .build();
    Account account = anAccount()
                          .withCompanyName(HARNESS_NAME)
                          .withAccountName(HARNESS_NAME)
                          .withUuid(ACCOUNT_ID)
                          .withBackgroundJobsDisabled(true)
                          .build();

    when(persistentScheduler.getAllJobKeysForAccount(ACCOUNT_ID)).thenReturn(Arrays.asList(jobKey1, jobKey2));
    when(perpetualTaskService.listAllTasksForAccount(ACCOUNT_ID))
        .thenReturn(Arrays.asList(perpetualTaskRecord1, perpetualTaskRecord2));
    when(accountService.getAccountStatus(ACCOUNT_ID)).thenReturn(AccountStatus.EXPIRED);
    when(accountService.get(ACCOUNT_ID)).thenReturn(account);

    accountBackgroundJobService.manageBackgroundJobsForAccount(ACCOUNT_ID);

    verifyZeroInteractions(persistentScheduler);
    verifyZeroInteractions(perpetualTaskService);
  }
}
