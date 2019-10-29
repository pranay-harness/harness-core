package software.wings.search.entities;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
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
import software.wings.audit.EntityAuditRecord;
import software.wings.beans.AccountType;
import software.wings.beans.Application;
import software.wings.beans.EntityType;
import software.wings.beans.Environment;
import software.wings.beans.Event.Type;
import software.wings.beans.Pipeline;
import software.wings.beans.Service;
import software.wings.beans.Workflow;
import software.wings.search.entities.application.ApplicationChangeHandler;
import software.wings.search.entities.application.ApplicationSearchEntity;
import software.wings.search.entities.application.ApplicationView;
import software.wings.search.entities.application.ApplicationView.ApplicationViewKeys;
import software.wings.search.entities.application.ApplicationViewBuilder;
import software.wings.search.entities.related.audit.RelatedAuditViewBuilder;
import software.wings.search.entities.service.ServiceViewBuilder;
import software.wings.search.framework.EntityInfo.EntityInfoKeys;
import software.wings.search.framework.SearchDao;
import software.wings.search.framework.SearchEntityUtils;
import software.wings.search.framework.changestreams.ChangeEvent;
import software.wings.search.framework.changestreams.ChangeType;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

public class ApplicationChangeHandlerTest extends WingsBaseTest {
  @Inject private RelatedAuditViewBuilder relatedAuditViewBuilder;
  @Inject private ApplicationViewBuilder applicationViewBuilder;
  @Inject private ServiceViewBuilder serviceViewBuilder;
  @Mock private SearchDao searchDao;

  @Inject @InjectMocks private ApplicationChangeHandler applicationChangeHandler;

  private static final String APP_NAME = "ApplicationHandlerTestForApplication" + System.currentTimeMillis();
  private static final String SERVICE_NAME = "ApplicationHandlerTestForService" + System.currentTimeMillis();
  private static final String ENVIRONMENT_NAME = "ApplicationHandlerTestForEnvironment" + System.currentTimeMillis();
  private static final String WORKFLOW_NAME = "ApplicationHandlerTestForWorkflow" + System.currentTimeMillis();
  private static final String PIPELINE_NAME = "ApplicationHandlerTestForPipeline" + System.currentTimeMillis();

  private Application application;
  private Service service;
  private Environment environment;
  private Workflow workflow;
  private Pipeline pipeline;
  private AuditHeader deleteAuditHeader;
  private AuditHeader nonDeleteAuditHeader;
  private ChangeEvent deleteAuditHeaderChangeEvent;
  private ChangeEvent nonDeleteAuditHeaderChangeEvent;
  private EntityAuditRecord nonDeleteEntityAuditRecord;
  private String documentId = generateUuid();
  private String serviceId = generateUuid();
  private String environmentId = generateUuid();
  private String workflowId = generateUuid();
  private String pipelineId = generateUuid();

  @Before
  public void setup() throws IOException {
    application = ApplicationEntityTestUtils.createApplication(getAccount(AccountType.PAID), documentId, APP_NAME);
    assertThat(application).isNotNull();

    service = ServiceEntityTestUtils.createService(getAccount(AccountType.PAID), documentId, serviceId, SERVICE_NAME);
    assertThat(service).isNotNull();

    environment = EnvironmentEntityTestUtils.createEnvironment(
        getAccount(AccountType.PAID), documentId, environmentId, ENVIRONMENT_NAME);
    assertThat(environment).isNotNull();

    workflow =
        WorkflowEntityTestUtils.createWorkflow(getAccount(AccountType.PAID), documentId, workflowId, WORKFLOW_NAME);
    assertThat(workflow).isNotNull();

    pipeline =
        PipelineEntityTestUtils.createPipeline(getAccount(AccountType.PAID), documentId, pipelineId, PIPELINE_NAME);
    assertThat(pipeline).isNotNull();

    deleteAuditHeader =
        SearchEntityTestUtils.createAuditHeader(EntityType.APPLICATION.name(), documentId, ChangeType.DELETE.name());
    assertThat(deleteAuditHeader).isNotNull();
    assertThat(deleteAuditHeader.getEntityAuditRecords()).isNotNull();

    nonDeleteAuditHeader =
        SearchEntityTestUtils.createAuditHeader(EntityType.APPLICATION.name(), documentId, Type.CREATE.name());
    assertThat(nonDeleteAuditHeader).isNotNull();
    assertThat(nonDeleteAuditHeader.getEntityAuditRecords()).isNotNull();

    deleteAuditHeaderChangeEvent = SearchEntityTestUtils.createAuditHeaderChangeEvent(AuditHeader.class,
        deleteAuditHeader, ChangeType.UPDATE, EntityType.APPLICATION.name(), Type.DELETE.name(), documentId);
    assertThat(deleteAuditHeaderChangeEvent).isNotNull();

    nonDeleteAuditHeaderChangeEvent = SearchEntityTestUtils.createAuditHeaderChangeEvent(AuditHeader.class,
        nonDeleteAuditHeader, ChangeType.UPDATE, EntityType.APPLICATION.name(), Type.CREATE.name(), documentId);
    assertThat(nonDeleteAuditHeaderChangeEvent).isNotNull();

    nonDeleteEntityAuditRecord = SearchEntityTestUtils.createEntityAuditRecord(
        EntityType.APPLICATION.name(), documentId, ChangeType.UPDATE.name());
    assertThat(nonDeleteEntityAuditRecord).isNotNull();
  }

