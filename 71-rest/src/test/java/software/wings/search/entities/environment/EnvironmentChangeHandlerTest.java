package software.wings.search.entities.environment;

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
import software.wings.beans.AccountType;
import software.wings.beans.Application;
import software.wings.beans.EntityType;
import software.wings.beans.Environment;
import software.wings.beans.Event.Type;
import software.wings.beans.Pipeline;
import software.wings.beans.Service;
import software.wings.beans.Workflow;
import software.wings.search.entities.application.ApplicationEntityTestUtils;
import software.wings.search.entities.environment.EnvironmentView.EnvironmentViewKeys;
import software.wings.search.entities.pipeline.PipelineEntityTestUtils;
import software.wings.search.entities.related.RelatedAuditEntityTestUtils;
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

    pipeline = PipelineEntityTestUtils.createPipeline(accountId, appId, pipelineId, PIPELINE_NAME);
    assertThat(pipeline).isNotNull();

    workflow = WorkflowEntityTestUtils.createWorkflow(accountId, appId, workflowId, WORKFLOW_NAME);
    assertThat(workflow).isNotNull();

    deleteAuditHeader = RelatedAuditEntityTestUtils.createAuditHeader(
        deleteAuditHeaderId, accountId, appId, environmentId, EntityType.ENVIRONMENT.name(), Type.DELETE.name());
    wingsPersistence.save(deleteAuditHeader);
    assertThat(deleteAuditHeader).isNotNull();

    nonDeleteAuditHeader = RelatedAuditEntityTestUtils.createAuditHeader(
        nonDeleteAuditHeaderId, accountId, appId, environmentId, EntityType.ENVIRONMENT.name(), Type.UPDATE.name());
    wingsPersistence.save(nonDeleteAuditHeader);
    assertThat(nonDeleteAuditHeader).isNotNull();

    deleteAuditHeaderChangeEvent = RelatedAuditEntityTestUtils.createAuditHeaderChangeEvent(
        deleteAuditHeader, appId, ChangeType.UPDATE, environmentId, Type.DELETE.name());
    assertThat(deleteAuditHeaderChangeEvent).isNotNull();

    nonDeleteAuditHeaderChangeEvent = RelatedAuditEntityTestUtils.createAuditHeaderChangeEvent(
        nonDeleteAuditHeader, appId, ChangeType.UPDATE, environmentId, Type.UPDATE.name());
    assertThat(nonDeleteAuditHeaderChangeEvent).isNotNull();
  }

  @Test
  @Category(UnitTests.class)
  public void testAuditRelatedChange() {
    boolean isSuccessful = environmentChangeHandler.handleChange(deleteAuditHeaderChangeEvent);
    assertThat(isSuccessful).isNotNull();
    assertThat(isSuccessful).isTrue();

    when(searchDao.addTimestamp(eq(EnvironmentSearchEntity.TYPE), eq(EnvironmentViewKeys.auditTimestamps), anyString(),
             eq(nonDeleteAuditHeader.getCreatedAt()), eq(7)))
        .thenReturn(true);
    when(searchDao.appendToListInSingleDocument(
             eq(EnvironmentSearchEntity.TYPE), eq(EnvironmentViewKeys.audits), anyString(), anyMap(), eq(3)))
        .thenReturn(true);
    boolean result = environmentChangeHandler.handleChange(nonDeleteAuditHeaderChangeEvent);
    assertThat(result).isNotNull();
    assertThat(result).isTrue();
  }

  @Test
  @Category(UnitTests.class)
  public void testEnvironmentInsertChange() {
    Environment environment =
        EnvironmentEntityTestUtils.createEnvironment(accountId, appId, environmentId, ENVIRONMENT_NAME);
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
    Environment environment =
        EnvironmentEntityTestUtils.createEnvironment(accountId, appId, environmentId, ENVIRONMENT_NAME);
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
    Environment environment =
        EnvironmentEntityTestUtils.createEnvironment(accountId, appId, environmentId, ENVIRONMENT_NAME);
    assertThat(environment).isNotNull();
    ChangeEvent environmentUpdateChangeEvent =
        EnvironmentEntityTestUtils.createEnvironmentChangeEvent(environment, ChangeType.UPDATE);
    when(searchDao.upsertDocument(eq(EnvironmentSearchEntity.TYPE), eq(environmentId), any())).thenReturn(true);
    boolean isUpdateSuccessful = environmentChangeHandler.handleChange(environmentUpdateChangeEvent);
    assertThat(isUpdateSuccessful).isTrue();
  }
}
