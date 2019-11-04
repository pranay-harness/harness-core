package software.wings.search.entities.deployment;

import static io.harness.beans.ExecutionStatus.SUCCESS;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import com.google.inject.Inject;

import io.harness.beans.WorkflowType;
import io.harness.category.element.UnitTests;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.ExecutionArgs;
import software.wings.beans.Pipeline;
import software.wings.beans.Service;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowExecution;
import software.wings.search.entities.application.ApplicationEntityTestUtils;
import software.wings.search.entities.deployment.DeploymentView.DeploymentViewKeys;
import software.wings.search.entities.environment.EnvironmentEntityTestUtils;
import software.wings.search.entities.pipeline.PipelineEntityTestUtils;
import software.wings.search.entities.service.ServiceEntityTestUtils;
import software.wings.search.entities.workflow.WorkflowEntityTestUtils;
import software.wings.search.framework.SearchDao;
import software.wings.search.framework.changestreams.ChangeEvent;
import software.wings.search.framework.changestreams.ChangeType;

import java.io.IOException;

public class DeploymentChangeHandlerTest extends WingsBaseTest {
  @Mock private SearchDao searchDao;
  @Inject @InjectMocks DeploymentChangeHandler deploymentChangeHandler;

  private static final String APP_NAME = "DeploymentHandlerTestForApplication" + System.currentTimeMillis();
  private static final String SERVICE_NAME = "DeploymentHandlerTestForService" + System.currentTimeMillis();
  private static final String ENVIRONMENT_NAME = "DeploymentHandlerTestForEnvironment" + System.currentTimeMillis();
  private static final String WORKFLOW_NAME = "DeploymentHandlerTestForWorkflow" + System.currentTimeMillis();
  private static final String PIPELINE_NAME = "DeploymentHandlerTestForPipeline" + System.currentTimeMillis();

  private Application application;
  private Service service;
  private Environment environment;
  private Workflow workflow;
  private Pipeline pipeline;
  private WorkflowExecution workflowExecution;
  private ExecutionArgs executionArgs;
  private ChangeEvent workflowExecutionInsertChangeEvent;
  private ChangeEvent workflowExecutionUpdateChangeEvent;
  private ChangeEvent workflowExecutionDeleteChangeEvent;
  private String workflowExecutionId = generateUuid();
  private String accountId = generateUuid();
  private String appId = generateUuid();
  private String serviceId = generateUuid();
  private String environmentId = generateUuid();
  private String workflowId = generateUuid();
  private String pipelineId = generateUuid();

  @Before
  public void setup() throws IOException {
    application = ApplicationEntityTestUtils.createApplication(accountId, appId, APP_NAME);
    assertThat(application).isNotNull();
    wingsPersistence.save(application);

    service = ServiceEntityTestUtils.createService(accountId, appId, serviceId, SERVICE_NAME);
    assertThat(service).isNotNull();
    wingsPersistence.save(service);

    environment = EnvironmentEntityTestUtils.createEnvironment(accountId, appId, environmentId, ENVIRONMENT_NAME);
    assertThat(environment).isNotNull();
    wingsPersistence.save(environment);

    workflow = WorkflowEntityTestUtils.createWorkflow(accountId, appId, workflowId, environmentId, WORKFLOW_NAME);
    assertThat(workflow).isNotNull();
    wingsPersistence.save(workflow);

    pipeline = PipelineEntityTestUtils.createPipeline(accountId, appId, pipelineId, PIPELINE_NAME);
    assertThat(pipeline).isNotNull();
    wingsPersistence.save(pipeline);

    executionArgs = DeploymentEntityTestUtils.createExecutionArgs(WorkflowType.PIPELINE);
    assertThat(executionArgs).isNotNull();

    workflowExecution = DeploymentEntityTestUtils.createWorkflowExecution(
        workflowExecutionId, appId, APP_NAME, executionArgs, WorkflowType.PIPELINE, SUCCESS);
    assertThat(workflowExecution).isNotNull();

    workflowExecutionInsertChangeEvent = DeploymentEntityTestUtils.createWorkflowExecutionChangeEvent(
        WorkflowExecution.class, workflowExecution, ChangeType.INSERT);
    assertThat(workflowExecutionInsertChangeEvent).isNotNull();

    workflowExecutionUpdateChangeEvent = DeploymentEntityTestUtils.createWorkflowExecutionChangeEvent(
        WorkflowExecution.class, workflowExecution, ChangeType.UPDATE);
    assertThat(workflowExecutionUpdateChangeEvent).isNotNull();

    workflowExecutionDeleteChangeEvent = DeploymentEntityTestUtils.createWorkflowExecutionChangeEvent(
        WorkflowExecution.class, workflowExecution, ChangeType.DELETE);
    assertThat(workflowExecutionDeleteChangeEvent).isNotNull();
  }

