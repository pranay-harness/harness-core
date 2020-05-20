package software.wings.sm;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.ANUBHAW;
import static io.harness.rule.OwnerRule.GARVIT;
import static io.harness.rule.OwnerRule.GEORGE;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;
import static software.wings.sm.ExecutionInterrupt.ExecutionInterruptBuilder.anExecutionInterrupt;

import com.google.inject.Inject;

import io.harness.beans.ExecutionStatus;
import io.harness.category.element.UnitTests;
import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import io.harness.interrupts.ExecutionInterruptType;
import io.harness.rule.Owner;
import lombok.extern.slf4j.Slf4j;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.Environment.Builder;
import software.wings.beans.WorkflowExecution;
import software.wings.dl.WingsPersistence;

import java.util.Collections;
import java.util.List;

/**
 * The type State machine execution event manager test.
 *
 * @author Rishi
 */
@Slf4j
public class ExecutionInterruptManagerTest extends WingsBaseTest {
  @Inject private WingsPersistence wingsPersistence;
  @InjectMocks @Inject private ExecutionInterruptManager executionInterruptManager;
  @Mock private StateMachineExecutor stateMachineExecutor;

  /**
   * Should throw invalid argument for null state execution instance.
   */
  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void shouldThrowInvalidArgumentForNullStateExecutionInstance() {
    Application app =
        wingsPersistence.saveAndGet(Application.class, Application.Builder.anApplication().name("App1").build());
    Environment env =
        wingsPersistence.saveAndGet(Environment.class, Builder.anEnvironment().appId(app.getUuid()).build());
    WorkflowExecution workflowExecution = WorkflowExecution.builder().appId(app.getAppId()).build();
    wingsPersistence.save(workflowExecution);
    ExecutionInterrupt executionInterrupt = anExecutionInterrupt()
                                                .appId(app.getUuid())
                                                .executionInterruptType(ExecutionInterruptType.PAUSE)
                                                .envId(env.getUuid())
                                                .executionUuid(workflowExecution.getUuid())
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
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void shouldThrowInvalidArgumentForInvalidStateExecutionInstance() {
    Application app =
        wingsPersistence.saveAndGet(Application.class, Application.Builder.anApplication().name("App1").build());
    Environment env =
        wingsPersistence.saveAndGet(Environment.class, Builder.anEnvironment().appId(app.getUuid()).build());
    ExecutionInterrupt executionInterrupt = anExecutionInterrupt()
                                                .appId(app.getUuid())
                                                .executionInterruptType(ExecutionInterruptType.PAUSE)
                                                .envId(env.getUuid())
                                                .executionUuid(generateUuid())
                                                .stateExecutionInstanceId(generateUuid())
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
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void shouldThrowStateNotForResume() {
    Application app =
        wingsPersistence.saveAndGet(Application.class, Application.Builder.anApplication().name("App1").build());
    Environment env =
        wingsPersistence.saveAndGet(Environment.class, Builder.anEnvironment().appId(app.getUuid()).build());
    StateExecutionInstance stateExecutionInstance =
        StateExecutionInstance.Builder.aStateExecutionInstance().appId(app.getUuid()).displayName("state1").build();
    wingsPersistence.save(stateExecutionInstance);
    ExecutionInterrupt executionInterrupt = anExecutionInterrupt()
                                                .appId(app.getUuid())
                                                .executionInterruptType(ExecutionInterruptType.RESUME)
                                                .envId(env.getUuid())
                                                .executionUuid(generateUuid())
                                                .stateExecutionInstanceId(stateExecutionInstance.getUuid())
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
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void shouldThrowStateNotForRetry() {
    Application app =
        wingsPersistence.saveAndGet(Application.class, Application.Builder.anApplication().name("App1").build());
    Environment env =
        wingsPersistence.saveAndGet(Environment.class, Builder.anEnvironment().appId(app.getUuid()).build());
    StateExecutionInstance stateExecutionInstance =
        StateExecutionInstance.Builder.aStateExecutionInstance().appId(app.getUuid()).displayName("state1").build();
    wingsPersistence.save(stateExecutionInstance);
    ExecutionInterrupt executionInterrupt = anExecutionInterrupt()
                                                .appId(app.getUuid())
                                                .executionInterruptType(ExecutionInterruptType.RETRY)
                                                .envId(env.getUuid())
                                                .executionUuid(generateUuid())
                                                .stateExecutionInstanceId(stateExecutionInstance.getUuid())
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
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void shouldThrowStateNotForPause() {
    Application app =
        wingsPersistence.saveAndGet(Application.class, Application.Builder.anApplication().name("App1").build());
    Environment env =
        wingsPersistence.saveAndGet(Environment.class, Builder.anEnvironment().appId(app.getUuid()).build());
    StateExecutionInstance stateExecutionInstance = StateExecutionInstance.Builder.aStateExecutionInstance()
                                                        .appId(app.getUuid())
                                                        .displayName("state1")
                                                        .status(ExecutionStatus.SUCCESS)
                                                        .build();
    wingsPersistence.save(stateExecutionInstance);
    ExecutionInterrupt executionInterrupt = anExecutionInterrupt()
                                                .appId(app.getUuid())
                                                .executionInterruptType(ExecutionInterruptType.PAUSE)
                                                .envId(env.getUuid())
                                                .executionUuid(generateUuid())
                                                .stateExecutionInstanceId(stateExecutionInstance.getUuid())
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
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void shouldThrowStateNotForAbort() {
    Application app =
        wingsPersistence.saveAndGet(Application.class, Application.Builder.anApplication().name("App1").build());
    Environment env =
        wingsPersistence.saveAndGet(Environment.class, Builder.anEnvironment().appId(app.getUuid()).build());
    StateExecutionInstance stateExecutionInstance = StateExecutionInstance.Builder.aStateExecutionInstance()
                                                        .appId(app.getUuid())
                                                        .displayName("state1")
                                                        .status(ExecutionStatus.SUCCESS)
                                                        .build();
    wingsPersistence.save(stateExecutionInstance);
    ExecutionInterrupt executionInterrupt = anExecutionInterrupt()
                                                .appId(app.getUuid())
                                                .executionInterruptType(ExecutionInterruptType.ABORT)
                                                .envId(env.getUuid())
                                                .executionUuid(generateUuid())
                                                .stateExecutionInstanceId(stateExecutionInstance.getUuid())
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
  @Owner(developers = ANUBHAW)
  @Category(UnitTests.class)
  @Ignore("TODO: please provide clear motivation why this test is ignored")
  public void shouldThrowAbortAllAlready() {
    Application app =
        wingsPersistence.saveAndGet(Application.class, Application.Builder.anApplication().name("App1").build());
    Environment env =
        wingsPersistence.saveAndGet(Environment.class, Builder.anEnvironment().appId(app.getUuid()).build());

    String executionUuid = generateUuid();
    ExecutionInterrupt executionInterrupt = anExecutionInterrupt()
                                                .appId(app.getUuid())
                                                .executionInterruptType(ExecutionInterruptType.ABORT_ALL)
                                                .envId(env.getUuid())
                                                .executionUuid(executionUuid)
                                                .build();
    executionInterrupt = executionInterruptManager.registerExecutionInterrupt(executionInterrupt);

    executionInterrupt = anExecutionInterrupt()
                             .appId(app.getUuid())
                             .executionInterruptType(ExecutionInterruptType.PAUSE_ALL)
                             .envId(env.getUuid())
                             .executionUuid(executionUuid)
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
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void shouldThrowPauseAllAlready() {
    Application app =
        wingsPersistence.saveAndGet(Application.class, Application.Builder.anApplication().name("App1").build());
    Environment env =
        wingsPersistence.saveAndGet(Environment.class, Builder.anEnvironment().appId(app.getUuid()).build());
    WorkflowExecution workflowExecution = WorkflowExecution.builder().appId(app.getAppId()).build();
    wingsPersistence.save(workflowExecution);

    ExecutionInterrupt executionInterrupt = anExecutionInterrupt()
                                                .appId(app.getUuid())
                                                .executionInterruptType(ExecutionInterruptType.PAUSE_ALL)
                                                .envId(env.getUuid())
                                                .executionUuid(workflowExecution.getUuid())
                                                .build();
    executionInterrupt = executionInterruptManager.registerExecutionInterrupt(executionInterrupt);

    executionInterrupt = anExecutionInterrupt()
                             .appId(app.getUuid())
                             .executionInterruptType(ExecutionInterruptType.PAUSE_ALL)
                             .envId(env.getUuid())
                             .executionUuid(workflowExecution.getUuid())
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
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void shouldPauseAllClearPreviousResumeAll() {
    Application app =
        wingsPersistence.saveAndGet(Application.class, Application.Builder.anApplication().name("App1").build());
    Environment env =
        wingsPersistence.saveAndGet(Environment.class, Builder.anEnvironment().appId(app.getUuid()).build());
    WorkflowExecution workflowExecution = WorkflowExecution.builder().appId(app.getAppId()).build();
    wingsPersistence.save(workflowExecution);

    ExecutionInterrupt executionInterrupt = anExecutionInterrupt()
                                                .appId(app.getUuid())
                                                .executionInterruptType(ExecutionInterruptType.PAUSE_ALL)
                                                .envId(env.getUuid())
                                                .executionUuid(workflowExecution.getUuid())
                                                .build();
    executionInterrupt = executionInterruptManager.registerExecutionInterrupt(executionInterrupt);

    ExecutionInterrupt resumeAll = anExecutionInterrupt()
                                       .appId(app.getUuid())
                                       .executionInterruptType(ExecutionInterruptType.RESUME_ALL)
                                       .envId(env.getUuid())
                                       .executionUuid(workflowExecution.getUuid())
                                       .build();
    resumeAll = executionInterruptManager.registerExecutionInterrupt(resumeAll);

    executionInterrupt = anExecutionInterrupt()
                             .appId(app.getUuid())
                             .executionInterruptType(ExecutionInterruptType.PAUSE_ALL)
                             .envId(env.getUuid())
                             .executionUuid(workflowExecution.getUuid())
                             .build();
    executionInterrupt = executionInterruptManager.registerExecutionInterrupt(executionInterrupt);
    final ExecutionInterrupt interrupt =
        wingsPersistence.getWithAppId(ExecutionInterrupt.class, resumeAll.getAppId(), resumeAll.getUuid());
    assertThat(interrupt.isSeized()).isTrue();
  }

  /**
   * Should throw resume all already.
   */
  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void shouldThrowResumeAllAlready() {
    Application app =
        wingsPersistence.saveAndGet(Application.class, Application.Builder.anApplication().name("App1").build());
    Environment env =
        wingsPersistence.saveAndGet(Environment.class, Builder.anEnvironment().appId(app.getUuid()).build());

    String executionUuid = generateUuid();
    ExecutionInterrupt executionInterrupt = anExecutionInterrupt()
                                                .appId(app.getUuid())
                                                .executionInterruptType(ExecutionInterruptType.RESUME_ALL)
                                                .envId(env.getUuid())
                                                .executionUuid(executionUuid)
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
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void shouldResumeAllClearPrevPauseAll() {
    Application app =
        wingsPersistence.saveAndGet(Application.class, Application.Builder.anApplication().name("App1").build());
    Environment env =
        wingsPersistence.saveAndGet(Environment.class, Builder.anEnvironment().appId(app.getUuid()).build());
    WorkflowExecution workflowExecution = WorkflowExecution.builder().appId(app.getAppId()).build();
    wingsPersistence.save(workflowExecution);

    ExecutionInterrupt pauseAll = anExecutionInterrupt()
                                      .appId(app.getUuid())
                                      .executionInterruptType(ExecutionInterruptType.PAUSE_ALL)
                                      .envId(env.getUuid())
                                      .executionUuid(workflowExecution.getUuid())
                                      .build();
    pauseAll = executionInterruptManager.registerExecutionInterrupt(pauseAll);

    ExecutionInterrupt executionInterrupt = anExecutionInterrupt()
                                                .appId(app.getUuid())
                                                .executionInterruptType(ExecutionInterruptType.RESUME_ALL)
                                                .envId(env.getUuid())
                                                .executionUuid(workflowExecution.getUuid())
                                                .build();
    executionInterrupt = executionInterruptManager.registerExecutionInterrupt(executionInterrupt);

    final ExecutionInterrupt interrupt =
        wingsPersistence.getWithAppId(ExecutionInterrupt.class, pauseAll.getAppId(), pauseAll.getUuid());
    assertThat(interrupt.isSeized()).isTrue();
  }

  /**
   * Should throw resume all already 2.
   */
  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void shouldThrowResumeAllAlready2() {
    Application app =
        wingsPersistence.saveAndGet(Application.class, Application.Builder.anApplication().name("App1").build());
    Environment env =
        wingsPersistence.saveAndGet(Environment.class, Builder.anEnvironment().appId(app.getUuid()).build());
    WorkflowExecution workflowExecution = WorkflowExecution.builder().appId(app.getAppId()).build();
    wingsPersistence.save(workflowExecution);

    ExecutionInterrupt pauseAll = anExecutionInterrupt()
                                      .appId(app.getUuid())
                                      .executionInterruptType(ExecutionInterruptType.PAUSE_ALL)
                                      .envId(env.getUuid())
                                      .executionUuid(workflowExecution.getUuid())
                                      .build();
    pauseAll = executionInterruptManager.registerExecutionInterrupt(pauseAll);

    ExecutionInterrupt executionInterrupt = anExecutionInterrupt()
                                                .appId(app.getUuid())
                                                .executionInterruptType(ExecutionInterruptType.RESUME_ALL)
                                                .envId(env.getUuid())
                                                .executionUuid(workflowExecution.getUuid())
                                                .build();
    executionInterrupt = executionInterruptManager.registerExecutionInterrupt(executionInterrupt);

    executionInterrupt = anExecutionInterrupt()
                             .appId(app.getUuid())
                             .executionInterruptType(ExecutionInterruptType.RESUME_ALL)
                             .envId(env.getUuid())
                             .executionUuid(workflowExecution.getUuid())
                             .build();
    try {
      executionInterrupt = executionInterruptManager.registerExecutionInterrupt(executionInterrupt);
      failBecauseExceptionWasNotThrown(WingsException.class);
    } catch (WingsException exception) {
      assertThat(exception).hasMessage(ErrorCode.RESUME_ALL_ALREADY.name());
    }
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testListByIdsUsingSecondary() {
    List<ExecutionInterrupt> executionInterrupts =
        executionInterruptManager.listByIdsUsingSecondary(Collections.emptyList());
    assertThat(executionInterrupts).isNotNull();
    assertThat(executionInterrupts).isEmpty();

    ExecutionInterrupt executionInterrupt = anExecutionInterrupt().executionUuid("id").build();
    String id = wingsPersistence.save(executionInterrupt);

    executionInterrupts = executionInterruptManager.listByIdsUsingSecondary(asList(id, "random"));
    assertThat(executionInterrupts).isNotNull();
    assertThat(executionInterrupts.size()).isEqualTo(1);
    assertThat(executionInterrupts.get(0).getUuid()).isEqualTo(id);
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testListByStateExecutionIdsUsingSecondary() {
    List<ExecutionInterrupt> executionInterrupts =
        executionInterruptManager.listByStateExecutionIdsUsingSecondary(Collections.emptyList());
    assertThat(executionInterrupts).isNotNull();
    assertThat(executionInterrupts).isEmpty();

    ExecutionInterrupt executionInterrupt =
        anExecutionInterrupt().stateExecutionInstanceId("seid").executionUuid("id").build();
    String id = wingsPersistence.save(executionInterrupt);

    executionInterrupts = executionInterruptManager.listByStateExecutionIdsUsingSecondary(asList("seid", "random"));
    assertThat(executionInterrupts).isNotNull();
    assertThat(executionInterrupts.size()).isEqualTo(1);
    assertThat(executionInterrupts.get(0).getUuid()).isEqualTo(id);
  }
}
