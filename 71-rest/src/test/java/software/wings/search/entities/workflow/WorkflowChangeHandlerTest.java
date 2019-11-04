package software.wings.search.entities.workflow;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.audit.AuditHeader;
import software.wings.beans.AccountType;
import software.wings.beans.Application;
import software.wings.beans.EntityType;
import software.wings.beans.Environment;
import software.wings.beans.Event.Type;
import software.wings.beans.Pipeline;
import software.wings.beans.Service;
import software.wings.beans.Workflow;
import software.wings.search.entities.application.ApplicationEntityTestUtils;
import software.wings.search.entities.environment.EnvironmentEntityTestUtils;
import software.wings.search.entities.pipeline.PipelineEntityTestUtils;
import software.wings.search.entities.related.RelatedAuditEntityTestUtils;
import software.wings.search.entities.related.audit.RelatedAuditViewBuilder;
import software.wings.search.entities.service.ServiceEntityTestUtils;
import software.wings.search.entities.workflow.WorkflowView.WorkflowViewKeys;
import software.wings.search.framework.SearchDao;
import software.wings.search.framework.changestreams.ChangeEvent;
import software.wings.search.framework.changestreams.ChangeType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class WorkflowChangeHandlerTest extends WingsBaseTest {
  @Mock private SearchDao searchDao;
  @Inject private RelatedAuditViewBuilder relatedAuditViewBuilder;
  @Inject @InjectMocks private WorkflowChangeHandler workflowChangeHandler;

  private static final String APP_NAME = "WorkflowHandlerTestForApplication" + System.currentTimeMillis();
  private static final String SERVICE_NAME = "WorkflowHandlerTestForService" + System.currentTimeMillis();
  private static final String ENVIRONMENT_NAME = "WorkflowHandlerTestForEnvironment" + System.currentTimeMillis();
  private static final String WORKFLOW_NAME = "WorkflowHandlerTestForWorkflow" + System.currentTimeMillis();
  private static final String PIPELINE_NAME = "WorkflowHandlerTestForPipeline" + System.currentTimeMillis();

  private Application application;
  private Service service;
  private Environment environment;
  private Workflow workflow;
  private Pipeline pipeline;
  private AuditHeader deleteAuditHeader;
  private AuditHeader nonDeleteAuditHeader;
  private ChangeEvent deleteAuditHeaderChangeEvent;
  private ChangeEvent nonDeleteAuditHeaderChangeEvent;
  private String accountId = getAccount(AccountType.PAID).getUuid();
  private String nonDeleteAuditHeaderId = generateUuid();
  private String deleteAuditHeaderId = generateUuid();
  private String appId = generateUuid();
  private String serviceId = generateUuid();
  private String environmentId = generateUuid();
  private String workflowId = generateUuid();
  private String pipelineId = generateUuid();

  @Before
  public void setup() throws IOException {
    application = ApplicationEntityTestUtils.createApplication(accountId, appId, APP_NAME);
    wingsPersistence.save(application);
    assertThat(application).isNotNull();

    service = ServiceEntityTestUtils.createService(accountId, appId, serviceId, SERVICE_NAME);
    assertThat(service).isNotNull();

    environment = EnvironmentEntityTestUtils.createEnvironment(accountId, appId, environmentId, ENVIRONMENT_NAME);
    wingsPersistence.save(environment);
    assertThat(environment).isNotNull();

    pipeline = PipelineEntityTestUtils.createPipeline(accountId, appId, pipelineId, PIPELINE_NAME);
    assertThat(pipeline).isNotNull();

    deleteAuditHeader = RelatedAuditEntityTestUtils.createAuditHeader(
        deleteAuditHeaderId, accountId, appId, workflowId, EntityType.WORKFLOW.name(), Type.DELETE.name());
    wingsPersistence.save(deleteAuditHeader);
    assertThat(deleteAuditHeader).isNotNull();

    nonDeleteAuditHeader = RelatedAuditEntityTestUtils.createAuditHeader(
        nonDeleteAuditHeaderId, accountId, appId, workflowId, EntityType.WORKFLOW.name(), Type.UPDATE.name());
    wingsPersistence.save(nonDeleteAuditHeader);
    assertThat(nonDeleteAuditHeader).isNotNull();

    deleteAuditHeaderChangeEvent = RelatedAuditEntityTestUtils.createAuditHeaderChangeEvent(
        deleteAuditHeader, appId, ChangeType.UPDATE, workflowId, Type.DELETE.name());
    assertThat(deleteAuditHeaderChangeEvent).isNotNull();

    nonDeleteAuditHeaderChangeEvent = RelatedAuditEntityTestUtils.createAuditHeaderChangeEvent(
        nonDeleteAuditHeader, appId, ChangeType.UPDATE, pipelineId, Type.UPDATE.name());
    assertThat(nonDeleteAuditHeaderChangeEvent).isNotNull();
  }

  @Test
  @Category(UnitTests.class)
  public void testAuditRelatedChange() {
    boolean isSuccessful = workflowChangeHandler.handleChange(deleteAuditHeaderChangeEvent);
    assertThat(isSuccessful).isNotNull();
    assertThat(isSuccessful).isTrue();

    when(searchDao.addTimestamp(eq(WorkflowSearchEntity.TYPE), eq(WorkflowViewKeys.auditTimestamps), anyString(),
             eq(nonDeleteAuditHeader.getCreatedAt()), eq(7)))
        .thenReturn(true);
    when(searchDao.appendToListInSingleDocument(
             eq(WorkflowSearchEntity.TYPE), eq(WorkflowViewKeys.audits), anyString(), anyMap(), eq(3)))
        .thenReturn(true);
    boolean result = workflowChangeHandler.handleChange(nonDeleteAuditHeaderChangeEvent);
    assertThat(result).isTrue();
  }

  @Test
  @Category(UnitTests.class)
  public void testWorkflowInsertChange() {
    Workflow workflow =
        WorkflowEntityTestUtils.createWorkflow(accountId, appId, workflowId, environmentId, WORKFLOW_NAME);
    assertThat(workflow).isNotNull();
    ChangeEvent workflowInsertChangeEvent =
        WorkflowEntityTestUtils.createWorkflowChangeEvent(workflow, ChangeType.INSERT);
    when(searchDao.upsertDocument(eq(WorkflowSearchEntity.TYPE), eq(workflowId), any())).thenReturn(true);
    boolean isInsertSuccessful = workflowChangeHandler.handleChange(workflowInsertChangeEvent);
    assertThat(isInsertSuccessful).isTrue();
  }

  @Test
  @Category(UnitTests.class)
  public void testWorkflowDeleteChange() {
    Workflow workflow =
        WorkflowEntityTestUtils.createWorkflow(accountId, appId, workflowId, environmentId, WORKFLOW_NAME);
    assertThat(workflow).isNotNull();
    ChangeEvent workflowDeleteChangeEvent =
        WorkflowEntityTestUtils.createWorkflowChangeEvent(workflow, ChangeType.DELETE);
    when(searchDao.deleteDocument(WorkflowSearchEntity.TYPE, workflowId)).thenReturn(true);
    boolean isDeleteSuccessful = workflowChangeHandler.handleChange(workflowDeleteChangeEvent);
    assertThat(isDeleteSuccessful).isTrue();
  }

  @Test
  @Category(UnitTests.class)
  public void testWorkflowUpdateChange() {
    Workflow workflow =
        WorkflowEntityTestUtils.createWorkflow(accountId, appId, workflowId, environmentId, WORKFLOW_NAME);
    assertThat(workflow).isNotNull();
    ChangeEvent workflowUpdateChangeEvent =
        WorkflowEntityTestUtils.createWorkflowChangeEvent(workflow, ChangeType.UPDATE);
    when(searchDao.upsertDocument(eq(WorkflowSearchEntity.TYPE), eq(workflowId), any())).thenReturn(true);
    boolean isUpdateSuccessful = workflowChangeHandler.handleChange(workflowUpdateChangeEvent);
    assertThat(isUpdateSuccessful).isTrue();
  }

  @Test
  @Category(UnitTests.class)
  public void testServiceUpdateChange() {
    ChangeEvent serviceUpdateChangeEvent = ServiceEntityTestUtils.createServiceChangeEvent(service, ChangeType.UPDATE);
    when(searchDao.updateListInMultipleDocuments(
             eq(WorkflowSearchEntity.TYPE), anyString(), anyString(), anyString(), anyString()))
        .thenReturn(true);
    boolean isUpdateSuccessful = workflowChangeHandler.handleChange(serviceUpdateChangeEvent);
    assertThat(isUpdateSuccessful).isTrue();
  }

  @Test
  @Category(UnitTests.class)
  public void testEnvironmentUpdateChange() {
    ChangeEvent environmentUpdateChangeEvent =
        EnvironmentEntityTestUtils.createEnvironmentChangeEvent(environment, ChangeType.UPDATE);
    when(searchDao.updateKeyInMultipleDocuments(
             eq(WorkflowSearchEntity.TYPE), anyString(), anyString(), anyString(), anyString()))
        .thenReturn(true);

    boolean isUpdateSuccessful = workflowChangeHandler.handleChange(environmentUpdateChangeEvent);
    assertThat(isUpdateSuccessful).isTrue();
  }

  @Test
  @Category(UnitTests.class)
  public void testApplicationUpdateChange() {
    ChangeEvent applicationUpdateChangeEvent =
        ApplicationEntityTestUtils.createApplicationChangeEvent(application, ChangeType.UPDATE);

    when(searchDao.updateKeyInMultipleDocuments(
             eq(WorkflowSearchEntity.TYPE), anyString(), anyString(), anyString(), anyString()))
        .thenReturn(true);

    boolean isUpdateSuccessful = workflowChangeHandler.handleChange(applicationUpdateChangeEvent);
    assertThat(isUpdateSuccessful).isTrue();
  }

  @Test
  @Category(UnitTests.class)
  public void testPipelineNameUpdateChange() {
    ChangeEvent pipelineUpdateChangeEvent =
        PipelineEntityTestUtils.createPipelineChangeEvent(pipeline, ChangeType.UPDATE);
    when(searchDao.updateListInMultipleDocuments(
             eq(WorkflowSearchEntity.TYPE), anyString(), anyString(), anyString(), anyString()))
        .thenReturn(true);

    boolean isUpdateSuccessful = workflowChangeHandler.handleChange(pipelineUpdateChangeEvent);
    assertThat(isUpdateSuccessful).isTrue();
  }

  @Test
  @Category(UnitTests.class)
  public void testPipelineDeleteChange() {
    ChangeEvent pipelineDeleteChangeEvent =
        PipelineEntityTestUtils.createPipelineChangeEvent(pipeline, ChangeType.DELETE);
    List<String> listOfString = new ArrayList<>();
    listOfString.add("id1");
    listOfString.add("id2");

    when(searchDao.nestedQuery(eq(WorkflowSearchEntity.TYPE), anyString(), anyString())).thenReturn(listOfString);
    when(
        searchDao.removeFromListInMultipleDocuments(eq(WorkflowSearchEntity.TYPE), anyString(), anyList(), anyString()))
        .thenReturn(true);
    boolean isDeleteSuccessful = workflowChangeHandler.handleChange(pipelineDeleteChangeEvent);
    assertThat(isDeleteSuccessful).isNotNull();
    assertThat(isDeleteSuccessful).isTrue();
  }
}