  @Test
  @Category(UnitTests.class)
  public void testWorkflowExecutionInsertChange() {
    when(searchDao.upsertDocument(eq(DeploymentSearchEntity.TYPE), eq(workflowExecutionId), any())).thenReturn(true);
    boolean isSuccessful = deploymentChangeHandler.handleChange(workflowExecutionInsertChangeEvent);
    assertThat(isSuccessful).isTrue();
  }

  @Test
  @Category(UnitTests.class)
  public void testWorkflowExecutionUpdateChange() {
    when(searchDao.upsertDocument(eq(DeploymentSearchEntity.TYPE), eq(workflowExecutionId), any())).thenReturn(true);
    boolean isSuccessful = deploymentChangeHandler.handleChange(workflowExecutionUpdateChangeEvent);
    assertThat(isSuccessful).isTrue();
  }

  @Test
  @Category(UnitTests.class)
  public void testWorkflowExecutionDeleteChange() {
    when(searchDao.deleteDocument(eq(DeploymentSearchEntity.TYPE), anyString())).thenReturn(true);
    boolean isSuccessful = deploymentChangeHandler.handleChange(workflowExecutionDeleteChangeEvent);
    assertThat(isSuccessful).isTrue();
  }

  @Test
  @Category(UnitTests.class)
  public void testWorkflowUpdateChange() {
    ChangeEvent workflowUpdateChangeEvent =
        WorkflowEntityTestUtils.createWorkflowChangeEvent(workflow, ChangeType.UPDATE);
    when(searchDao.updateListInMultipleDocuments(
             eq(DeploymentSearchEntity.TYPE), eq(DeploymentViewKeys.workflows), anyString(), anyString(), anyString()))
        .thenReturn(true);

    boolean isUpdateSuccessful = deploymentChangeHandler.handleChange(workflowUpdateChangeEvent);
    assertThat(isUpdateSuccessful).isTrue();
  }

  @Test
  @Category(UnitTests.class)
  public void testServiceUpdateChange() {
    ChangeEvent serviceUpdateChangeEvent = ServiceEntityTestUtils.createServiceChangeEvent(service, ChangeType.UPDATE);
    when(searchDao.updateListInMultipleDocuments(
             eq(DeploymentSearchEntity.TYPE), eq(DeploymentViewKeys.services), anyString(), anyString(), anyString()))
        .thenReturn(true);
    boolean isUpdateSuccessful = deploymentChangeHandler.handleChange(serviceUpdateChangeEvent);
    assertThat(isUpdateSuccessful).isTrue();
  }

  @Test
  @Category(UnitTests.class)
  public void testEnvironmentUpdateChange() {
    ChangeEvent environmentUpdateChangeEvent =
        EnvironmentEntityTestUtils.createEnvironmentChangeEvent(environment, ChangeType.UPDATE);
    when(searchDao.updateListInMultipleDocuments(eq(DeploymentSearchEntity.TYPE), eq(DeploymentViewKeys.environments),
             anyString(), anyString(), anyString()))
        .thenReturn(true);
    boolean isUpdateSuccessful = deploymentChangeHandler.handleChange(environmentUpdateChangeEvent);
    assertThat(isUpdateSuccessful).isTrue();
  }

  @Test
  @Category(UnitTests.class)
  public void testApplicationUpdateChange() {
    ChangeEvent applicationUpdateChangeEvent =
        ApplicationEntityTestUtils.createApplicationChangeEvent(application, ChangeType.UPDATE);
    when(searchDao.updateKeyInMultipleDocuments(
             eq(DeploymentSearchEntity.TYPE), eq(DeploymentViewKeys.appName), anyString(), anyString(), anyString()))
        .thenReturn(true);
    boolean isUpdateSuccessful = deploymentChangeHandler.handleChange(applicationUpdateChangeEvent);
    assertThat(isUpdateSuccessful).isTrue();
  }
}
