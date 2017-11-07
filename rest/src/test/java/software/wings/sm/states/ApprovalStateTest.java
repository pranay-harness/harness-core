package software.wings.sm.states;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.Application.Builder.anApplication;
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
import software.wings.beans.alert.AlertType;
import software.wings.beans.User;
import software.wings.beans.alert.ApprovalAlert;
import software.wings.dl.PageResponse;
import software.wings.service.intfc.AlertService;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.WorkflowStandardParams;

import java.util.Arrays;

/**
 * Created by anubhaw on 11/3/16.
 */
public class ApprovalStateTest extends WingsBaseTest {
  private static final WorkflowStandardParams WORKFLOW_STANDARD_PARAMS =
      aWorkflowStandardParams().withArtifactIds(Arrays.asList(ARTIFACT_ID)).build();

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
    pageResponse.setResponse(Arrays.asList(User.Builder.anUser().build()));

    ExecutionResponse executionResponse = approvalState.execute(context);
    verify(alertService).openAlert(eq(ACCOUNT_ID), eq(APP_ID), eq(AlertType.ApprovalNeeded), any(ApprovalAlert.class));
    assertThat(executionResponse.isAsync()).isTrue();
    assertThat(executionResponse.getExecutionStatus()).isEqualTo(ExecutionStatus.PAUSED);
  }
}
