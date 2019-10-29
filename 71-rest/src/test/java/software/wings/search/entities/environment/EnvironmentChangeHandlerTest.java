package software.wings.search.entities.environment;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
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
import software.wings.search.entities.SearchEntityTestUtils;
import software.wings.search.entities.application.ApplicationEntityTestUtils;
import software.wings.search.entities.pipeline.PipelineEntityTestUtils;
import software.wings.search.entities.related.audit.RelatedAuditViewBuilder;
import software.wings.search.entities.service.ServiceEntityTestUtils;
import software.wings.search.entities.workflow.WorkflowEntityTestUtils;
import software.wings.search.framework.SearchDao;
import software.wings.search.framework.changestreams.ChangeEvent;
import software.wings.search.framework.changestreams.ChangeType;

import java.io.IOException;

public class EnvironmentChangeHandlerTest extends WingsBaseTest {
  @Mock private SearchDao searchDao;
  @Inject private RelatedAuditViewBuilder relatedAuditViewBuilder;
  @Inject @InjectMocks private EnvironmentChangeHandler environmentChangeHandler;

  private static final String APP_NAME = "EnvironmentHandlerTestForApplication" + System.currentTimeMillis();
  private static final String SERVICE_NAME = "EnvironmentHandlerTestForService" + System.currentTimeMillis();
  private static final String ENVIRONMENT_NAME = "EnvironmentHandlerTestForEnvironment" + System.currentTimeMillis();
  private static final String WORKFLOW_NAME = "EnvironmentHandlerTestForWorkflow" + System.currentTimeMillis();
  private static final String PIPELINE_NAME = "EnvironmentHandlerTestForPipeline" + System.currentTimeMillis();

  private AuditHeader deleteAuditHeader;
  private AuditHeader nonDeleteAuditHeader;
  private ChangeEvent nonDeleteChangeEvent;
  private ChangeEvent deleteChangeEvent;
  private EntityAuditRecord nonDeleteEntityAuditRecord;
  private Application application;
  private Service service;
  private Environment environment;
  private Pipeline pipeline;
  private Workflow workflow;
  private String documentId = generateUuid();
  private String appId = generateUuid();
  private String serviceId = generateUuid();
  private String environmentId = generateUuid();
  private String workflowId = generateUuid();
  private String pipelineId = generateUuid();
  @Before
  public void setup() throws IOException {
    application = ApplicationEntityTestUtils.createApplication(getAccount(AccountType.PAID), appId, APP_NAME);
    wingsPersistence.save(application);
    assertThat(application).isNotNull();

    service = ServiceEntityTestUtils.createService(getAccount(AccountType.PAID), appId, serviceId, SERVICE_NAME);
    assertThat(service).isNotNull();

    environment = EnvironmentEntityTestUtils.createEnvironment(
        getAccount(AccountType.PAID), appId, environmentId, ENVIRONMENT_NAME);
    assertThat(environment).isNotNull();

    pipeline = PipelineEntityTestUtils.createPipeline(getAccount(AccountType.PAID), appId, pipelineId, PIPELINE_NAME);
    assertThat(pipeline).isNotNull();

    workflow = WorkflowEntityTestUtils.createWorkflow(getAccount(AccountType.PAID), appId, workflowId, WORKFLOW_NAME);
    assertThat(environment).isNotNull();

    nonDeleteEntityAuditRecord = SearchEntityTestUtils.createEntityAuditRecord(
        EntityType.ENVIRONMENT.name(), documentId, ChangeType.INSERT.name());
    assertThat(nonDeleteEntityAuditRecord).isNotNull();

    deleteAuditHeader =
        SearchEntityTestUtils.createAuditHeader(EntityType.ENVIRONMENT.name(), documentId, ChangeType.DELETE.name());
    assertThat(deleteAuditHeader).isNotNull();
    assertThat(deleteAuditHeader.getEntityAuditRecords()).isNotNull();

    nonDeleteAuditHeader =
        SearchEntityTestUtils.createAuditHeader(EntityType.ENVIRONMENT.name(), documentId, ChangeType.INSERT.name());
    assertThat(nonDeleteAuditHeader).isNotNull();
    assertThat(nonDeleteAuditHeader.getEntityAuditRecords()).isNotNull();

    deleteChangeEvent = SearchEntityTestUtils.createAuditHeaderChangeEvent(AuditHeader.class, deleteAuditHeader,
        ChangeType.UPDATE, EntityType.ENVIRONMENT.name(), Type.DELETE.name(), documentId);
    assertThat(deleteChangeEvent).isNotNull();

    nonDeleteChangeEvent = SearchEntityTestUtils.createAuditHeaderChangeEvent(AuditHeader.class, nonDeleteAuditHeader,
        ChangeType.UPDATE, EntityType.ENVIRONMENT.name(), Type.CREATE.name(), documentId);
    assertThat(nonDeleteChangeEvent).isNotNull();
  }