  @Test
  @Category(UnitTests.class)
  public void testAuditRelatedChange() {
    boolean isSuccessful = applicationChangeHandler.handleChange(deleteAuditHeaderChangeEvent);
    assertThat(isSuccessful).isNotNull();
    assertThat(isSuccessful).isTrue();

    Map<String, Object> auditViewMap =
        relatedAuditViewBuilder.getAuditRelatedEntityViewMap(nonDeleteAuditHeader, nonDeleteEntityAuditRecord);

    when(searchDao.addTimestamp(ApplicationSearchEntity.TYPE, ApplicationViewKeys.auditTimestamps, documentId,
             nonDeleteAuditHeader.getCreatedAt(), 7))
        .thenReturn(true);
    when(searchDao.appendToListInSingleDocument(
             ApplicationSearchEntity.TYPE, ApplicationViewKeys.audits, documentId, auditViewMap, 3))
        .thenReturn(true);
    boolean result = applicationChangeHandler.handleChange(nonDeleteAuditHeaderChangeEvent);
    assertThat(result).isNotNull();
    assertThat(result).isTrue();
  }

  @Test
  @Category(UnitTests.class)
  public void testApplicationInsertChange() {
    ApplicationView applicationViewInsert = applicationViewBuilder.createApplicationView(application, false);
    Optional<String> applicationViewInsertJson = SearchEntityUtils.convertToJson(applicationViewInsert);

    ChangeEvent applicationInsertChangeEvent =
        ApplicationEntityTestUtils.createApplicationChangeEvent(application, ChangeType.INSERT);
    when(searchDao.upsertDocument(ApplicationSearchEntity.TYPE, documentId, applicationViewInsertJson.get()))
        .thenReturn(true);
    boolean isInsertSuccessful = applicationChangeHandler.handleChange(applicationInsertChangeEvent);
    assertThat(isInsertSuccessful).isNotNull();
    assertThat(isInsertSuccessful).isTrue();
  }

  @Test
  @Category(UnitTests.class)
  public void testApplicationDeleteChange() {
    ChangeEvent applicationDeleteChangeEvent =
        ApplicationEntityTestUtils.createApplicationChangeEvent(application, ChangeType.DELETE);
    when(searchDao.deleteDocument(ApplicationSearchEntity.TYPE, documentId)).thenReturn(true);
    boolean isDeleteSuccessful = applicationChangeHandler.handleChange(applicationDeleteChangeEvent);
    assertThat(isDeleteSuccessful).isNotNull();
    assertThat(isDeleteSuccessful).isTrue();
  }

  @Test
  @Category(UnitTests.class)
  public void testApplicationUpdateChange() {
    ApplicationView applicationViewUpdate = applicationViewBuilder.createApplicationView(application, true);
    Optional<String> applicationViewUpdateJson = SearchEntityUtils.convertToJson(applicationViewUpdate);
    ChangeEvent applicationUpdateChangeEvent =
        ApplicationEntityTestUtils.createApplicationChangeEvent(application, ChangeType.UPDATE);
    when(searchDao.upsertDocument(ApplicationSearchEntity.TYPE, documentId, applicationViewUpdateJson.get()))
        .thenReturn(true);
    boolean isUpdateSuccessful = applicationChangeHandler.handleChange(applicationUpdateChangeEvent);
    assertThat(isUpdateSuccessful).isNotNull();
    assertThat(isUpdateSuccessful).isTrue();
  }

