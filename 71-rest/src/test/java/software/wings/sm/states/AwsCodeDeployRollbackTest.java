package software.wings.sm.states;

import static io.harness.rule.OwnerRule.ANSHUL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static software.wings.api.AwsCodeDeployRequestElement.AWS_CODE_DEPLOY_REQUEST_PARAM;

import io.harness.beans.ExecutionStatus;
import io.harness.category.element.UnitTests;
import io.harness.context.ContextElementType;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;

public class AwsCodeDeployRollbackTest extends WingsBaseTest {
  @Mock private ExecutionContextImpl context;

  @InjectMocks AwsCodeDeployRollback awsCodeDeployRollback = new AwsCodeDeployRollback("awsCodeDeployRollback");

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testExecute() {
    when(context.getContextElement(ContextElementType.PARAM, AWS_CODE_DEPLOY_REQUEST_PARAM)).thenReturn(null);
    ExecutionResponse response = awsCodeDeployRollback.execute(context);
    assertThat(response.getExecutionStatus()).isEqualTo(ExecutionStatus.SKIPPED);
    assertThat(response.getStateExecutionData().getErrorMsg()).isEqualTo("No context found for rollback. Skipping.");
  }
}
