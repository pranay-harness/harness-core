package software.wings.graphql.datafetcher.pipeline;

import static io.harness.rule.OwnerRule.RUSHABH;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;

import software.wings.beans.Pipeline;
import software.wings.beans.User;
import software.wings.graphql.datafetcher.AbstractDataFetcherTestBase;
import software.wings.graphql.schema.query.QLPipelineQueryParameters;
import software.wings.graphql.schema.type.QLPipeline;
import software.wings.security.UserThreadLocal;

import com.google.inject.Inject;
import java.sql.SQLException;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class PipelineDataFetcherTest extends AbstractDataFetcherTestBase {
  @Inject PipelineDataFetcher pipelineDataFetcher;
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
  public void testPipelineDataFetcher() {
    Pipeline pipeline = createPipeline(ACCOUNT1_ID, APP1_ID_ACCOUNT1, PIPELINE1);
    QLPipeline qlPipeline = pipelineDataFetcher.fetch(
        QLPipelineQueryParameters.builder().pipelineId(pipeline.getUuid()).build(), ACCOUNT1_ID);

    assertThat(qlPipeline.getId()).isEqualTo(pipeline.getUuid());
    assertThat(qlPipeline.getName()).isEqualTo(PIPELINE1);

    try {
      qlPipeline = pipelineDataFetcher.fetch(
          QLPipelineQueryParameters.builder().pipelineName(pipeline.getName()).build(), ACCOUNT1_ID);
      fail("Empty Application Id");
    } catch (Exception e) {
      assertThat(e).isInstanceOf(InvalidRequestException.class);
    }

    qlPipeline = pipelineDataFetcher.fetch(
        QLPipelineQueryParameters.builder().pipelineName(pipeline.getName()).applicationId(pipeline.getAppId()).build(),
        ACCOUNT1_ID);

    assertThat(qlPipeline.getAppId()).isEqualTo(APP1_ID_ACCOUNT1);
    assertThat(qlPipeline.getName()).isEqualTo(PIPELINE1);

    String workflowExecutionId = createWorkflowExecution(ACCOUNT1_ID, APP1_ID_ACCOUNT1, qlPipeline.getId());
    qlPipeline = pipelineDataFetcher.fetch(
        QLPipelineQueryParameters.builder().executionId(workflowExecutionId).build(), ACCOUNT1_ID);

    assertThat(qlPipeline.getId()).isEqualTo(pipeline.getUuid());
    assertThat(qlPipeline.getName()).isEqualTo(PIPELINE1);

    qlPipeline =
        pipelineDataFetcher.fetch(QLPipelineQueryParameters.builder().pipelineId("fakeId").build(), ACCOUNT1_ID);

    assertThat(qlPipeline).isNull();

    try {
      qlPipeline = pipelineDataFetcher.fetch(
          QLPipelineQueryParameters.builder().pipelineId(pipeline.getUuid()).build(), ACCOUNT2_ID);
      fail("InvalidRequestException expected here");
    } catch (Exception e) {
      assertThat(e).isInstanceOf(InvalidRequestException.class);
    }
  }
}
