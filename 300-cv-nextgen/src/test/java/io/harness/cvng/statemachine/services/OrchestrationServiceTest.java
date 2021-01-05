package io.harness.cvng.statemachine.services;

import static io.harness.cvng.CVConstants.STATE_MACHINE_IGNORE_LIMIT;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.PRAVEEN;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CvNextGenTest;
import io.harness.category.element.UnitTests;
import io.harness.cvng.core.entities.AppDynamicsCVConfig;
import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.core.services.api.VerificationTaskService;
import io.harness.cvng.models.VerificationType;
import io.harness.cvng.statemachine.beans.AnalysisStatus;
import io.harness.cvng.statemachine.entities.AnalysisOrchestrator;
import io.harness.cvng.statemachine.entities.AnalysisOrchestrator.AnalysisOrchestratorKeys;
import io.harness.cvng.statemachine.entities.AnalysisStateMachine;
import io.harness.cvng.statemachine.services.intfc.AnalysisStateMachineService;
import io.harness.cvng.statemachine.services.intfc.OrchestrationService;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class OrchestrationServiceTest extends CvNextGenTest {
  @Inject HPersistence hPersistence;

  @Mock AnalysisStateMachineService mockStateMachineService;
  @Inject private VerificationTaskService verificationTaskService;
  @Inject OrchestrationService orchestrationService;
  private String cvConfigId;
  private String verificationTaskId;
  private String accountId;

  @Before
  public void setup() throws Exception {
    cvConfigId = generateUuid();
    accountId = generateUuid();
    CVConfig cvConfig = new AppDynamicsCVConfig();
    cvConfig.setVerificationType(VerificationType.TIME_SERIES);
    cvConfig.setUuid(cvConfigId);
    hPersistence.save(cvConfig);
    MockitoAnnotations.initMocks(this);
    verificationTaskId = verificationTaskService.create(accountId, cvConfigId);
    FieldUtils.writeField(orchestrationService, "stateMachineService", mockStateMachineService, true);
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testQueueAnalysis_firstEverOrchestration() {
    AnalysisOrchestrator orchestrator = hPersistence.createQuery(AnalysisOrchestrator.class)
                                            .filter(AnalysisOrchestratorKeys.verificationTaskId, cvConfigId)
                                            .get();

    assertThat(orchestrator).isNull();

    when(mockStateMachineService.ignoreOldStatemachine(any())).thenReturn(Optional.empty());
    orchestrationService.queueAnalysis(cvConfigId, Instant.now(), Instant.now().minus(5, ChronoUnit.MINUTES));

    orchestrator = hPersistence.createQuery(AnalysisOrchestrator.class)
                       .filter(AnalysisOrchestratorKeys.verificationTaskId, cvConfigId)
                       .get();

    assertThat(orchestrator).isNotNull();
    assertThat(orchestrator.getStatus().name()).isEqualTo(AnalysisStatus.RUNNING.name());
  }

  @Test(expected = NullPointerException.class)
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testQueueAnalysis_firstEverOrchestrationInvalidInputs() {
    AnalysisOrchestrator orchestrator = hPersistence.createQuery(AnalysisOrchestrator.class)
                                            .filter(AnalysisOrchestratorKeys.verificationTaskId, cvConfigId)
                                            .get();

    assertThat(orchestrator).isNull();

    orchestrationService.queueAnalysis(cvConfigId, Instant.now(), null);
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testOrchestrate_currentStateMachineDoneNothingNewToExecute() {
    AnalysisOrchestrator orchestrator = AnalysisOrchestrator
                                            .builder()

                                            .verificationTaskId(cvConfigId)
                                            .status(AnalysisStatus.RUNNING)
                                            .build();
    hPersistence.save(orchestrator);

    AnalysisStateMachine stateMachine = AnalysisStateMachine.builder()
                                            .status(AnalysisStatus.SUCCESS)
                                            .verificationTaskId(verificationTaskId)
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
                                            .verificationTaskId(cvConfigId)
                                            .status(AnalysisStatus.RUNNING)
                                            .analysisStateMachineQueue(new ArrayList<>())
                                            .build();

    AnalysisStateMachine stateMachine =
        AnalysisStateMachine.builder().status(AnalysisStatus.SUCCESS).verificationTaskId(verificationTaskId).build();

    AnalysisStateMachine nextStateMachine = AnalysisStateMachine.builder()
                                                .status(AnalysisStatus.CREATED)
                                                .verificationTaskId(verificationTaskId)
                                                .analysisStartTime(Instant.now().minus(5, ChronoUnit.MINUTES))
                                                .analysisEndTime(Instant.now())
                                                .build();
    orchestrator.getAnalysisStateMachineQueue().add(nextStateMachine);

    hPersistence.save(orchestrator);

    when(mockStateMachineService.getExecutingStateMachine(cvConfigId)).thenReturn(stateMachine);
    when(mockStateMachineService.ignoreOldStatemachine(any())).thenReturn(Optional.empty());

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

                                            .verificationTaskId(cvConfigId)
                                            .status(AnalysisStatus.RUNNING)
                                            .build();
    hPersistence.save(orchestrator);

    AnalysisStateMachine stateMachine = AnalysisStateMachine.builder()
                                            .status(AnalysisStatus.RUNNING)
                                            .verificationTaskId(verificationTaskId)
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

                                            .verificationTaskId(cvConfigId)
                                            .status(AnalysisStatus.RUNNING)
                                            .build();
    hPersistence.save(orchestrator);

    AnalysisStateMachine stateMachine = AnalysisStateMachine.builder()
                                            .status(AnalysisStatus.FAILED)
                                            .verificationTaskId(verificationTaskId)
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

                                            .verificationTaskId(cvConfigId)
                                            .status(AnalysisStatus.RUNNING)
                                            .build();
    hPersistence.save(orchestrator);

    AnalysisStateMachine stateMachine = AnalysisStateMachine.builder()
                                            .status(AnalysisStatus.TIMEOUT)
                                            .verificationTaskId(verificationTaskId)
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
  public void testOrchestrate_ignoreStateMachine() {
    AnalysisOrchestrator orchestrator = AnalysisOrchestrator.builder()
                                            .verificationTaskId(cvConfigId)
                                            .status(AnalysisStatus.RUNNING)
                                            .analysisStateMachineQueue(new ArrayList<>())
                                            .build();

    AnalysisStateMachine stateMachine =
        AnalysisStateMachine.builder().status(AnalysisStatus.SUCCESS).verificationTaskId(verificationTaskId).build();

    AnalysisStateMachine firstSM = AnalysisStateMachine.builder()
                                       .status(AnalysisStatus.CREATED)
                                       .verificationTaskId(verificationTaskId)
                                       .analysisStartTime(Instant.now().minus(5, ChronoUnit.MINUTES))
                                       .analysisEndTime(Instant.now())
                                       .build();
    orchestrator.getAnalysisStateMachineQueue().add(firstSM);
    when(mockStateMachineService.ignoreOldStatemachine(firstSM)).thenReturn(Optional.of(firstSM));

    AnalysisStateMachine nextSM = AnalysisStateMachine.builder()
                                      .status(AnalysisStatus.CREATED)
                                      .verificationTaskId(verificationTaskId)
                                      .analysisStartTime(Instant.now())
                                      .analysisEndTime(Instant.now().plus(5, ChronoUnit.MINUTES))
                                      .build();
    orchestrator.getAnalysisStateMachineQueue().add(nextSM);
    when(mockStateMachineService.ignoreOldStatemachine(nextSM)).thenReturn(Optional.empty());

    hPersistence.save(orchestrator);

    when(mockStateMachineService.getExecutingStateMachine(cvConfigId)).thenReturn(stateMachine);

    orchestrationService.orchestrate(cvConfigId);

    // verify
    verify(mockStateMachineService).getExecutingStateMachine(cvConfigId);
    verify(mockStateMachineService).ignoreOldStatemachine(firstSM);
    verify(mockStateMachineService).ignoreOldStatemachine(nextSM);
    verify(mockStateMachineService, times(1)).initiateStateMachine(cvConfigId, nextSM);

    AnalysisOrchestrator orchestratorFromDB = hPersistence.createQuery(AnalysisOrchestrator.class).get();
    assertThat(orchestratorFromDB.getAnalysisStateMachineQueue()).isNull();
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testOrchestrate_ignoreStateMachine100OldOnes() {
    AnalysisOrchestrator orchestrator = AnalysisOrchestrator.builder()
                                            .verificationTaskId(cvConfigId)
                                            .status(AnalysisStatus.RUNNING)
                                            .analysisStateMachineQueue(new ArrayList<>())
                                            .build();

    AnalysisStateMachine stateMachine =
        AnalysisStateMachine.builder().status(AnalysisStatus.SUCCESS).verificationTaskId(verificationTaskId).build();

    List<AnalysisStateMachine> ignoreList = new ArrayList<>();

    for (int i = 0; i < 110; i++) {
      AnalysisStateMachine firstSM = AnalysisStateMachine.builder()
                                         .status(AnalysisStatus.CREATED)
                                         .verificationTaskId(verificationTaskId)
                                         .analysisStartTime(Instant.now().minus(5, ChronoUnit.MINUTES))
                                         .analysisEndTime(Instant.now())
                                         .build();
      orchestrator.getAnalysisStateMachineQueue().add(firstSM);
      when(mockStateMachineService.ignoreOldStatemachine(firstSM)).thenReturn(Optional.of(firstSM));

      if (i < STATE_MACHINE_IGNORE_LIMIT) {
        ignoreList.add(firstSM);
      }
    }

    hPersistence.save(orchestrator);

    when(mockStateMachineService.getExecutingStateMachine(cvConfigId)).thenReturn(stateMachine);

    orchestrationService.orchestrate(cvConfigId);

    // verify
    verify(mockStateMachineService).getExecutingStateMachine(cvConfigId);
    verify(mockStateMachineService).save(ignoreList);

    AnalysisOrchestrator orchestratorFromDB = hPersistence.createQuery(AnalysisOrchestrator.class).get();
    assertThat(orchestratorFromDB.getAnalysisStateMachineQueue().size()).isEqualTo(10);
  }
}
