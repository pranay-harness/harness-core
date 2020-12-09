package software.wings.graphql.datafetcher.execution;

import static io.harness.rule.OwnerRule.PRABU;

import static junit.framework.TestCase.assertEquals;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.harness.beans.CreatedByType;
import io.harness.beans.EmbeddedUser;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.beans.WorkflowExecution;
import software.wings.dl.WingsPersistence;
import software.wings.graphql.datafetcher.AbstractDataFetcherTestBase;
import software.wings.graphql.schema.query.QLExecutionQueryParameters;
import software.wings.graphql.schema.type.QLExecution;
import software.wings.utils.JsonUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mongodb.morphia.query.Query;

public class ExecutionDataFetcherTest extends AbstractDataFetcherTestBase {
  public static final String EXECUTION_ID_1 = "EXECUTION_ID1";
  @Mock WingsPersistence wingsPersistence;
  @Mock Query<WorkflowExecution> query;
  @Inject @InjectMocks ExecutionDataFetcher executionDataFetcher;

  @Test
  @Owner(developers = PRABU)
  @Category(UnitTests.class)
  public void testExecutionDataFetcherAlongPipeline() {
    WorkflowExecution workflowExecution =
        JsonUtils.readResourceFile("./execution/build_workflow_execution.json", WorkflowExecution.class);
    assertThat(workflowExecution).isNotNull();
    when(wingsPersistence.createAuthorizedQuery(WorkflowExecution.class)).thenReturn(query);
    when(query.filter("_id", EXECUTION_ID_1)).thenReturn(query);
    when(query.get()).thenReturn(workflowExecution);
    QLExecution execution = executionDataFetcher.fetch(new QLExecutionQueryParameters(EXECUTION_ID_1), ACCOUNT1_ID);
    JsonNode actual = JsonUtils.toJsonNode(execution);
    assertThat(execution).isNotNull();
    JsonNode expected = JsonUtils.readResourceFile("./execution/build_workflow_qlExecution.json", JsonNode.class);
    assertEquals("QL execution should be equal", expected, actual);
  }

  @Test
  @Owner(developers = PRABU)
  @Category(UnitTests.class)
  public void testExecutionDataFetcherByTrigger() {
    WorkflowExecution workflowExecution =
        JsonUtils.readResourceFile("./execution/build_workflow_execution.json", WorkflowExecution.class);
    assertThat(workflowExecution).isNotNull();
    workflowExecution.setDeploymentTriggerId("TRIGGER_ID");
    workflowExecution.setPipelineExecutionId(null);
    when(wingsPersistence.createAuthorizedQuery(WorkflowExecution.class)).thenReturn(query);
    when(query.filter("_id", EXECUTION_ID_1)).thenReturn(query);
    when(query.get()).thenReturn(workflowExecution);
    QLExecution execution = executionDataFetcher.fetch(new QLExecutionQueryParameters(EXECUTION_ID_1), ACCOUNT1_ID);
    JsonNode actual = JsonUtils.toJsonNode(execution);
    assertThat(execution).isNotNull();
    JsonNode expected =
        JsonUtils.readResourceFile("./execution/build_workflow_qlExecution_by_trigger.json", JsonNode.class);
    assertEquals("QL execution should be equal", expected, actual);
  }

  @Test
  @Owner(developers = PRABU)
  @Category(UnitTests.class)
  public void testExecutionDataFetcherByApiKey() {
    WorkflowExecution workflowExecution =
        JsonUtils.readResourceFile("./execution/build_workflow_execution.json", WorkflowExecution.class);
    assertThat(workflowExecution).isNotNull();
    workflowExecution.setPipelineExecutionId(null);
    workflowExecution.setCreatedByType(CreatedByType.API_KEY);
    workflowExecution.setCreatedBy(EmbeddedUser.builder().name("API_KEY").uuid("KEY_ID").build());
    when(wingsPersistence.createAuthorizedQuery(WorkflowExecution.class)).thenReturn(query);
    when(query.filter("_id", EXECUTION_ID_1)).thenReturn(query);
    when(query.get()).thenReturn(workflowExecution);
    QLExecution execution = executionDataFetcher.fetch(new QLExecutionQueryParameters(EXECUTION_ID_1), ACCOUNT1_ID);
    JsonNode actual = JsonUtils.toJsonNode(execution);
    assertThat(execution).isNotNull();
    JsonNode expected =
        JsonUtils.readResourceFile("./execution/build_workflow_qlExecution_by_apikey.json", JsonNode.class);
    assertEquals("QL execution should be equal", expected, actual);
  }

  @Test
  @Owner(developers = PRABU)
  @Category(UnitTests.class)
  public void testExecutionDataFetcherByUser() {
    WorkflowExecution workflowExecution =
        JsonUtils.readResourceFile("./execution/build_workflow_execution.json", WorkflowExecution.class);
    assertThat(workflowExecution).isNotNull();
    workflowExecution.setPipelineExecutionId(null);
    workflowExecution.setCreatedByType(CreatedByType.USER);
    workflowExecution.setTriggeredBy(
        EmbeddedUser.builder().name("admin").uuid("USER_ID").email("admin@harness.io").build());
    when(wingsPersistence.createAuthorizedQuery(WorkflowExecution.class)).thenReturn(query);
    when(query.filter("_id", EXECUTION_ID_1)).thenReturn(query);
    when(query.get()).thenReturn(workflowExecution);
    QLExecution execution = executionDataFetcher.fetch(new QLExecutionQueryParameters(EXECUTION_ID_1), ACCOUNT1_ID);
    JsonNode actual = JsonUtils.toJsonNode(execution);
    assertThat(execution).isNotNull();
    JsonNode expected =
        JsonUtils.readResourceFile("./execution/build_workflow_qlExecution_by_user.json", JsonNode.class);
    assertEquals("QL execution should be equal", expected, actual);
  }
}