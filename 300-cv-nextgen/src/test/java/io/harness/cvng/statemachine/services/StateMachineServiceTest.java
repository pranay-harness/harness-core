package io.harness.cvng.statemachine.services;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.PRAVEEN;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CvNextGenTest;
import io.harness.category.element.UnitTests;
import io.harness.cvng.analysis.services.api.TimeSeriesAnalysisService;
import io.harness.cvng.beans.DataSourceType;
import io.harness.cvng.core.entities.AppDynamicsCVConfig;
import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.core.entities.VerificationTask;
import io.harness.cvng.core.services.api.CVConfigService;
import io.harness.cvng.core.services.api.VerificationTaskService;
import io.harness.cvng.models.VerificationType;
import io.harness.cvng.statemachine.beans.AnalysisInput;
import io.harness.cvng.statemachine.beans.AnalysisStatus;
import io.harness.cvng.statemachine.entities.AnalysisStateMachine;
import io.harness.cvng.statemachine.entities.ServiceGuardTimeSeriesAnalysisState;
import io.harness.cvng.statemachine.entities.TimeSeriesAnalysisState;
import io.harness.cvng.statemachine.exception.AnalysisStateMachineException;
import io.harness.cvng.statemachine.services.intfc.AnalysisStateMachineService;
import io.harness.cvng.verificationjob.beans.CanaryVerificationJobDTO;
import io.harness.cvng.verificationjob.beans.Sensitivity;
import io.harness.cvng.verificationjob.beans.VerificationJobDTO;
import io.harness.cvng.verificationjob.entities.VerificationJobInstance;
import io.harness.cvng.verificationjob.services.api.VerificationJobInstanceService;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;

import com.google.common.collect.Lists;
import com.google.inject.Injector;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.Sort;

public class StateMachineServiceTest extends CvNextGenTest {
  private String cvConfigId;
  private String verificationTaskId;
  private CVConfig cvConfig;
  // TODO: Why do we have these test with mocks?
  //  Probably investigate and need to rewrite these test with actual services. These test are unmaintainable and using
  //  too many mocks.
  @Mock HPersistence hPersistence;
  @Mock Injector injector;

  @Mock private TimeSeriesAnalysisState timeSeriesAnalysisState;
  @Mock private TimeSeriesAnalysisService mockTimeSeriesAnalysisService;
  @Mock private Query<AnalysisStateMachine> stateMachineQuery;
  @Mock private Query<CVConfig> cvConfigQuery;
  @Mock private CVConfigService cvConfigService;
  @Mock private VerificationTaskService verificationTaskService;
  @Mock private VerificationJobInstanceService verificationJobInstanceService;
  @InjectMocks AnalysisStateMachineService stateMachineService = new AnalysisStateMachineServiceImpl();