  @Test
  @Category(UnitTests.class)
  public void testServiceInsertChange() {
    ChangeEvent serviceInsertChangeEvent = ServiceEntityTestUtils.createServiceChangeEvent(service, ChangeType.INSERT);
    when(searchDao.appendToListInSingleDocument(
             eq(ApplicationSearchEntity.TYPE), eq(ApplicationViewKeys.services), eq(service.getAppId()), anyMap()))
        .thenReturn(true);
    boolean isInsertSuccessful = applicationChangeHandler.handleChange(serviceInsertChangeEvent);
    assertThat(isInsertSuccessful).isNotNull();
    assertThat(isInsertSuccessful).isTrue();
  }

  @Test
  @Category(UnitTests.class)
  public void testServiceDeleteChange() {
    ChangeEvent serviceDeleteChangeEvent = ServiceEntityTestUtils.createServiceChangeEvent(service, ChangeType.DELETE);
    when(searchDao.removeFromListInMultipleDocuments(
             eq(ApplicationSearchEntity.TYPE), eq(ApplicationViewKeys.services), any()))
        .thenReturn(true);
    boolean isDeleteSuccessful = applicationChangeHandler.handleChange(serviceDeleteChangeEvent);
    assertThat(isDeleteSuccessful).isNotNull();
    assertThat(isDeleteSuccessful).isTrue();
  }

  @Test
  @Category(UnitTests.class)
  public void testServiceUpdateChange() {
    ChangeEvent serviceUpdateChangeEvent = ServiceEntityTestUtils.createServiceChangeEvent(service, ChangeType.UPDATE);
    when(searchDao.updateListInMultipleDocuments(eq(ApplicationSearchEntity.TYPE), eq(ApplicationViewKeys.services),
             anyString(), anyString(), eq(EntityInfoKeys.name)))
        .thenReturn(true);
    boolean isUpdateSuccessful = applicationChangeHandler.handleChange(serviceUpdateChangeEvent);
    assertThat(isUpdateSuccessful).isNotNull();
    assertThat(isUpdateSuccessful).isTrue();
  }

  @Test
  @Category(UnitTests.class)
  public void testEnvironmentInsertChange() {
    ChangeEvent environmentInsertChangeEvent =
        EnvironmentEntityTestUtils.createEnvironmentChangeEvent(environment, ChangeType.INSERT);
    when(searchDao.appendToListInSingleDocument(eq(ApplicationSearchEntity.TYPE), eq(ApplicationViewKeys.environments),
             eq(environment.getAppId()), anyMap()))
        .thenReturn(true);
    boolean isInsertSuccessful = applicationChangeHandler.handleChange(environmentInsertChangeEvent);
    assertThat(isInsertSuccessful).isNotNull();
    assertThat(isInsertSuccessful).isTrue();
  }

  @Test
  @Category(UnitTests.class)
  public void testEnvironmentDeleteChange() {
    ChangeEvent environmentDeleteChangeEvent =
        EnvironmentEntityTestUtils.createEnvironmentChangeEvent(environment, ChangeType.DELETE);
    when(searchDao.removeFromListInMultipleDocuments(
             eq(ApplicationSearchEntity.TYPE), eq(ApplicationViewKeys.environments), any()))
        .thenReturn(true);
    boolean isDeleteSuccessful = applicationChangeHandler.handleChange(environmentDeleteChangeEvent);
    assertThat(isDeleteSuccessful).isNotNull();
    assertThat(isDeleteSuccessful).isTrue();
  }

  @Test
  @Category(UnitTests.class)
  public void testEnvironmentUpdateChange() {
    ChangeEvent environmentUpdateChangeEvent =
        EnvironmentEntityTestUtils.createEnvironmentChangeEvent(environment, ChangeType.UPDATE);
    when(searchDao.updateListInMultipleDocuments(eq(ApplicationSearchEntity.TYPE), eq(ApplicationViewKeys.environments),
             anyString(), anyString(), eq(EntityInfoKeys.name)))
        .thenReturn(true);
    boolean isUpdateSuccessful = applicationChangeHandler.handleChange(environmentUpdateChangeEvent);
    assertThat(isUpdateSuccessful).isNotNull();
    assertThat(isUpdateSuccessful).isTrue();
  }

