package software.wings.sm.states;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.GARVIT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.utils.WingsTestConstants.APP_ID;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.resume.ResumeStateUtils;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class ApprovalResumeStateTest extends WingsBaseTest {
  @Mock private ResumeStateUtils resumeStateUtils;

  @InjectMocks private ApprovalResumeState approvalResumeState = new ApprovalResumeState("APPROVAL_RESUME_STATE");

  private String prevStateExecutionId = generateUuid();
  private String prevPipelineExecutionId = generateUuid();
  private String currStateExecutionId = generateUuid();
  private String currPipelineExecutionId = generateUuid();

  @Before
  public void setUp() {
    approvalResumeState.setPrevPipelineExecutionId(prevPipelineExecutionId);
    approvalResumeState.setPrevStateExecutionId(prevStateExecutionId);
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void shouldExecute() {
    ExecutionContext context = mock(ExecutionContextImpl.class);
    when(context.getAppId()).thenReturn(APP_ID);
    when(context.getStateExecutionInstanceId()).thenReturn(currStateExecutionId);
    when(resumeStateUtils.fetchPipelineExecutionId(context)).thenReturn(currPipelineExecutionId);

    approvalResumeState.execute(context);
    verify(resumeStateUtils)
        .copyPipelineStageOutputs(eq(APP_ID), eq(prevPipelineExecutionId), eq(prevStateExecutionId),
            (List<String>) isNull(), eq(currPipelineExecutionId), eq(currStateExecutionId));
    verify(resumeStateUtils).prepareExecutionResponse(eq(context), eq(prevStateExecutionId));
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void shouldGetTimeout() {
    approvalResumeState.setTimeoutMillis(null);
    Integer timeoutMillis = approvalResumeState.getTimeoutMillis();
    assertThat(timeoutMillis).isEqualTo(ResumeStateUtils.RESUME_STATE_TIMEOUT_MILLIS);
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void shouldGetSetTimeout() {
    approvalResumeState.setTimeoutMillis((int) TimeUnit.HOURS.toMillis(1));
    Integer timeoutMillis = approvalResumeState.getTimeoutMillis();
    assertThat(timeoutMillis).isEqualTo((int) TimeUnit.HOURS.toMillis(1));
  }
}
