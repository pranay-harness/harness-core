package software.wings.sm;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;
import static software.wings.beans.WorkflowExecution.WorkflowExecutionBuilder.aWorkflowExecution;
import static software.wings.sm.ExecutionInterrupt.ExecutionInterruptBuilder.anExecutionInterrupt;

import com.google.inject.Inject;

import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.WingsBaseTest;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.Environment.Builder;
import software.wings.beans.WorkflowExecution;
import software.wings.dl.WingsPersistence;

/**
 * The type State machine execution event manager test.
 *
 * @author Rishi
 */
public class ExecutionInterruptManagerTest extends WingsBaseTest {
  private static final Logger logger = LoggerFactory.getLogger(ExecutionInterruptManagerTest.class);

  @Inject private WingsPersistence wingsPersistence;
  @InjectMocks @Inject private ExecutionInterruptManager executionInterruptManager;
  @Mock private StateMachineExecutor stateMachineExecutor;

  /**
   * Should throw invalid argument for null state execution instance.
   */
  @Test
  public void shouldThrowInvalidArgumentForNullStateExecutionInstance() {
    Application app =
        wingsPersistence.saveAndGet(Application.class, Application.Builder.anApplication().withName("App1").build());
    Environment env =
        wingsPersistence.saveAndGet(Environment.class, Builder.anEnvironment().withAppId(app.getUuid()).build());
    WorkflowExecution workflowExecution =
        wingsPersistence.saveAndGet(WorkflowExecution.class, aWorkflowExecution().withAppId(app.getAppId()).build());
    ExecutionInterrupt executionInterrupt = anExecutionInterrupt()
                                                .withAppId(app.getUuid())
                                                .withExecutionInterruptType(ExecutionInterruptType.PAUSE)
                                                .withEnvId(env.getUuid())
                                                .withExecutionUuid(workflowExecution.getUuid())
                                                .build();
    try {
      executionInterrupt = executionInterruptManager.registerExecutionInterrupt(executionInterrupt);
      failBecauseExceptionWasNotThrown(WingsException.class);
    } catch (WingsException exception) {
      assertThat(exception.getParams().values()).doesNotContainNull();
      assertThat(exception.getParams().values().iterator().next())
          .isInstanceOf(String.class)
          .asString()
          .startsWith("null stateExecutionInstanceId");
      assertThat(exception).hasMessage(ErrorCode.INVALID_ARGUMENT.name());
    }
  }

  /**
   * Should throw invalid argument for invalid state execution instance.
   */
  @Test
  public void shouldThrowInvalidArgumentForInvalidStateExecutionInstance() {
    Application app =
        wingsPersistence.saveAndGet(Application.class, Application.Builder.anApplication().withName("App1").build());
    Environment env =
        wingsPersistence.saveAndGet(Environment.class, Builder.anEnvironment().withAppId(app.getUuid()).build());
    ExecutionInterrupt executionInterrupt = anExecutionInterrupt()
                                                .withAppId(app.getUuid())
                                                .withExecutionInterruptType(ExecutionInterruptType.PAUSE)
                                                .withEnvId(env.getUuid())
                                                .withExecutionUuid(generateUuid())
                                                .withStateExecutionInstanceId(generateUuid())
                                                .build();
    try {
      executionInterrupt = executionInterruptManager.registerExecutionInterrupt(executionInterrupt);
      failBecauseExceptionWasNotThrown(WingsException.class);
    } catch (WingsException exception) {
      assertThat(exception.getParams().values()).doesNotContainNull();
      assertThat(exception.getParams().values().iterator().next())
          .isInstanceOf(String.class)
          .asString()
          .startsWith("invalid stateExecutionInstanceId");
      assertThat(exception).hasMessage(ErrorCode.INVALID_ARGUMENT.name());
    }
  }

