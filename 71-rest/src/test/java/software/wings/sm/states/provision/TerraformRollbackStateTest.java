package software.wings.sm.states.provision;

import static io.harness.rule.OwnerRule.ARCHIT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.PORTAL_URL;
import static software.wings.utils.WingsTestConstants.WORKFLOW_EXECUTION_ID;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.app.MainConfiguration;
import software.wings.beans.infrastructure.TerraformConfig;
import software.wings.sm.ExecutionContextImpl;

public class TerraformRollbackStateTest extends WingsBaseTest {
  @Mock TerraformConfig configParameter;
  @Mock(answer = Answers.RETURNS_DEEP_STUBS) ExecutionContextImpl executionContext;
  @Mock(answer = Answers.RETURNS_DEEP_STUBS) MainConfiguration configuration;
  @InjectMocks TerraformRollbackState terraformRollbackState = new TerraformRollbackState("Rollback Terraform Test");

  /**
   * Tests whether expected last successful workflow execution is returned.
   */
  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void shouldReturnValidLastSuccessfulWorkflowExecutionUrl() {
    when(configuration.getPortal().getUrl()).thenReturn(PORTAL_URL);
    when(configParameter.getAccountId()).thenReturn(ACCOUNT_ID);
    when(configParameter.getAppId()).thenReturn(APP_ID);
    when(executionContext.getEnv().getUuid()).thenReturn(ENV_ID);
    when(configParameter.getWorkflowExecutionId()).thenReturn(WORKFLOW_EXECUTION_ID);

    final String expectedUrl = PORTAL_URL + "/#/account/" + ACCOUNT_ID + "/app/" + APP_ID + "/env/" + ENV_ID
        + "/executions/" + WORKFLOW_EXECUTION_ID + "/details";
    assertThat(terraformRollbackState.getLastSuccessfulWorkflowExecutionUrl(configParameter, executionContext)
                   .toString()
                   .equals(expectedUrl))
        .isTrue();
    // Check Url when env is null.
    when(executionContext.getEnv()).thenReturn(null);

    final String nullEnvExpectedUrl = PORTAL_URL + "/#/account/" + ACCOUNT_ID + "/app/" + APP_ID
        + "/env/null/executions/" + WORKFLOW_EXECUTION_ID + "/details";
    assertThat(terraformRollbackState.getLastSuccessfulWorkflowExecutionUrl(configParameter, executionContext)
                   .toString()
                   .equals(nullEnvExpectedUrl))
        .isTrue();
  }
}