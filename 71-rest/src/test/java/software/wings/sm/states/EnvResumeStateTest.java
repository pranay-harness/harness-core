package software.wings.sm.states;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.GARVIT;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
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

public class EnvResumeStateTest extends WingsBaseTest {
  @Mock private ResumeStateUtils resumeStateUtils;

  @InjectMocks private EnvResumeState envResumeState = new EnvResumeState("ENV_RESUME_STATE");

  private String prevStateExecutionId = generateUuid();
  private String prevPipelineExecutionId = generateUuid();
  private List<String> prevWorkflowExecutionIds = asList(generateUuid(), generateUuid());
  private String currStateExecutionId = generateUuid();
  private String currPipelineExecutionId = generateUuid();

  @Before
  public void setUp() {
    envResumeState.setPrevPipelineExecutionId(prevPipelineExecutionId);
    envResumeState.setPrevStateExecutionId(prevStateExecutionId);
    envResumeState.setPrevWorkflowExecutionIds(prevWorkflowExecutionIds);
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void shouldExecute() {
    ExecutionContext context = mock(ExecutionContextImpl.class);
    when(context.getAppId()).thenReturn(APP_ID);
    when(context.getStateExecutionInstanceId()).thenReturn(currStateExecutionId);
    when(resumeStateUtils.fetchPipelineExecutionId(context)).thenReturn(currPipelineExecutionId);

    envResumeState.execute(context);
    verify(resumeStateUtils)
        .copyPipelineStageOutputs(eq(APP_ID), eq(prevPipelineExecutionId), eq(prevStateExecutionId),
            eq(prevWorkflowExecutionIds), eq(currPipelineExecutionId), eq(currStateExecutionId));
    verify(resumeStateUtils).prepareExecutionResponse(eq(context), eq(prevStateExecutionId));
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void shouldGetTimeout() {
    envResumeState.setTimeoutMillis(null);
    Integer timeoutMillis = envResumeState.getTimeoutMillis();
    assertThat(timeoutMillis).isEqualTo(ResumeStateUtils.RESUME_STATE_TIMEOUT_MILLIS);
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void shouldGetSetTimeout() {
    envResumeState.setTimeoutMillis((int) TimeUnit.HOURS.toMillis(1));
    Integer timeoutMillis = envResumeState.getTimeoutMillis();
    assertThat(timeoutMillis).isEqualTo((int) TimeUnit.HOURS.toMillis(1));
  }
}
