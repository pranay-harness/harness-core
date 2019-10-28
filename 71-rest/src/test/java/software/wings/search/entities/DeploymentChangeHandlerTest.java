package software.wings.search.entities;

import static io.harness.beans.ExecutionStatus.SUCCESS;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.UJJAWAL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import com.google.inject.Inject;

import io.harness.beans.WorkflowType;
import io.harness.category.element.UnitTests;
import io.harness.rule.OwnerRule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.Application;
import software.wings.beans.ExecutionArgs;
import software.wings.beans.WorkflowExecution;
import software.wings.search.entities.deployment.DeploymentChangeHandler;
import software.wings.search.entities.deployment.DeploymentSearchEntity;
import software.wings.search.framework.SearchDao;
import software.wings.search.framework.changestreams.ChangeEvent;
import software.wings.search.framework.changestreams.ChangeType;

import java.io.IOException;

public class DeploymentChangeHandlerTest extends WingsBaseTest {
  @Mock private SearchDao searchDao;

  @Inject @InjectMocks DeploymentChangeHandler deploymentChangeHandler;
  private WorkflowExecution workflowExecution;
  private ExecutionArgs executionArgs;
  private ChangeEvent workflowExecutionInsertChangeEvent;
  private ChangeEvent workflowExecutionUpdateChangeEvent;
  private String workflowExecutionId = generateUuid();
  private String appId = generateUuid();
  private String appName = "DeploymentChangeHandlerApplication" + System.currentTimeMillis();

  @Before
  public void setup() throws IOException {
    Application application = new Application();
    application.setUuid(appId);
    application.setName(appName);
    wingsPersistence.save(application);

    executionArgs = WorkflowExecutionEntityTestUtils.createExecutionArgs(WorkflowType.PIPELINE);
    assertThat(executionArgs).isNotNull();

    workflowExecution = WorkflowExecutionEntityTestUtils.createWorkflowExecution(
        workflowExecutionId, appId, appName, executionArgs, WorkflowType.PIPELINE, SUCCESS);
    assertThat(workflowExecution).isNotNull();

    workflowExecutionInsertChangeEvent = WorkflowExecutionEntityTestUtils.createWorkflowExecutionChangeEvent(
        WorkflowExecution.class, workflowExecution, ChangeType.INSERT);

    workflowExecutionUpdateChangeEvent = WorkflowExecutionEntityTestUtils.createWorkflowExecutionChangeEvent(
        WorkflowExecution.class, workflowExecution, ChangeType.UPDATE);
  }

  @Test
  @Owner(emails = UJJAWAL)
  @Category(UnitTests.class)
  public void testWorkflowExecutionInsertChange() {
    when(searchDao.upsertDocument(eq(DeploymentSearchEntity.TYPE), eq(workflowExecutionId), any())).thenReturn(true);
    boolean isSuccessful = deploymentChangeHandler.handleChange(workflowExecutionInsertChangeEvent);
    assertThat(isSuccessful).isNotNull();
    assertThat(isSuccessful).isTrue();
  }

  @Test
  @Owner(emails = UJJAWAL)
  @Category(UnitTests.class)
  public void testWorkflowExecutionUpdateChange() {
    when(searchDao.upsertDocument(eq(DeploymentSearchEntity.TYPE), eq(workflowExecutionId), any())).thenReturn(true);
    boolean isSuccessful = deploymentChangeHandler.handleChange(workflowExecutionUpdateChangeEvent);
    assertThat(isSuccessful).isNotNull();
    assertThat(isSuccessful).isTrue();
  }
}
