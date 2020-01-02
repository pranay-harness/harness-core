package software.wings.scheduler;

import static io.harness.rule.OwnerRule.YOGESH_CHAUHAN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import static software.wings.scheduler.ResourceConstraintBackupJob.GROUP;
import static software.wings.scheduler.ResourceConstraintBackupJob.NAME;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.rule.OwnerRule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import software.wings.WingsBaseTest;
import software.wings.service.intfc.ResourceConstraintService;
import software.wings.service.intfc.WorkflowExecutionService;

public class ResourceConstraintBackupJobTest extends WingsBaseTest {
  @Mock WorkflowExecutionService workflowExecutionService;
  @Inject @InjectMocks private ResourceConstraintService resourceConstraintService;

  @Inject ResourceConstraintBackupJob job;

  @Test
  @Owner(developers = YOGESH_CHAUHAN)
  @Category(UnitTests.class)
  public void jobExecute() throws Exception {
    ArgumentCaptor<JobKey> captor = ArgumentCaptor.forClass(JobKey.class);
    JobExecutionContext context = mock(JobExecutionContext.class);

    Scheduler scheduler = mock(Scheduler.class);
    when(context.getScheduler()).thenReturn(scheduler);

    job.execute(context);
    verify(scheduler, times(1)).deleteJob(captor.capture());
    JobKey jobKey = captor.getValue();
    assertThat(jobKey.getGroup()).isEqualTo(GROUP);
    assertThat(jobKey.getName()).isEqualTo(NAME);
  }
}