  @Test
  @Category(UnitTests.class)
  public void testWorkflowInsertChange() {
    ChangeEvent workflowInsertChangeEvent =
        WorkflowEntityTestUtils.createWorkflowChangeEvent(workflow, ChangeType.INSERT);
    when(searchDao.appendToListInSingleDocument(
             eq(ApplicationSearchEntity.TYPE), eq(ApplicationViewKeys.workflows), eq(workflow.getAppId()), anyMap()))
        .thenReturn(true);
    boolean isInsertSuccessful = applicationChangeHandler.handleChange(workflowInsertChangeEvent);
    assertThat(isInsertSuccessful).isNotNull();
    assertThat(isInsertSuccessful).isTrue();
  }

  @Test
  @Category(UnitTests.class)
  public void testWorkflowDeleteChange() {
    ChangeEvent workflowDeleteChangeEvent =
        WorkflowEntityTestUtils.createWorkflowChangeEvent(workflow, ChangeType.DELETE);
    when(searchDao.removeFromListInMultipleDocuments(
             eq(ApplicationSearchEntity.TYPE), eq(ApplicationViewKeys.workflows), any()))
        .thenReturn(true);
    boolean isDeleteSuccessful = applicationChangeHandler.handleChange(workflowDeleteChangeEvent);
    assertThat(isDeleteSuccessful).isNotNull();
    assertThat(isDeleteSuccessful).isTrue();
  }

  @Test
  @Category(UnitTests.class)
  public void testWorkflowUpdateChange() {
    ChangeEvent workflowUpdateChangeEvent =
        WorkflowEntityTestUtils.createWorkflowChangeEvent(workflow, ChangeType.UPDATE);
    when(searchDao.updateListInMultipleDocuments(eq(ApplicationSearchEntity.TYPE), eq(ApplicationViewKeys.workflows),
             anyString(), anyString(), eq(EntityInfoKeys.name)))
        .thenReturn(true);
    boolean isUpdateSuccessful = applicationChangeHandler.handleChange(workflowUpdateChangeEvent);
    assertThat(isUpdateSuccessful).isNotNull();
    assertThat(isUpdateSuccessful).isTrue();
  }

  @Test
  @Category(UnitTests.class)
  public void testPipelineInsertChange() {
    ChangeEvent pipelineInsertChangeEvent =
        PipelineEntityTestUtils.createPipelineChangeEvent(pipeline, ChangeType.INSERT);
    when(searchDao.appendToListInSingleDocument(
             eq(ApplicationSearchEntity.TYPE), eq(ApplicationViewKeys.pipelines), eq(pipeline.getAppId()), anyMap()))
        .thenReturn(true);
    boolean isInsertSuccessful = applicationChangeHandler.handleChange(pipelineInsertChangeEvent);
    assertThat(isInsertSuccessful).isNotNull();
    assertThat(isInsertSuccessful).isTrue();
  }

  @Test
  @Category(UnitTests.class)
  public void testPipelineDeleteChange() {
    ChangeEvent pipelineDeleteChangeEvent =
        PipelineEntityTestUtils.createPipelineChangeEvent(pipeline, ChangeType.DELETE);
    when(searchDao.removeFromListInMultipleDocuments(
             eq(ApplicationSearchEntity.TYPE), eq(ApplicationViewKeys.pipelines), any()))
        .thenReturn(true);
    boolean isDeleteSuccessful = applicationChangeHandler.handleChange(pipelineDeleteChangeEvent);
    assertThat(isDeleteSuccessful).isNotNull();
    assertThat(isDeleteSuccessful).isTrue();
  }

  @Test
  @Category(UnitTests.class)
  public void testPipelineUpdateChange() {
    ChangeEvent pipelineUpdateChangeEvent =
        PipelineEntityTestUtils.createPipelineChangeEvent(pipeline, ChangeType.UPDATE);
    when(searchDao.updateListInMultipleDocuments(eq(ApplicationSearchEntity.TYPE), eq(ApplicationViewKeys.pipelines),
             anyString(), anyString(), eq(EntityInfoKeys.name)))
        .thenReturn(true);
    boolean isUpdateSuccessful = applicationChangeHandler.handleChange(pipelineUpdateChangeEvent);
    assertThat(isUpdateSuccessful).isNotNull();
    assertThat(isUpdateSuccessful).isTrue();
  }
}