  /**
   * Should throw state not for resume.
   */
  @Test
  public void shouldThrowStateNotForResume() {
    Application app =
        wingsPersistence.saveAndGet(Application.class, Application.Builder.anApplication().withName("App1").build());
    Environment env =
        wingsPersistence.saveAndGet(Environment.class, Builder.anEnvironment().withAppId(app.getUuid()).build());
    StateExecutionInstance stateExecutionInstance = StateExecutionInstance.Builder.aStateExecutionInstance()
                                                        .withAppId(app.getUuid())
                                                        .withDisplayName("state1")
                                                        .build();
    stateExecutionInstance = wingsPersistence.saveAndGet(StateExecutionInstance.class, stateExecutionInstance);
    ExecutionInterrupt executionInterrupt = anExecutionInterrupt()
                                                .withAppId(app.getUuid())
                                                .withExecutionInterruptType(ExecutionInterruptType.RESUME)
                                                .withEnvId(env.getUuid())
                                                .withExecutionUuid(generateUuid())
                                                .withStateExecutionInstanceId(stateExecutionInstance.getUuid())
                                                .build();
    try {
      executionInterrupt = executionInterruptManager.registerExecutionInterrupt(executionInterrupt);
      failBecauseExceptionWasNotThrown(WingsException.class);
    } catch (WingsException exception) {
      assertThat(exception).hasMessage(ErrorCode.STATE_NOT_FOR_TYPE.name());
    }
  }

  /**
   * Should throw state not for retry.
   */
  @Test
  public void shouldThrowStateNotForRetry() {
    Application app =
        wingsPersistence.saveAndGet(Application.class, Application.Builder.anApplication().withName("App1").build());
    Environment env =
        wingsPersistence.saveAndGet(Environment.class, Builder.anEnvironment().withAppId(app.getUuid()).build());
    StateExecutionInstance stateExecutionInstance = StateExecutionInstance.Builder.aStateExecutionInstance()
                                                        .withAppId(app.getUuid())
                                                        .withDisplayName("state1")
                                                        .build();
    stateExecutionInstance = wingsPersistence.saveAndGet(StateExecutionInstance.class, stateExecutionInstance);
    ExecutionInterrupt executionInterrupt = anExecutionInterrupt()
                                                .withAppId(app.getUuid())
                                                .withExecutionInterruptType(ExecutionInterruptType.RETRY)
                                                .withEnvId(env.getUuid())
                                                .withExecutionUuid(generateUuid())
                                                .withStateExecutionInstanceId(stateExecutionInstance.getUuid())
                                                .build();
    try {
      executionInterrupt = executionInterruptManager.registerExecutionInterrupt(executionInterrupt);
      failBecauseExceptionWasNotThrown(WingsException.class);
    } catch (WingsException exception) {
      assertThat(exception).hasMessage(ErrorCode.STATE_NOT_FOR_TYPE.name());
    }
  }

  /**
   * Should throw state not for pause.
   */
  @Test
  public void shouldThrowStateNotForPause() {
    Application app =
        wingsPersistence.saveAndGet(Application.class, Application.Builder.anApplication().withName("App1").build());
    Environment env =
        wingsPersistence.saveAndGet(Environment.class, Builder.anEnvironment().withAppId(app.getUuid()).build());
    StateExecutionInstance stateExecutionInstance = StateExecutionInstance.Builder.aStateExecutionInstance()
                                                        .withAppId(app.getUuid())
                                                        .withDisplayName("state1")
                                                        .withStatus(ExecutionStatus.SUCCESS)
                                                        .build();
    stateExecutionInstance = wingsPersistence.saveAndGet(StateExecutionInstance.class, stateExecutionInstance);
    ExecutionInterrupt executionInterrupt = anExecutionInterrupt()
                                                .withAppId(app.getUuid())
                                                .withExecutionInterruptType(ExecutionInterruptType.PAUSE)
                                                .withEnvId(env.getUuid())
                                                .withExecutionUuid(generateUuid())
                                                .withStateExecutionInstanceId(stateExecutionInstance.getUuid())
                                                .build();
    try {
      executionInterrupt = executionInterruptManager.registerExecutionInterrupt(executionInterrupt);
      failBecauseExceptionWasNotThrown(WingsException.class);
    } catch (WingsException exception) {
      assertThat(exception).hasMessage(ErrorCode.STATE_NOT_FOR_TYPE.name());
    }
  }

