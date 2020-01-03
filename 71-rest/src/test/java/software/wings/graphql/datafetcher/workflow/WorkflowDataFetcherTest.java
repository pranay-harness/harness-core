package software.wings.graphql.datafetcher.workflow;

import static io.harness.rule.OwnerRule.RUSHABH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.User;
import software.wings.beans.Workflow;
import software.wings.graphql.datafetcher.AbstractDataFetcherTest;
import software.wings.graphql.schema.query.QLWorkflowQueryParameters;
import software.wings.graphql.schema.type.QLWorkflow;
import software.wings.security.UserThreadLocal;

import java.sql.SQLException;

public class WorkflowDataFetcherTest extends AbstractDataFetcherTest {
  @Inject WorkflowDataFetcher workflowDataFetcher;
  @Before
  public void setup() throws SQLException {
    User user = testUtils.createUser(testUtils.createAccount());
    UserThreadLocal.set(user);

    // Account1
    createAccount(ACCOUNT1_ID, getLicenseInfo());
    createApp(ACCOUNT1_ID, APP1_ID_ACCOUNT1, APP1_ID_ACCOUNT1, TAG_TEAM, TAG_VALUE_TEAM1);
  }

  @Test
  @Owner(developers = RUSHABH)
  @Category(UnitTests.class)
  public void testWorkflowDataFetcher() {
    Workflow workflow = createWorkflow(ACCOUNT1_ID, APP1_ID_ACCOUNT1, WORKFLOW1);
    QLWorkflow qlWorkflow = workflowDataFetcher.fetch(
        QLWorkflowQueryParameters.builder().workflowId(workflow.getUuid()).build(), ACCOUNT1_ID);

    assertThat(qlWorkflow.getId()).isEqualTo(workflow.getUuid());
    assertThat(qlWorkflow.getName()).isEqualTo(WORKFLOW1);

    String workflowExecutionId = createWorkflowExecution(ACCOUNT1_ID, APP1_ID_ACCOUNT1, qlWorkflow.getId());
    qlWorkflow = workflowDataFetcher.fetch(
        QLWorkflowQueryParameters.builder().executionId(workflowExecutionId).build(), ACCOUNT1_ID);

    assertThat(qlWorkflow.getId()).isEqualTo(workflow.getUuid());
    assertThat(qlWorkflow.getName()).isEqualTo(WORKFLOW1);

    qlWorkflow =
        workflowDataFetcher.fetch(QLWorkflowQueryParameters.builder().workflowId("fakeId").build(), ACCOUNT1_ID);

    assertThat(qlWorkflow).isNull();

    try {
      qlWorkflow = workflowDataFetcher.fetch(
          QLWorkflowQueryParameters.builder().workflowId(workflow.getUuid()).build(), ACCOUNT2_ID);
      fail("InvalidRequestException expected here");
    } catch (Exception e) {
      assertThat(e).isInstanceOf(InvalidRequestException.class);
    }
  }
}
