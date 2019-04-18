package software.wings.scheduler;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.inject.Inject;

import io.harness.beans.ExecutionStatus;
import io.harness.category.element.UnitTests;
import io.harness.distribution.constraint.Constraint;
import io.harness.distribution.constraint.Constraint.Strategy;
import io.harness.distribution.constraint.ConstraintUnit;
import io.harness.distribution.constraint.Consumer;
import io.harness.distribution.constraint.ConsumerId;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.quartz.JobExecutionContext;
import software.wings.WingsBaseTest;
import software.wings.beans.ResourceConstraint;
import software.wings.beans.ResourceConstraintInstance.ResourceConstraintInstanceKeys;
import software.wings.beans.ResourceConstraintUsage;
import software.wings.beans.WorkflowExecution;
import software.wings.service.intfc.ResourceConstraintService;
import software.wings.service.intfc.WorkflowExecutionService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ResourceConstraintBackupJobTest extends WingsBaseTest {
  @Mock WorkflowExecutionService workflowExecutionService;
  @Inject @InjectMocks private ResourceConstraintService resourceConstraintService;

  @Inject ResourceConstraintBackupJob job;

  @Test
  @Category(UnitTests.class)
  public void jobExecute() throws Exception {
    final String accountId = generateUuid();

    final ResourceConstraint resourceConstraint =
        ResourceConstraint.builder().accountId(accountId).strategy(Strategy.ASAP).capacity(1).name("Foo").build();
    resourceConstraintService.save(resourceConstraint);

    final Constraint constraint = resourceConstraintService.createAbstraction(resourceConstraint);

    final String appId = generateUuid();
    final String workflowExecutionId = generateUuid();

    Map<String, Object> constraintContext = new HashMap();
    constraintContext.put(ResourceConstraintInstanceKeys.appId, appId);
    constraintContext.put(ResourceConstraintInstanceKeys.releaseEntityType, "WORKFLOW");
    constraintContext.put(ResourceConstraintInstanceKeys.releaseEntityId, workflowExecutionId);
    constraintContext.put(ResourceConstraintInstanceKeys.order, 1);

    final Consumer.State state = constraint.registerConsumer(
        new ConstraintUnit("1"), new ConsumerId("1"), 1, constraintContext, resourceConstraintService.getRegistry());

    final WorkflowExecution workflowExecution = WorkflowExecution.builder().build();
    workflowExecution.setStatus(ExecutionStatus.ABORTED);
    when(workflowExecutionService.getWorkflowExecution(appId, workflowExecutionId)).thenReturn(workflowExecution);

    JobExecutionContext context = mock(JobExecutionContext.class);
    job.execute(context);

    final List<ResourceConstraintUsage> usage =
        resourceConstraintService.usage(accountId, asList(resourceConstraint.getUuid()));
    assertThat(usage.size()).isEqualTo(0);
  }
}