  /**
   * Should throw state not for abort.
   */
  @Test
  public void shouldThrowStateNotForAbort() {
    Application app =
        wingsPersistence.saveAndGet(Application.class, Application.Builder.anApplication().withName("App1").build());
    Environment env =
        wingsPersistence.saveAndGet(Environment.class, Builder.anEnvironment().withAppId(app.getUuid()).build());
    StateExecutionInstance stateExecutionInstance = StateExecutionInstance.Builder.aStateExecutionInstance()
                                                        .withAppId(app.getUuid())
                                                        .withDisplayName("state1")
                                                        .withStatus(ExecutionStatus.SUCCESS)
                                                        .build();
    stateExecutionInstance = wingsPersistence.saveAndGet(StateExecutionInstance.class, stateExecutionInstance);
    ExecutionInterrupt executionInterrupt = anExecutionInterrupt()
                                                .withAppId(app.getUuid())
                                                .withExecutionInterruptType(ExecutionInterruptType.ABORT)
                                                .withEnvId(env.getUuid())
                                                .withExecutionUuid(generateUuid())
                                                .withStateExecutionInstanceId(stateExecutionInstance.getUuid())
                                                .build();
    try {
      executionInterrupt = executionInterruptManager.registerExecutionInterrupt(executionInterrupt);
      failBecauseExceptionWasNotThrown(WingsException.class);
    } catch (WingsException exception) {
      assertThat(exception).hasMessage(ErrorCode.STATE_NOT_FOR_TYPE.name());
    }
  }

  /**
   * Should throw abort all already.
   */
  @Test
  @Ignore
  public void shouldThrowAbortAllAlready() {
    Application app =
        wingsPersistence.saveAndGet(Application.class, Application.Builder.anApplication().withName("App1").build());
    Environment env =
        wingsPersistence.saveAndGet(Environment.class, Builder.anEnvironment().withAppId(app.getUuid()).build());

    String executionUuid = generateUuid();
    ExecutionInterrupt executionInterrupt = anExecutionInterrupt()
                                                .withAppId(app.getUuid())
                                                .withExecutionInterruptType(ExecutionInterruptType.ABORT_ALL)
                                                .withEnvId(env.getUuid())
                                                .withExecutionUuid(executionUuid)
                                                .build();
    executionInterrupt = executionInterruptManager.registerExecutionInterrupt(executionInterrupt);

    executionInterrupt = anExecutionInterrupt()
                             .withAppId(app.getUuid())
                             .withExecutionInterruptType(ExecutionInterruptType.PAUSE_ALL)
                             .withEnvId(env.getUuid())
                             .withExecutionUuid(executionUuid)
                             .build();
    try {
      executionInterrupt = executionInterruptManager.registerExecutionInterrupt(executionInterrupt);
      failBecauseExceptionWasNotThrown(WingsException.class);
    } catch (WingsException exception) {
      assertThat(exception).hasMessage(ErrorCode.ABORT_ALL_ALREADY.name());
    }
  }

