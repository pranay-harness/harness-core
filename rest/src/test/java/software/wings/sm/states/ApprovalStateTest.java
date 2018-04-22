package software.wings.sm.states;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.api.ApprovalStateExecutionData.Builder.anApprovalStateExecutionData;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.common.Constants.DEFAULT_APPROVAL_STATE_TIMEOUT_MILLIS;
import static software.wings.sm.WorkflowStandardParams.Builder.aWorkflowStandardParams;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_ID;
import static software.wings.utils.WingsTestConstants.PIPELINE_WORKFLOW_EXECUTION_ID;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.User;
import software.wings.beans.alert.AlertType;
import software.wings.beans.alert.ApprovalNeededAlert;
import software.wings.dl.PageResponse;
import software.wings.service.intfc.AlertService;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.WorkflowStandardParams;

import java.util.concurrent.TimeUnit;

/**
 * Created by anubhaw on 11/3/16.
 */
public class ApprovalStateTest extends WingsBaseTest {
  private static final WorkflowStandardParams WORKFLOW_STANDARD_PARAMS =
      aWorkflowStandardParams().withArtifactIds(asList(ARTIFACT_ID)).build();

  @Mock private ExecutionContextImpl context;
  @Mock private AlertService alertService;

  @InjectMocks private ApprovalState approvalState = new ApprovalState("ApprovalState");

  @Before
  public void setUp() throws Exception {
    when(context.getApp()).thenReturn(anApplication().withAccountId(ACCOUNT_ID).withUuid(APP_ID).build());
    when(context.getContextElement(ContextElementType.STANDARD)).thenReturn(WORKFLOW_STANDARD_PARAMS);
    when(context.getWorkflowExecutionId()).thenReturn(PIPELINE_WORKFLOW_EXECUTION_ID);
  }

  @Test
  public void shouldExecute() {
    PageResponse pageResponse = new PageResponse();
    pageResponse.setResponse(asList(User.Builder.anUser().build()));

    ExecutionResponse executionResponse = approvalState.execute(context);
    verify(alertService)
        .openAlert(eq(ACCOUNT_ID), eq(APP_ID), eq(AlertType.ApprovalNeeded), any(ApprovalNeededAlert.class));
    assertThat(executionResponse.isAsync()).isTrue();
    assertThat(executionResponse.getExecutionStatus()).isEqualTo(ExecutionStatus.PAUSED);
  }

  @Test
  public void shouldGetTimeout() {
    Integer timeoutMillis = approvalState.getTimeoutMillis();
    assertThat(timeoutMillis).isEqualTo(DEFAULT_APPROVAL_STATE_TIMEOUT_MILLIS);
  }

  @Test
  public void shouldGetSetTimeout() {
    approvalState.setTimeoutMillis((int) (0.6 * TimeUnit.HOURS.toMillis(1)));
    Integer timeoutMillis = approvalState.getTimeoutMillis();
    assertThat(timeoutMillis).isEqualTo((int) (0.6 * TimeUnit.HOURS.toMillis(1)));
  }

  @Test
  public void shouldHandleAbort() {
    when(context.getStateExecutionData())
        .thenReturn(anApprovalStateExecutionData().withApprovalId("APPROVAL_ID").build());
    approvalState.setTimeoutMillis((int) (0.6 * TimeUnit.HOURS.toMillis(1)));
    approvalState.handleAbortEvent(context);
    assertThat(context.getStateExecutionData()).isNotNull();
    assertThat(context.getStateExecutionData().getErrorMsg()).contains("Pipeline was not approved within 36m");
  }
}
