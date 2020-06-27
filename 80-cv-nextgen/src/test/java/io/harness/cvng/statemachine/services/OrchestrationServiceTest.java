package io.harness.cvng.statemachine.services;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.PRAVEEN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.cvng.CVNextGenBaseTest;
import io.harness.cvng.core.services.entities.AppDynamicsCVConfig;
import io.harness.cvng.core.services.entities.CVConfig;
import io.harness.cvng.models.VerificationType;
import io.harness.cvng.statemachine.entities.AnalysisOrchestrator;
import io.harness.cvng.statemachine.entities.AnalysisOrchestrator.AnalysisOrchestratorKeys;
import io.harness.cvng.statemachine.entities.AnalysisStateMachine;
import io.harness.cvng.statemachine.entities.AnalysisStatus;
import io.harness.cvng.statemachine.exception.AnalysisOrchestrationException;
import io.harness.cvng.statemachine.services.intfc.AnalysisStateMachineService;
import io.harness.cvng.statemachine.services.intfc.OrchestrationService;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class OrchestrationServiceTest extends CVNextGenBaseTest {
  @Inject HPersistence hPersistence;

  @Mock AnalysisStateMachineService mockStateMachineService;
  @Inject OrchestrationService orchestrationService;
  private String cvConfigId;

  @Before
  public void setup() throws Exception {
    cvConfigId = generateUuid();
    CVConfig cvConfig = new AppDynamicsCVConfig();
    cvConfig.setVerificationType(VerificationType.TIME_SERIES);
    cvConfig.setUuid(cvConfigId);
    hPersistence.save(cvConfig);
    MockitoAnnotations.initMocks(this);
    FieldUtils.writeField(orchestrationService, "stateMachineService", mockStateMachineService, true);
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testQueueAnalysis_firstEverOrchestration() {
    AnalysisOrchestrator orchestrator = hPersistence.createQuery(AnalysisOrchestrator.class)
                                            .filter(AnalysisOrchestratorKeys.cvConfigId, cvConfigId)
                                            .get();

    assertThat(orchestrator).isNull();

    orchestrationService.queueAnalysis(cvConfigId, Instant.now(), Instant.now().minus(5, ChronoUnit.MINUTES));

    orchestrator = hPersistence.createQuery(AnalysisOrchestrator.class)
                       .filter(AnalysisOrchestratorKeys.cvConfigId, cvConfigId)
                       .get();

    assertThat(orchestrator).isNotNull();
    assertThat(orchestrator.getStatus().name()).isEqualTo(AnalysisStatus.RUNNING.name());
  }

  @Test(expected = AnalysisOrchestrationException.class)
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testQueueAnalysis_firstEverOrchestrationInvalidInputs() {
    AnalysisOrchestrator orchestrator = hPersistence.createQuery(AnalysisOrchestrator.class)
                                            .filter(AnalysisOrchestratorKeys.cvConfigId, cvConfigId)
                                            .get();

    assertThat(orchestrator).isNull();

    orchestrationService.queueAnalysis(cvConfigId, Instant.now(), null);
  }

  @Test(expected = AnalysisOrchestrationException.class)
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testQueueAnalysis_firstEverOrchestrationBadCvConfig() {
    AnalysisOrchestrator orchestrator = hPersistence.createQuery(AnalysisOrchestrator.class)
                                            .filter(AnalysisOrchestratorKeys.cvConfigId, cvConfigId)
                                            .get();

    assertThat(orchestrator).isNull();

    int lastCollectionMinute =
        (int) TimeUnit.MILLISECONDS.toMinutes(Instant.now().minus(5, ChronoUnit.MINUTES).toEpochMilli());
    orchestrationService.queueAnalysis(cvConfigId + "-bad", Instant.now(), Instant.now().minus(5, ChronoUnit.MINUTES));
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testOrchestrate_currentStateMachineDoneNothingNewToExecute() {
    AnalysisOrchestrator orchestrator = AnalysisOrchestrator
                                            .builder()

                                            .cvConfigId(cvConfigId)
                                            .status(AnalysisStatus.RUNNING)
                                            .build();
    hPersistence.save(orchestrator);

    AnalysisStateMachine stateMachine = AnalysisStateMachine.builder()
                                            .status(AnalysisStatus.SUCCESS)
                                            .cvConfigId(cvConfigId)
                                            .nextAttemptTime(Instant.now().toEpochMilli())
                                            .build();
    when(mockStateMachineService.getExecutingStateMachine(cvConfigId)).thenReturn(stateMachine);

    orchestrationService.orchestrate(cvConfigId);

    // verify
    verify(mockStateMachineService).getExecutingStateMachine(cvConfigId);
    verify(mockStateMachineService, times(0)).executeStateMachine(any(AnalysisStateMachine.class));
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testOrchestrate_currentStateMachineDoneExecuteNext() {
    AnalysisOrchestrator orchestrator = AnalysisOrchestrator.builder()
                                            .cvConfigId(cvConfigId)
                                            .status(AnalysisStatus.RUNNING)
                                            .analysisStateMachineQueue(new ArrayList<>())
                                            .build();

    AnalysisStateMachine stateMachine =
        AnalysisStateMachine.builder().status(AnalysisStatus.SUCCESS).cvConfigId(cvConfigId).build();

    AnalysisStateMachine nextStateMachine = AnalysisStateMachine.builder()
                                                .status(AnalysisStatus.CREATED)
                                                .cvConfigId(cvConfigId)
                                                .analysisStartTime(Instant.now().minus(5, ChronoUnit.MINUTES))
                                                .analysisEndTime(Instant.now())
                                                .build();
    orchestrator.getAnalysisStateMachineQueue().add(nextStateMachine);

    hPersistence.save(orchestrator);

    when(mockStateMachineService.getExecutingStateMachine(cvConfigId)).thenReturn(stateMachine);

    orchestrationService.orchestrate(cvConfigId);

    // verify
    verify(mockStateMachineService).getExecutingStateMachine(cvConfigId);
    verify(mockStateMachineService, times(1)).initiateStateMachine(cvConfigId, nextStateMachine);
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testOrchestrate_currentlyRunning() {
    AnalysisOrchestrator orchestrator = AnalysisOrchestrator
                                            .builder()

                                            .cvConfigId(cvConfigId)
                                            .status(AnalysisStatus.RUNNING)
                                            .build();
    hPersistence.save(orchestrator);

    AnalysisStateMachine stateMachine = AnalysisStateMachine.builder()
                                            .status(AnalysisStatus.RUNNING)
                                            .cvConfigId(cvConfigId)
                                            .nextAttemptTime(Instant.now().toEpochMilli())
                                            .build();
    when(mockStateMachineService.getExecutingStateMachine(cvConfigId)).thenReturn(stateMachine);

    orchestrationService.orchestrate(cvConfigId);

    // verify
    verify(mockStateMachineService).getExecutingStateMachine(cvConfigId);
    verify(mockStateMachineService).executeStateMachine(stateMachine);
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testOrchestrate_failed() {
    AnalysisOrchestrator orchestrator = AnalysisOrchestrator
                                            .builder()

                                            .cvConfigId(cvConfigId)
                                            .status(AnalysisStatus.RUNNING)
                                            .build();
    hPersistence.save(orchestrator);

    AnalysisStateMachine stateMachine = AnalysisStateMachine.builder()
                                            .status(AnalysisStatus.FAILED)
                                            .cvConfigId(cvConfigId)
                                            .nextAttemptTime(Instant.now().toEpochMilli())
                                            .build();
    when(mockStateMachineService.getExecutingStateMachine(cvConfigId)).thenReturn(stateMachine);

    orchestrationService.orchestrate(cvConfigId);

    // verify
    verify(mockStateMachineService).getExecutingStateMachine(cvConfigId);
    verify(mockStateMachineService).retryStateMachineAfterFailure(stateMachine);
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testOrchestrate_timeout() {
    AnalysisOrchestrator orchestrator = AnalysisOrchestrator
                                            .builder()

                                            .cvConfigId(cvConfigId)
                                            .status(AnalysisStatus.RUNNING)
                                            .build();
    hPersistence.save(orchestrator);

    AnalysisStateMachine stateMachine = AnalysisStateMachine.builder()
                                            .status(AnalysisStatus.TIMEOUT)
                                            .cvConfigId(cvConfigId)
                                            .nextAttemptTime(Instant.now().toEpochMilli())
                                            .build();
    when(mockStateMachineService.getExecutingStateMachine(cvConfigId)).thenReturn(stateMachine);

    orchestrationService.orchestrate(cvConfigId);

    // verify
    verify(mockStateMachineService).getExecutingStateMachine(cvConfigId);
    verify(mockStateMachineService).retryStateMachineAfterFailure(stateMachine);
  }

  @Test(expected = AnalysisOrchestrationException.class)
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testOrchestrate_badCvConfigId() {
    orchestrationService.orchestrate(cvConfigId + "-bad");
  }
}