  /**
   * Should throw pause all already.
   */
  @Test
  public void shouldThrowPauseAllAlready() {
    Application app =
        wingsPersistence.saveAndGet(Application.class, Application.Builder.anApplication().withName("App1").build());
    Environment env =
        wingsPersistence.saveAndGet(Environment.class, Builder.anEnvironment().withAppId(app.getUuid()).build());
    WorkflowExecution workflowExecution =
        wingsPersistence.saveAndGet(WorkflowExecution.class, aWorkflowExecution().withAppId(app.getAppId()).build());

    ExecutionInterrupt executionInterrupt = anExecutionInterrupt()
                                                .withAppId(app.getUuid())
                                                .withExecutionInterruptType(ExecutionInterruptType.PAUSE_ALL)
                                                .withEnvId(env.getUuid())
                                                .withExecutionUuid(workflowExecution.getUuid())
                                                .build();
    executionInterrupt = executionInterruptManager.registerExecutionInterrupt(executionInterrupt);

    executionInterrupt = anExecutionInterrupt()
                             .withAppId(app.getUuid())
                             .withExecutionInterruptType(ExecutionInterruptType.PAUSE_ALL)
                             .withEnvId(env.getUuid())
                             .withExecutionUuid(workflowExecution.getUuid())
                             .build();
    try {
      executionInterrupt = executionInterruptManager.registerExecutionInterrupt(executionInterrupt);
      failBecauseExceptionWasNotThrown(WingsException.class);
    } catch (WingsException exception) {
      assertThat(exception).hasMessage(ErrorCode.PAUSE_ALL_ALREADY.name());
    }
  }

  /**
   * Should pause all clear previous resume all.
   */
  @Test
  public void shouldPauseAllClearPreviousResumeAll() {
    Application app =
        wingsPersistence.saveAndGet(Application.class, Application.Builder.anApplication().withName("App1").build());
    Environment env =
        wingsPersistence.saveAndGet(Environment.class, Builder.anEnvironment().withAppId(app.getUuid()).build());
    WorkflowExecution workflowExecution =
        wingsPersistence.saveAndGet(WorkflowExecution.class, aWorkflowExecution().withAppId(app.getAppId()).build());

    ExecutionInterrupt executionInterrupt = anExecutionInterrupt()
                                                .withAppId(app.getUuid())
                                                .withExecutionInterruptType(ExecutionInterruptType.PAUSE_ALL)
                                                .withEnvId(env.getUuid())
                                                .withExecutionUuid(workflowExecution.getUuid())
                                                .build();
    executionInterrupt = executionInterruptManager.registerExecutionInterrupt(executionInterrupt);

    ExecutionInterrupt resumeAll = anExecutionInterrupt()
                                       .withAppId(app.getUuid())
                                       .withExecutionInterruptType(ExecutionInterruptType.RESUME_ALL)
                                       .withEnvId(env.getUuid())
                                       .withExecutionUuid(workflowExecution.getUuid())
                                       .build();
    resumeAll = executionInterruptManager.registerExecutionInterrupt(resumeAll);

    executionInterrupt = anExecutionInterrupt()
                             .withAppId(app.getUuid())
                             .withExecutionInterruptType(ExecutionInterruptType.PAUSE_ALL)
                             .withEnvId(env.getUuid())
                             .withExecutionUuid(workflowExecution.getUuid())
                             .build();
    executionInterrupt = executionInterruptManager.registerExecutionInterrupt(executionInterrupt);
    final ExecutionInterrupt interrupt =
        wingsPersistence.get(ExecutionInterrupt.class, resumeAll.getAppId(), resumeAll.getUuid());
    assertThat(interrupt.isSeized()).isTrue();
  }

  /**
   * Should throw resume all already.
   */
  @Test
  public void shouldThrowResumeAllAlready() {
    Application app =
        wingsPersistence.saveAndGet(Application.class, Application.Builder.anApplication().withName("App1").build());
    Environment env =
        wingsPersistence.saveAndGet(Environment.class, Builder.anEnvironment().withAppId(app.getUuid()).build());

    String executionUuid = generateUuid();
    ExecutionInterrupt executionInterrupt = anExecutionInterrupt()
                                                .withAppId(app.getUuid())
                                                .withExecutionInterruptType(ExecutionInterruptType.RESUME_ALL)
                                                .withEnvId(env.getUuid())
                                                .withExecutionUuid(executionUuid)
                                                .build();
    try {
      executionInterrupt = executionInterruptManager.registerExecutionInterrupt(executionInterrupt);
      failBecauseExceptionWasNotThrown(WingsException.class);
    } catch (WingsException exception) {
      assertThat(exception).hasMessage(ErrorCode.RESUME_ALL_ALREADY.name());
    }
  }