  @Before
  public void setup() {
    cvConfigId = generateUuid();
    verificationTaskId = generateUuid();
    cvConfig = new AppDynamicsCVConfig();
    cvConfig.setVerificationType(VerificationType.TIME_SERIES);
    cvConfig.setUuid(cvConfigId);

    MockitoAnnotations.initMocks(this);
    // FieldUtils.writeField(stateMachineService, "injector", injector, true);
    doNothing().when(injector).injectMembers(any());
    when(hPersistence.createQuery(CVConfig.class)).thenReturn(cvConfigQuery);
    when(cvConfigQuery.filter(any(), any())).thenReturn(cvConfigQuery);
    when(cvConfigQuery.get()).thenReturn(cvConfig);

    when(hPersistence.createQuery(AnalysisStateMachine.class)).thenReturn(stateMachineQuery);
    when(stateMachineQuery.filter(any(), any())).thenReturn(stateMachineQuery);
    when(stateMachineQuery.order(Sort.descending(any()))).thenReturn(stateMachineQuery);
    when(stateMachineQuery.get()).thenReturn(buildStateMachine(AnalysisStatus.RUNNING));
    when(cvConfigService.get(any())).thenReturn(cvConfig);
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testCreateStateMachine_forServiceGuard() {
    when(cvConfigService.get(cvConfigId)).thenReturn(cvConfig);
    when(verificationTaskService.isServiceGuardId(anyString())).thenReturn(true);
    AnalysisInput inputs = AnalysisInput.builder()
                               .verificationTaskId(verificationTaskId)
                               .startTime(Instant.now().minus(5, ChronoUnit.MINUTES))
                               .endTime(Instant.now())
                               .build();

    AnalysisStateMachine stateMachine = stateMachineService.createStateMachine(inputs);
    assertThat(stateMachine).isNotNull();
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testCreateStateMachine_forDeployment() {
    String verificationTaskId = generateUuid();
    String verificationJobInstanceId = generateUuid();
    when(verificationTaskService.get(verificationTaskId))
        .thenReturn(VerificationTask.builder()
                        .verificationJobInstanceId(verificationJobInstanceId)
                        .cvConfigId(cvConfigId)
                        .build());
    when(verificationJobInstanceService.getVerificationJobInstance(verificationJobInstanceId))
        .thenReturn(
            VerificationJobInstance.builder().resolvedJob(newCanaryVerificationJobDTO().getVerificationJob()).build());

    AnalysisInput inputs = AnalysisInput.builder()
                               .verificationTaskId(verificationTaskId)
                               .startTime(Instant.now().minus(5, ChronoUnit.MINUTES))
                               .endTime(Instant.now())
                               .build();

    AnalysisStateMachine stateMachine = stateMachineService.createStateMachine(inputs);
    assertThat(stateMachine).isNotNull();
  }
  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testInitiateStatemachine() {
    when(stateMachineQuery.get()).thenReturn(null);
    int analysisMinute = (int) TimeUnit.MILLISECONDS.toMinutes(Instant.now().toEpochMilli());
    AnalysisStateMachine stateMachine = buildStateMachine(AnalysisStatus.CREATED);

    stateMachineService.initiateStateMachine(verificationTaskId, stateMachine);

    // verify
    ArgumentCaptor<AnalysisStateMachine> stateMachineArgumentCaptor =
        ArgumentCaptor.forClass(AnalysisStateMachine.class);
    verify(hPersistence).save(stateMachineArgumentCaptor.capture());
    verify(timeSeriesAnalysisState).execute();

    AnalysisStateMachine savedStateMachine = stateMachineArgumentCaptor.getValue();

    assertThat(savedStateMachine).isNotNull();
    assertThat(savedStateMachine.getVerificationTaskId()).isEqualTo(verificationTaskId);
    assertThat(savedStateMachine.getStatus().name()).isEqualTo(AnalysisStatus.RUNNING.name());
  }

  @Test(expected = NullPointerException.class)
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testInitiateStatemachine_badStateMachine() {
    int analysisMinute = (int) TimeUnit.MILLISECONDS.toMinutes(Instant.now().toEpochMilli());
    AnalysisStateMachine stateMachine = AnalysisStateMachine.builder()
                                            .verificationTaskId(verificationTaskId)
                                            .analysisStartTime(Instant.now().minus(5, ChronoUnit.MINUTES))
                                            .analysisEndTime(Instant.now())
                                            .build();
    TimeSeriesAnalysisState timeSeriesAnalysisState = ServiceGuardTimeSeriesAnalysisState.builder().build();
    timeSeriesAnalysisState.setInputs(AnalysisInput.builder()
                                          .verificationTaskId(verificationTaskId)
                                          .startTime(Instant.now().minus(5, ChronoUnit.MINUTES))
                                          .endTime(Instant.now())
                                          .build());

    stateMachine.setCurrentState(timeSeriesAnalysisState);

    stateMachineService.initiateStateMachine(cvConfigId, null);
  }

  @Test(expected = NullPointerException.class)
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testInitiateStatemachine_badStateMachineNoFirstState() {
    int analysisMinute = (int) TimeUnit.MILLISECONDS.toMinutes(Instant.now().toEpochMilli());
    AnalysisStateMachine stateMachine = AnalysisStateMachine.builder()
                                            .verificationTaskId(verificationTaskId)
                                            .analysisStartTime(Instant.now().minus(5, ChronoUnit.MINUTES))
                                            .analysisEndTime(Instant.now())
                                            .build();

    stateMachineService.initiateStateMachine(verificationTaskId, stateMachine);
  }

  @Test(expected = AnalysisStateMachineException.class)
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testInitiateStatemachine_alreadyRunningStateMachine() {
    int analysisMinute = (int) TimeUnit.MILLISECONDS.toMinutes(Instant.now().toEpochMilli());
    AnalysisStateMachine stateMachine = AnalysisStateMachine.builder()
                                            .verificationTaskId(verificationTaskId)
                                            .analysisStartTime(Instant.now().minus(5, ChronoUnit.MINUTES))
                                            .analysisEndTime(Instant.now())
                                            .build();
    TimeSeriesAnalysisState timeSeriesAnalysisState = ServiceGuardTimeSeriesAnalysisState.builder().build();
    timeSeriesAnalysisState.setInputs(AnalysisInput.builder()
                                          .verificationTaskId(verificationTaskId)
                                          .startTime(Instant.now().minus(5, ChronoUnit.MINUTES))
                                          .endTime(Instant.now())
                                          .build());

    stateMachine.setCurrentState(timeSeriesAnalysisState);
    stateMachine.setStatus(AnalysisStatus.RUNNING);
    hPersistence.save(stateMachine);

    AnalysisStateMachine anotherStateMachine = AnalysisStateMachine.builder()
                                                   .analysisStartTime(Instant.now())
                                                   .analysisEndTime(Instant.now().plus(5, ChronoUnit.MINUTES))
                                                   .verificationTaskId(verificationTaskId)
                                                   .build();
    timeSeriesAnalysisState.getInputs().setStartTime(Instant.now());
    timeSeriesAnalysisState.getInputs().setStartTime(Instant.now().plus(5, ChronoUnit.MINUTES));
    anotherStateMachine.setCurrentState(timeSeriesAnalysisState);

    stateMachineService.initiateStateMachine(cvConfigId, anotherStateMachine);
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testExecuteStateMachine_currentlyRunning() {
    when(timeSeriesAnalysisState.getExecutionStatus()).thenReturn(AnalysisStatus.RUNNING);
    timeSeriesAnalysisState.setStatus(AnalysisStatus.RUNNING);
    when(timeSeriesAnalysisState.handleRunning()).thenReturn(timeSeriesAnalysisState);

    stateMachineService.executeStateMachine(cvConfigId);
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testExecuteStateMachine_currentlyCreated() {
    when(timeSeriesAnalysisState.getExecutionStatus())
        .thenReturn(AnalysisStatus.CREATED)
        .thenReturn(AnalysisStatus.RUNNING);

    when(timeSeriesAnalysisState.execute()).thenReturn(timeSeriesAnalysisState);
    when(timeSeriesAnalysisState.getStatus()).thenReturn(AnalysisStatus.RUNNING);

    stateMachineService.executeStateMachine(cvConfigId);

    verify(timeSeriesAnalysisState).execute();
    ArgumentCaptor<AnalysisStateMachine> stateMachineArgumentCaptor =
        ArgumentCaptor.forClass(AnalysisStateMachine.class);
    verify(hPersistence).save(stateMachineArgumentCaptor.capture());

    AnalysisStateMachine savedStateMachine = stateMachineArgumentCaptor.getValue();
    assertThat(savedStateMachine.getStatus().name()).isEqualTo(AnalysisStatus.RUNNING.name());
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testExecuteStateMachine_currentlySuccess() {
    when(timeSeriesAnalysisState.getExecutionStatus()).thenReturn(AnalysisStatus.SUCCESS);

    when(timeSeriesAnalysisState.handleSuccess()).thenReturn(timeSeriesAnalysisState);
    when(timeSeriesAnalysisState.getStatus()).thenReturn(AnalysisStatus.SUCCESS);

    stateMachineService.executeStateMachine(cvConfigId);

    verify(timeSeriesAnalysisState).handleSuccess();
    ArgumentCaptor<AnalysisStateMachine> stateMachineArgumentCaptor =
        ArgumentCaptor.forClass(AnalysisStateMachine.class);
    verify(hPersistence).save(stateMachineArgumentCaptor.capture());

    AnalysisStateMachine savedStateMachine = stateMachineArgumentCaptor.getValue();
    assertThat(savedStateMachine.getStatus().name()).isEqualTo(AnalysisStatus.SUCCESS.name());
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testExecuteStateMachine_currentlyTransition() {
    when(timeSeriesAnalysisState.getExecutionStatus()).thenReturn(AnalysisStatus.TRANSITION);

    when(timeSeriesAnalysisState.handleTransition()).thenReturn(timeSeriesAnalysisState);
    when(timeSeriesAnalysisState.getStatus()).thenReturn(AnalysisStatus.CREATED);

    stateMachineService.executeStateMachine(cvConfigId);

    verify(timeSeriesAnalysisState).handleTransition();
    verify(timeSeriesAnalysisState).execute();
    ArgumentCaptor<AnalysisStateMachine> stateMachineArgumentCaptor =
        ArgumentCaptor.forClass(AnalysisStateMachine.class);
    verify(hPersistence).save(stateMachineArgumentCaptor.capture());

    AnalysisStateMachine savedStateMachine = stateMachineArgumentCaptor.getValue();
    assertThat(savedStateMachine.getStatus().name()).isEqualTo(AnalysisStatus.RUNNING.name());
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testExecuteStateMachine_currentlyRetryTransitionedToRunning() {
    when(timeSeriesAnalysisState.getExecutionStatus()).thenReturn(AnalysisStatus.RETRY);

    when(timeSeriesAnalysisState.handleRetry()).thenReturn(timeSeriesAnalysisState);
    when(timeSeriesAnalysisState.getStatus()).thenReturn(AnalysisStatus.RUNNING);

    stateMachineService.executeStateMachine(cvConfigId);

    verify(timeSeriesAnalysisState).handleRetry();
    ArgumentCaptor<AnalysisStateMachine> stateMachineArgumentCaptor =
        ArgumentCaptor.forClass(AnalysisStateMachine.class);
    verify(hPersistence).save(stateMachineArgumentCaptor.capture());

    AnalysisStateMachine savedStateMachine = stateMachineArgumentCaptor.getValue();
    assertThat(savedStateMachine.getStatus().name()).isEqualTo(AnalysisStatus.RUNNING.name());
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testExecuteStateMachine_currentlyRetryTransitionedToFailed() {
    when(timeSeriesAnalysisState.getExecutionStatus()).thenReturn(AnalysisStatus.RETRY);

    when(timeSeriesAnalysisState.handleRetry()).thenReturn(timeSeriesAnalysisState);
    when(timeSeriesAnalysisState.getStatus()).thenReturn(AnalysisStatus.FAILED);

    stateMachineService.executeStateMachine(cvConfigId);

    verify(timeSeriesAnalysisState).handleRetry();
    ArgumentCaptor<AnalysisStateMachine> stateMachineArgumentCaptor =
        ArgumentCaptor.forClass(AnalysisStateMachine.class);
    verify(hPersistence).save(stateMachineArgumentCaptor.capture());

    AnalysisStateMachine savedStateMachine = stateMachineArgumentCaptor.getValue();
    assertThat(savedStateMachine.getStatus().name()).isEqualTo(AnalysisStatus.FAILED.name());
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testExecuteStateMachine_currentlyRetryTransitionedToTimeout() {
    when(timeSeriesAnalysisState.getExecutionStatus()).thenReturn(AnalysisStatus.RETRY);

    when(timeSeriesAnalysisState.handleRetry()).thenReturn(timeSeriesAnalysisState);
    when(timeSeriesAnalysisState.getStatus()).thenReturn(AnalysisStatus.TIMEOUT);

    stateMachineService.executeStateMachine(cvConfigId);

    verify(timeSeriesAnalysisState).handleRetry();
    ArgumentCaptor<AnalysisStateMachine> stateMachineArgumentCaptor =
        ArgumentCaptor.forClass(AnalysisStateMachine.class);
    verify(hPersistence).save(stateMachineArgumentCaptor.capture());

    AnalysisStateMachine savedStateMachine = stateMachineArgumentCaptor.getValue();
    assertThat(savedStateMachine.getStatus().name()).isEqualTo(AnalysisStatus.TIMEOUT.name());
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testRetryStateMachineAfterFailure_notYetTimeToRetry() {
    Instant nextAttemptTime = Instant.now().plus(5, ChronoUnit.MINUTES);
    AnalysisStateMachine stateMachine = buildStateMachine(AnalysisStatus.RETRY);
    stateMachine.setNextAttemptTime(nextAttemptTime.toEpochMilli());
    when(stateMachineQuery.get()).thenReturn(stateMachine);

    stateMachineService.retryStateMachineAfterFailure(stateMachine);

    // verify that nothing changed and the statemachine is still in retry state with same nextAttemptTime
    verify(hPersistence, times(0)).save(any(AnalysisStateMachine.class));
    verify(timeSeriesAnalysisState, times(0)).handleRerun();
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testRetryStateMachineAfterFailure_executeRetry() {
    Instant nextAttemptTime = Instant.now().minus(5, ChronoUnit.MINUTES);
    AnalysisStateMachine stateMachine = buildStateMachine(AnalysisStatus.RETRY);
    stateMachine.setNextAttemptTime(nextAttemptTime.toEpochMilli());
    stateMachine.getCurrentState().setStatus(AnalysisStatus.FAILED);
    when(stateMachineQuery.get()).thenReturn(stateMachine);
    when(timeSeriesAnalysisState.getStatus()).thenReturn(AnalysisStatus.FAILED);
    // test behavior
    stateMachineService.retryStateMachineAfterFailure(stateMachine);

    // verify that nothing changed and the statemachine is still in retry state with same nextAttemptTime
    ArgumentCaptor<AnalysisStateMachine> analysisStateMachineArgumentCaptor =
        ArgumentCaptor.forClass(AnalysisStateMachine.class);
    verify(hPersistence).save(analysisStateMachineArgumentCaptor.capture());
    verify(timeSeriesAnalysisState).handleRerun();
    AnalysisStateMachine stateMachineFromDB = analysisStateMachineArgumentCaptor.getValue();

    assertThat(stateMachineFromDB).isNotNull();
    assertThat(stateMachineFromDB.getStatus().name()).isEqualTo(AnalysisStatus.RUNNING.name());
  }

  private AnalysisStateMachine buildStateMachine(AnalysisStatus status) {
    int analysisMinute = (int) TimeUnit.MILLISECONDS.toMinutes(Instant.now().toEpochMilli());
    AnalysisStateMachine stateMachine = AnalysisStateMachine.builder()
                                            .verificationTaskId(verificationTaskId)
                                            .analysisStartTime(Instant.now().minus(5, ChronoUnit.MINUTES))
                                            .analysisEndTime(Instant.now())
                                            .build();
    stateMachine.setCurrentState(timeSeriesAnalysisState);
    stateMachine.setStatus(status);
    return stateMachine;
  }

  private VerificationJobDTO newCanaryVerificationJobDTO() {
    CanaryVerificationJobDTO canaryVerificationJobDTO = new CanaryVerificationJobDTO();
    canaryVerificationJobDTO.setIdentifier(generateUuid());
    canaryVerificationJobDTO.setJobName(generateUuid());
    canaryVerificationJobDTO.setDataSources(Lists.newArrayList(DataSourceType.SPLUNK));
    canaryVerificationJobDTO.setSensitivity(Sensitivity.MEDIUM.name());
    canaryVerificationJobDTO.setServiceIdentifier("service");
    canaryVerificationJobDTO.setOrgIdentifier(generateUuid());
    canaryVerificationJobDTO.setProjectIdentifier(generateUuid());
    canaryVerificationJobDTO.setEnvIdentifier("env");
    canaryVerificationJobDTO.setSensitivity(Sensitivity.MEDIUM.name());
    canaryVerificationJobDTO.setDuration("15m");
    return canaryVerificationJobDTO;
  }
}
