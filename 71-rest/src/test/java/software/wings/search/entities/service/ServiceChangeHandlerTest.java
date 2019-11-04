package software.wings.search.entities.service;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.UJJAWAL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.rule.OwnerRule.Owner;
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
import software.wings.search.entities.service.ServiceView.ServiceViewKeys;
import software.wings.search.entities.workflow.WorkflowEntityTestUtils;
import software.wings.search.framework.SearchDao;
import software.wings.search.framework.changestreams.ChangeEvent;
import software.wings.search.framework.changestreams.ChangeType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ServiceChangeHandlerTest extends WingsBaseTest {
  @Mock private SearchDao searchDao;
  @Inject private RelatedAuditViewBuilder relatedAuditViewBuilder;
  @Inject @InjectMocks private ServiceChangeHandler serviceChangeHandler;

  private static final String APP_NAME = "ServiceHandlerTestForApplication" + System.currentTimeMillis();
  private static final String SERVICE_NAME = "ServiceHandlerTestForService" + System.currentTimeMillis();
  private static final String ENVIRONMENT_NAME = "ServiceHandlerTestForEnvironment" + System.currentTimeMillis();
  private static final String WORKFLOW_NAME = "ServiceHandlerTestForWorkflow" + System.currentTimeMillis();
  private static final String PIPELINE_NAME = "ServiceHandlerTestForPipeline" + System.currentTimeMillis();

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
    assertThat(environment).isNotNull();

    workflow = WorkflowEntityTestUtils.createWorkflow(accountId, appId, workflowId, environmentId, WORKFLOW_NAME);
    assertThat(environment).isNotNull();

    pipeline = PipelineEntityTestUtils.createPipeline(accountId, appId, pipelineId, PIPELINE_NAME);
    assertThat(pipeline).isNotNull();

    deleteAuditHeader = RelatedAuditEntityTestUtils.createAuditHeader(
        deleteAuditHeaderId, accountId, appId, serviceId, EntityType.SERVICE.name(), Type.DELETE.name());
    wingsPersistence.save(deleteAuditHeader);
    assertThat(deleteAuditHeader).isNotNull();

    nonDeleteAuditHeader = RelatedAuditEntityTestUtils.createAuditHeader(
        nonDeleteAuditHeaderId, accountId, appId, serviceId, EntityType.SERVICE.name(), Type.UPDATE.name());
    wingsPersistence.save(nonDeleteAuditHeader);
    assertThat(nonDeleteAuditHeader).isNotNull();

    deleteAuditHeaderChangeEvent = RelatedAuditEntityTestUtils.createAuditHeaderChangeEvent(
        deleteAuditHeader, appId, ChangeType.UPDATE, serviceId, Type.DELETE.name());
    assertThat(deleteAuditHeaderChangeEvent).isNotNull();

    nonDeleteAuditHeaderChangeEvent = RelatedAuditEntityTestUtils.createAuditHeaderChangeEvent(
        nonDeleteAuditHeader, appId, ChangeType.UPDATE, serviceId, Type.UPDATE.name());
    assertThat(nonDeleteAuditHeaderChangeEvent).isNotNull();
  }

  @Test
  @Owner(emails = UJJAWAL)
  @Category(UnitTests.class)
  public void testAuditRelatedChange() {
    boolean isSuccessful = serviceChangeHandler.handleChange(deleteAuditHeaderChangeEvent);
    assertThat(isSuccessful).isNotNull();
    assertThat(isSuccessful).isTrue();

    when(searchDao.addTimestamp(eq(ServiceSearchEntity.TYPE), eq(ServiceViewKeys.auditTimestamps), anyString(),
             eq(nonDeleteAuditHeader.getCreatedAt()), eq(7)))
        .thenReturn(true);
    when(searchDao.appendToListInSingleDocument(
             eq(ServiceSearchEntity.TYPE), eq(ServiceViewKeys.audits), anyString(), anyMap(), eq(3)))
        .thenReturn(true);
    boolean result = serviceChangeHandler.handleChange(nonDeleteAuditHeaderChangeEvent);
    assertThat(result).isTrue();
  }

  @Test
  @Category(UnitTests.class)
  public void testServiceInsertChange() {
    Service service = ServiceEntityTestUtils.createService(accountId, appId, serviceId, SERVICE_NAME);
    assertThat(service).isNotNull();
    ChangeEvent serviceInsertChangeEvent = ServiceEntityTestUtils.createServiceChangeEvent(service, ChangeType.INSERT);
    when(searchDao.upsertDocument(eq(ServiceSearchEntity.TYPE), eq(serviceId), any())).thenReturn(true);
    boolean isInsertSuccessful = serviceChangeHandler.handleChange(serviceInsertChangeEvent);
    assertThat(isInsertSuccessful).isTrue();
  }

  @Test
  @Category(UnitTests.class)
  public void testServiceDeleteChange() {
    Service service = ServiceEntityTestUtils.createService(accountId, appId, serviceId, SERVICE_NAME);
    assertThat(service).isNotNull();
    ChangeEvent serviceDeleteChangeEvent = ServiceEntityTestUtils.createServiceChangeEvent(service, ChangeType.DELETE);
    when(searchDao.deleteDocument(ServiceSearchEntity.TYPE, serviceId)).thenReturn(true);
    boolean isDeleteSuccessful = serviceChangeHandler.handleChange(serviceDeleteChangeEvent);
    assertThat(isDeleteSuccessful).isTrue();
  }

  @Test
  @Category(UnitTests.class)
  public void testServiceUpdateChange() {
    Service service = ServiceEntityTestUtils.createService(accountId, appId, serviceId, SERVICE_NAME);
    assertThat(service).isNotNull();
    ChangeEvent serviceUpdateChangeEvent = ServiceEntityTestUtils.createServiceChangeEvent(service, ChangeType.UPDATE);
    when(searchDao.upsertDocument(eq(ServiceSearchEntity.TYPE), eq(serviceId), any())).thenReturn(true);
    boolean isUpdateSuccessful = serviceChangeHandler.handleChange(serviceUpdateChangeEvent);
    assertThat(isUpdateSuccessful).isTrue();
  }

  @Test
  @Category(UnitTests.class)
  public void testPipelineUpdateChange() {
    ChangeEvent serviceUpdateChangeEvent =
        PipelineEntityTestUtils.createPipelineChangeEvent(pipeline, ChangeType.UPDATE);
    when(searchDao.updateListInMultipleDocuments(
             eq(ServiceSearchEntity.TYPE), eq(ServiceViewKeys.pipelines), anyString(), anyString(), anyString()))
        .thenReturn(true);
    boolean isUpdateSuccessful = serviceChangeHandler.handleChange(serviceUpdateChangeEvent);
    assertThat(isUpdateSuccessful).isTrue();
  }

  @Test
  @Category(UnitTests.class)
  public void testPipelineDeleteChange() {
    ChangeEvent serviceDeleteChangeEvent =
        PipelineEntityTestUtils.createPipelineChangeEvent(pipeline, ChangeType.DELETE);

    List<String> stringList = new ArrayList<>();
    stringList.add("value1");

    when(searchDao.nestedQuery(eq(ServiceSearchEntity.TYPE), eq(ServiceViewKeys.pipelines), anyString()))
        .thenReturn(stringList);
    when(searchDao.removeFromListInMultipleDocuments(
             eq(ServiceSearchEntity.TYPE), eq(ServiceViewKeys.pipelines), anyList(), anyString()))
        .thenReturn(true);

    boolean isUpdateSuccessful = serviceChangeHandler.handleChange(serviceDeleteChangeEvent);
    assertThat(isUpdateSuccessful).isTrue();
  }

  @Test
  @Category(UnitTests.class)
  public void testApplicationUpdateChange() {
    ChangeEvent applicationUpdateChangeEvent =
        ApplicationEntityTestUtils.createApplicationChangeEvent(application, ChangeType.UPDATE);

    when(searchDao.updateKeyInMultipleDocuments(
             eq(ServiceSearchEntity.TYPE), anyString(), anyString(), anyString(), anyString()))
        .thenReturn(true);

    boolean isUpdateSuccessful = serviceChangeHandler.handleChange(applicationUpdateChangeEvent);
    assertThat(isUpdateSuccessful).isTrue();
  }

  @Test
  @Category(UnitTests.class)
  public void testWorkflowUpdateChange() {
    ChangeEvent workflowUpdateChangeEvent =
        WorkflowEntityTestUtils.createWorkflowChangeEvent(workflow, ChangeType.UPDATE);

    List<String> stringList = new ArrayList<>();
    stringList.add("value1");

    when(searchDao.nestedQuery(eq(ServiceSearchEntity.TYPE), eq(ServiceViewKeys.workflows), anyString()))
        .thenReturn(stringList);
    when(searchDao.appendToListInMultipleDocuments(
             eq(ServiceSearchEntity.TYPE), eq(ServiceViewKeys.workflows), anyList(), anyMap()))
        .thenReturn(true);
    when(searchDao.removeFromListInMultipleDocuments(
             eq(ServiceSearchEntity.TYPE), eq(ServiceViewKeys.workflows), anyList(), anyString()))
        .thenReturn(true);
    when(searchDao.updateListInMultipleDocuments(
             eq(ServiceSearchEntity.TYPE), eq(ServiceViewKeys.workflows), anyString(), anyString(), anyString()))
        .thenReturn(true);

    boolean isUpdateSuccessful = serviceChangeHandler.handleChange(workflowUpdateChangeEvent);
    assertThat(isUpdateSuccessful).isTrue();
  }

  @Test
  @Category(UnitTests.class)
  public void testWorkflowInsertChange() {
    ChangeEvent workflowInsertChangeEvent =
        WorkflowEntityTestUtils.createWorkflowChangeEvent(workflow, ChangeType.INSERT);

    when(searchDao.appendToListInMultipleDocuments(
             eq(ServiceSearchEntity.TYPE), eq(ServiceViewKeys.workflows), anyList(), anyMap()))
        .thenReturn(true);

    boolean isUpdateSuccessful = serviceChangeHandler.handleChange(workflowInsertChangeEvent);
    assertThat(isUpdateSuccessful).isTrue();
  }

  @Test
  @Category(UnitTests.class)
  public void testWorkflowDeleteChange() {
    ChangeEvent workflowDeleteChangeEvent =
        WorkflowEntityTestUtils.createWorkflowChangeEvent(workflow, ChangeType.DELETE);

    List<String> stringList = new ArrayList<>();
    stringList.add("value1");

    when(searchDao.nestedQuery(eq(ServiceSearchEntity.TYPE), eq(ServiceViewKeys.workflows), anyString()))
        .thenReturn(stringList);
    when(searchDao.removeFromListInMultipleDocuments(
             eq(ServiceSearchEntity.TYPE), eq(ServiceViewKeys.workflows), anyList(), anyString()))
        .thenReturn(true);

    boolean isUpdateSuccessful = serviceChangeHandler.handleChange(workflowDeleteChangeEvent);
    assertThat(isUpdateSuccessful).isTrue();
  }
}