  /**
   * Should resume all clear prev pause all.
   */
  @Test
  public void shouldResumeAllClearPrevPauseAll() {
    Application app =
        wingsPersistence.saveAndGet(Application.class, Application.Builder.anApplication().withName("App1").build());
    Environment env =
        wingsPersistence.saveAndGet(Environment.class, Builder.anEnvironment().withAppId(app.getUuid()).build());
    WorkflowExecution workflowExecution =
        wingsPersistence.saveAndGet(WorkflowExecution.class, aWorkflowExecution().withAppId(app.getAppId()).build());

    ExecutionInterrupt pauseAll = anExecutionInterrupt()
                                      .withAppId(app.getUuid())
                                      .withExecutionInterruptType(ExecutionInterruptType.PAUSE_ALL)
                                      .withEnvId(env.getUuid())
                                      .withExecutionUuid(workflowExecution.getUuid())
                                      .build();
    pauseAll = executionInterruptManager.registerExecutionInterrupt(pauseAll);

    ExecutionInterrupt executionInterrupt = anExecutionInterrupt()
                                                .withAppId(app.getUuid())
                                                .withExecutionInterruptType(ExecutionInterruptType.RESUME_ALL)
                                                .withEnvId(env.getUuid())
                                                .withExecutionUuid(workflowExecution.getUuid())
                                                .build();
    executionInterrupt = executionInterruptManager.registerExecutionInterrupt(executionInterrupt);

    final ExecutionInterrupt interrupt =
        wingsPersistence.get(ExecutionInterrupt.class, pauseAll.getAppId(), pauseAll.getUuid());
    assertThat(interrupt.isSeized()).isTrue();
  }

  /**
   * Should throw resume all already 2.
   */
  @Test
  public void shouldThrowResumeAllAlready2() {
    Application app =
        wingsPersistence.saveAndGet(Application.class, Application.Builder.anApplication().withName("App1").build());
    Environment env =
        wingsPersistence.saveAndGet(Environment.class, Builder.anEnvironment().withAppId(app.getUuid()).build());
    WorkflowExecution workflowExecution =
        wingsPersistence.saveAndGet(WorkflowExecution.class, aWorkflowExecution().withAppId(app.getAppId()).build());

    ExecutionInterrupt pauseAll = anExecutionInterrupt()
                                      .withAppId(app.getUuid())
                                      .withExecutionInterruptType(ExecutionInterruptType.PAUSE_ALL)
                                      .withEnvId(env.getUuid())
                                      .withExecutionUuid(workflowExecution.getUuid())
                                      .build();
    pauseAll = executionInterruptManager.registerExecutionInterrupt(pauseAll);

    ExecutionInterrupt executionInterrupt = anExecutionInterrupt()
                                                .withAppId(app.getUuid())
                                                .withExecutionInterruptType(ExecutionInterruptType.RESUME_ALL)
                                                .withEnvId(env.getUuid())
                                                .withExecutionUuid(workflowExecution.getUuid())
                                                .build();
    executionInterrupt = executionInterruptManager.registerExecutionInterrupt(executionInterrupt);

    executionInterrupt = anExecutionInterrupt()
                             .withAppId(app.getUuid())
                             .withExecutionInterruptType(ExecutionInterruptType.RESUME_ALL)
                             .withEnvId(env.getUuid())
                             .withExecutionUuid(workflowExecution.getUuid())
                             .build();
    try {
      executionInterrupt = executionInterruptManager.registerExecutionInterrupt(executionInterrupt);
      failBecauseExceptionWasNotThrown(WingsException.class);
    } catch (WingsException exception) {
      assertThat(exception).hasMessage(ErrorCode.RESUME_ALL_ALREADY.name());
    }
  }
}