  /*@Test
  @Owner(emails = UJJAWAL)
  @Category(UnitTests.class)
  public void testAuditRelatedChange() {
    boolean isSuccessful = environmentChangeHandler.handleChange(deleteChangeEvent);
    assertThat(isSuccessful).isNotNull();
    assertThat(isSuccessful).isTrue();

    Map<String, Object> auditViewMap =
        relatedAuditViewBuilder.getAuditRelatedEntityViewMap(nonDeleteAuditHeader, nonDeleteEntityAuditRecord);

    when(searchDao.addTimestamp(EnvironmentSearchEntity.TYPE, EnvironmentViewKeys.auditTimestamps, documentId,
             nonDeleteAuditHeader.getCreatedAt(), 7))
        .thenReturn(true);
    when(searchDao.appendToListInSingleDocument(
             EnvironmentSearchEntity.TYPE, EnvironmentViewKeys.audits, documentId, auditViewMap, 3))
        .thenReturn(true);
    boolean result = environmentChangeHandler.handleChange(nonDeleteChangeEvent);
    assertThat(result).isNotNull();
    assertThat(result).isTrue();
  }
*/
  @Test
  @Category(UnitTests.class)
  public void testEnvironmentInsertChange() {
    Environment environment = EnvironmentEntityTestUtils.createEnvironment(
        getAccount(AccountType.PAID), appId, environmentId, ENVIRONMENT_NAME);
    assertThat(environment).isNotNull();
    ChangeEvent environmentInsertChangeEvent =
        EnvironmentEntityTestUtils.createEnvironmentChangeEvent(environment, ChangeType.INSERT);
    when(searchDao.upsertDocument(eq(EnvironmentSearchEntity.TYPE), eq(environmentId), any())).thenReturn(true);
    boolean isInsertSuccessful = environmentChangeHandler.handleChange(environmentInsertChangeEvent);
    assertThat(isInsertSuccessful).isTrue();
  }

  @Test
  @Category(UnitTests.class)
  public void testEnvironmentDeleteChange() {
    Environment environment = EnvironmentEntityTestUtils.createEnvironment(
        getAccount(AccountType.PAID), appId, environmentId, ENVIRONMENT_NAME);
    assertThat(environment).isNotNull();
    ChangeEvent environmentDeleteChangeEvent =
        EnvironmentEntityTestUtils.createEnvironmentChangeEvent(environment, ChangeType.DELETE);
    when(searchDao.deleteDocument(EnvironmentSearchEntity.TYPE, environmentId)).thenReturn(true);
    boolean isDeleteSuccessful = environmentChangeHandler.handleChange(environmentDeleteChangeEvent);
    assertThat(isDeleteSuccessful).isTrue();
  }

  @Test
  @Category(UnitTests.class)
  public void testEnvironmentUpdateChange() {
    Environment environment = EnvironmentEntityTestUtils.createEnvironment(
        getAccount(AccountType.PAID), appId, environmentId, ENVIRONMENT_NAME);
    assertThat(environment).isNotNull();
    ChangeEvent environmentUpdateChangeEvent =
        EnvironmentEntityTestUtils.createEnvironmentChangeEvent(environment, ChangeType.UPDATE);
    when(searchDao.upsertDocument(eq(EnvironmentSearchEntity.TYPE), eq(environmentId), any())).thenReturn(true);
    boolean isUpdateSuccessful = environmentChangeHandler.handleChange(environmentUpdateChangeEvent);
    assertThat(isUpdateSuccessful).isTrue();
  }
}
