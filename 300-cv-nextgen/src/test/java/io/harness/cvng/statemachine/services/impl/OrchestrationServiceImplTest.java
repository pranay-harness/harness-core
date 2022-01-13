/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.statemachine.services.impl;

import static io.harness.cvng.CVConstants.STATE_MACHINE_IGNORE_LIMIT;
import static io.harness.cvng.CVConstants.STATE_MACHINE_IGNORE_MINUTES;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.DEEPAK_CHHIKARA;
import static io.harness.rule.OwnerRule.KAMAL;
import static io.harness.rule.OwnerRule.PRAVEEN;
import static io.harness.rule.OwnerRule.SOWMYA;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.CvNextGenTestBase;
import io.harness.category.element.UnitTests;
import io.harness.cvng.BuilderFactory;
import io.harness.cvng.DataGenerator;
import io.harness.cvng.core.entities.AppDynamicsCVConfig;
import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.core.services.api.VerificationTaskService;
import io.harness.cvng.models.VerificationType;
import io.harness.cvng.servicelevelobjective.entities.ServiceLevelIndicator;
import io.harness.cvng.servicelevelobjective.services.api.ServiceLevelIndicatorService;
import io.harness.cvng.statemachine.beans.AnalysisInput;
import io.harness.cvng.statemachine.beans.AnalysisState;
import io.harness.cvng.statemachine.beans.AnalysisStatus;
import io.harness.cvng.statemachine.entities.AnalysisOrchestrator;
import io.harness.cvng.statemachine.entities.AnalysisOrchestrator.AnalysisOrchestratorKeys;
import io.harness.cvng.statemachine.entities.AnalysisStateMachine;
import io.harness.cvng.statemachine.entities.ServiceGuardTimeSeriesAnalysisState;
import io.harness.cvng.statemachine.entities.TimeSeriesAnalysisState;
import io.harness.cvng.statemachine.services.api.AnalysisStateMachineService;
import io.harness.cvng.statemachine.services.api.OrchestrationService;
import io.harness.persistence.HPersistence;
import io.harness.reflection.ReflectionUtils;
import io.harness.rule.Owner;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class OrchestrationServiceImplTest extends CvNextGenTestBase {
  @Inject HPersistence hPersistence;
  @Inject AnalysisStateMachineService analysisStateMachineService;
  @Inject private VerificationTaskService verificationTaskService;
  @Inject OrchestrationService orchestrationService;
  @Inject private Clock clock;
  @Inject private ServiceLevelIndicatorService serviceLevelIndicatorService;
  private BuilderFactory builderFactory;
  private String cvConfigId;
  private String verificationTaskId;
  private String accountId;
  private DataGenerator dataGenerator;
  private TimeSeriesAnalysisState timeSeriesAnalysisState;

  @Before
  public void setup() throws Exception {
    builderFactory = BuilderFactory.getDefault();
    cvConfigId = generateUuid();
    accountId = generateUuid();
    CVConfig cvConfig = new AppDynamicsCVConfig();
    cvConfig.setVerificationType(VerificationType.TIME_SERIES);
    cvConfig.setUuid(cvConfigId);
    hPersistence.save(cvConfig);
    dataGenerator = DataGenerator.builder().accountId(accountId).build();
    verificationTaskId =
        verificationTaskService.createLiveMonitoringVerificationTask(accountId, cvConfigId, cvConfig.getType());
    timeSeriesAnalysisState = ServiceGuardTimeSeriesAnalysisState.builder().build();
    timeSeriesAnalysisState.setStatus(AnalysisStatus.CREATED);
    timeSeriesAnalysisState.setInputs(AnalysisInput.builder()
                                          .verificationTaskId(verificationTaskId)
                                          .startTime(clock.instant().minus(Duration.ofMinutes(5)))
                                          .endTime(clock.instant())
                                          .build());
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testQueueAnalysis_firstEverOrchestration() {
    AnalysisOrchestrator orchestrator = hPersistence.createQuery(AnalysisOrchestrator.class)
                                            .filter(AnalysisOrchestratorKeys.verificationTaskId, cvConfigId)
                                            .get();

    assertThat(orchestrator).isNull();
    orchestrationService.queueAnalysis(cvConfigId, clock.instant(), clock.instant().minus(5, ChronoUnit.MINUTES));

    orchestrator = hPersistence.createQuery(AnalysisOrchestrator.class)
                       .filter(AnalysisOrchestratorKeys.verificationTaskId, verificationTaskId)
                       .get();

    assertThat(orchestrator).isNotNull();
    assertThat(orchestrator.getUuid()).isNotNull();
    assertThat(orchestrator.getStatus().name()).isEqualTo(AnalysisStatus.CREATED.name());
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testQueueAnalysis_multiple() {
    for (Instant startTime = clock.instant(); startTime.isBefore(clock.instant().plus(Duration.ofMinutes(30)));
         startTime = startTime.plus(Duration.ofMinutes(5))) {
      orchestrationService.queueAnalysis(verificationTaskId, startTime, startTime.plus(5, ChronoUnit.MINUTES));
    }
    List<AnalysisOrchestrator> orchestrator =
        hPersistence.createQuery(AnalysisOrchestrator.class)
            .filter(AnalysisOrchestratorKeys.verificationTaskId, verificationTaskId)
            .asList();
    assertThat(orchestrator).hasSize(1);
    assertThat(orchestrator.get(0).getStatus().name()).isEqualTo(AnalysisStatus.CREATED.name());
    assertThat(orchestrator.get(0).getAnalysisStateMachineQueue()).hasSize(6);
    orchestrationService.orchestrate(orchestrator.get(0));
    for (Instant startTime = clock.instant(); startTime.isBefore(clock.instant().plus(Duration.ofMinutes(15)));
         startTime = startTime.plus(Duration.ofMinutes(5))) {
      orchestrationService.queueAnalysis(verificationTaskId, startTime, startTime.plus(5, ChronoUnit.MINUTES));
    }
    orchestrator = hPersistence.createQuery(AnalysisOrchestrator.class)
                       .filter(AnalysisOrchestratorKeys.verificationTaskId, verificationTaskId)
                       .asList();
    assertThat(orchestrator).hasSize(1);
    assertThat(orchestrator.get(0).getStatus().name()).isEqualTo(AnalysisStatus.RUNNING.name());
    assertThat(orchestrator.get(0).getAnalysisStateMachineQueue()).hasSize(8);
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testQueueAnalysis_firstEverOrchestrationInvalidInputs() {
    AnalysisOrchestrator orchestrator = hPersistence.createQuery(AnalysisOrchestrator.class)
                                            .filter(AnalysisOrchestratorKeys.verificationTaskId, cvConfigId)
                                            .get();

    assertThat(orchestrator).isNull();

    assertThatThrownBy(() -> orchestrationService.queueAnalysis(cvConfigId, clock.instant(), null))
        .isInstanceOf(NullPointerException.class);
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
    AnalysisStateMachine stateMachine =
        dataGenerator.buildStateMachine(AnalysisStatus.SUCCESS, verificationTaskId, timeSeriesAnalysisState);
    hPersistence.save(stateMachine);
    orchestrate(verificationTaskId);

    AnalysisStateMachine savedStateMachine = hPersistence.createQuery(AnalysisStateMachine.class).get();
    assertThat(savedStateMachine).isNotNull();
    assertThat(savedStateMachine.getStatus()).isEqualTo(AnalysisStatus.SUCCESS);
    AnalysisOrchestrator savedOrchestrator = hPersistence.createQuery(AnalysisOrchestrator.class).get();
    assertThat(savedOrchestrator).isNotNull();
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

    hPersistence.save(orchestrator);
    AnalysisStateMachine stateMachine =
        dataGenerator.buildStateMachine(AnalysisStatus.SUCCESS, verificationTaskId, timeSeriesAnalysisState);
    hPersistence.save(stateMachine);

    AnalysisStateMachine nextStateMachine =
        dataGenerator.buildStateMachine(AnalysisStatus.CREATED, verificationTaskId, timeSeriesAnalysisState);
    orchestrator.getAnalysisStateMachineQueue().add(nextStateMachine);
    hPersistence.save(orchestrator);

    orchestrate(verificationTaskId);

    List<AnalysisStateMachine> savedStateMachines = hPersistence.createQuery(AnalysisStateMachine.class).asList();
    assertThat(savedStateMachines.size()).isEqualTo(2);
    assertThat(savedStateMachines.get(0).getStatus()).isEqualTo(AnalysisStatus.SUCCESS);
    assertThat(savedStateMachines.get(1).getStatus()).isEqualTo(AnalysisStatus.RUNNING);
    AnalysisOrchestrator savedOrchestrator = hPersistence.createQuery(AnalysisOrchestrator.class).get();
    assertThat(savedOrchestrator).isNotNull();
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testOrchestrate_currentStateMachineRunningWithCompletedTasks() {
    AnalysisOrchestrator orchestrator = AnalysisOrchestrator.builder()
                                            .verificationTaskId(cvConfigId)
                                            .status(AnalysisStatus.RUNNING)
                                            .analysisStateMachineQueue(new ArrayList<>())
                                            .build();

    hPersistence.save(orchestrator);
    timeSeriesAnalysisState.setStatus(AnalysisStatus.SUCCESS);
    AnalysisStateMachine stateMachine =
        dataGenerator.buildStateMachine(AnalysisStatus.RUNNING, verificationTaskId, timeSeriesAnalysisState);
    hPersistence.save(stateMachine);

    AnalysisStateMachine nextStateMachine =
        dataGenerator.buildStateMachine(AnalysisStatus.CREATED, verificationTaskId, timeSeriesAnalysisState);
    orchestrator.getAnalysisStateMachineQueue().add(nextStateMachine);
    hPersistence.save(orchestrator);

    orchestrate(verificationTaskId);

    List<AnalysisStateMachine> savedStateMachines = hPersistence.createQuery(AnalysisStateMachine.class).asList();
    assertThat(savedStateMachines.size()).isEqualTo(2);
    assertThat(savedStateMachines.get(0).getStatus()).isEqualTo(AnalysisStatus.SUCCESS);
    assertThat(savedStateMachines.get(1).getStatus()).isEqualTo(AnalysisStatus.RUNNING);
    AnalysisOrchestrator savedOrchestrator = hPersistence.createQuery(AnalysisOrchestrator.class).get();
    assertThat(savedOrchestrator).isNotNull();
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

    AnalysisStateMachine stateMachine =
        dataGenerator.buildStateMachine(AnalysisStatus.RUNNING, verificationTaskId, timeSeriesAnalysisState);
    analysisStateMachineService.initiateStateMachine(verificationTaskId, stateMachine);
    orchestrate(verificationTaskId);

    AnalysisStateMachine savedStateMachine = hPersistence.createQuery(AnalysisStateMachine.class).get();
    assertThat(savedStateMachine).isNotNull();
    assertThat(savedStateMachine.getStatus()).isEqualByComparingTo(AnalysisStatus.RUNNING);
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testOrchestrate_failed() {
    AnalysisOrchestrator orchestrator =
        AnalysisOrchestrator.builder().verificationTaskId(verificationTaskId).status(AnalysisStatus.RUNNING).build();
    hPersistence.save(orchestrator);

    AnalysisStateMachine stateMachine =
        dataGenerator.buildStateMachine(AnalysisStatus.FAILED, verificationTaskId, timeSeriesAnalysisState);
    analysisStateMachineService.initiateStateMachine(verificationTaskId, stateMachine);
    stateMachine.setStatus(AnalysisStatus.FAILED);
    stateMachine.getCurrentState().setStatus(AnalysisStatus.FAILED);
    hPersistence.save(stateMachine);
    orchestrate(verificationTaskId);
    AnalysisStateMachine savedStateMachine = hPersistence.createQuery(AnalysisStateMachine.class).get();
    assertThat(savedStateMachine).isNotNull();
    assertThat(savedStateMachine.getStatus()).isEqualByComparingTo(AnalysisStatus.RUNNING);
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

    AnalysisStateMachine stateMachine =
        dataGenerator.buildStateMachine(AnalysisStatus.CREATED, verificationTaskId, timeSeriesAnalysisState);
    analysisStateMachineService.initiateStateMachine(verificationTaskId, stateMachine);
    stateMachine.setStatus(AnalysisStatus.TIMEOUT);
    stateMachine.getCurrentState().setStatus(AnalysisStatus.TIMEOUT);
    hPersistence.save(stateMachine);

    orchestrate(verificationTaskId);

    AnalysisStateMachine savedStateMachine = hPersistence.createQuery(AnalysisStateMachine.class).get();
    assertThat(savedStateMachine).isNotNull();
    assertThat(savedStateMachine.getStatus()).isEqualByComparingTo(AnalysisStatus.RUNNING);
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
        dataGenerator.buildStateMachine(AnalysisStatus.SUCCESS, verificationTaskId, timeSeriesAnalysisState);
    hPersistence.save(stateMachine);

    AnalysisStateMachine firstSM =
        dataGenerator.buildStateMachine(AnalysisStatus.CREATED, verificationTaskId, timeSeriesAnalysisState);
    firstSM.setAnalysisStartTime(clock.instant().minus(STATE_MACHINE_IGNORE_MINUTES + 20, ChronoUnit.MINUTES));
    firstSM.setAnalysisEndTime(clock.instant().minus(STATE_MACHINE_IGNORE_MINUTES + 10, ChronoUnit.MINUTES));
    orchestrator.getAnalysisStateMachineQueue().add(firstSM);

    AnalysisStateMachine nextSM =
        dataGenerator.buildStateMachine(AnalysisStatus.CREATED, verificationTaskId, timeSeriesAnalysisState);
    orchestrator.getAnalysisStateMachineQueue().add(nextSM);
    hPersistence.save(orchestrator);

    orchestrate(verificationTaskId);

    List<AnalysisStateMachine> savedStateMachines = hPersistence.createQuery(AnalysisStateMachine.class).asList();
    assertThat(savedStateMachines.size()).isEqualTo(3);
    assertThat(savedStateMachines.get(0).getStatus()).isEqualTo(AnalysisStatus.SUCCESS);
    assertThat(savedStateMachines.get(1).getStatus()).isEqualTo(AnalysisStatus.IGNORED);
    assertThat(savedStateMachines.get(2).getStatus()).isEqualTo(AnalysisStatus.RUNNING);
    AnalysisOrchestrator savedOrchestrator = hPersistence.createQuery(AnalysisOrchestrator.class).get();
    assertThat(savedOrchestrator).isNotNull();
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
        dataGenerator.buildStateMachine(AnalysisStatus.SUCCESS, verificationTaskId, timeSeriesAnalysisState);
    hPersistence.save(stateMachine);

    List<AnalysisStateMachine> ignoreList = new ArrayList<>();

    for (int i = 0; i < STATE_MACHINE_IGNORE_LIMIT + 10; i++) {
      AnalysisStateMachine firstSM =
          dataGenerator.buildStateMachine(AnalysisStatus.CREATED, verificationTaskId, timeSeriesAnalysisState);
      firstSM.setAnalysisStartTime(clock.instant().minus(STATE_MACHINE_IGNORE_MINUTES + 20, ChronoUnit.MINUTES));
      firstSM.setAnalysisEndTime(clock.instant().minus(STATE_MACHINE_IGNORE_MINUTES + 10, ChronoUnit.MINUTES));
      orchestrator.getAnalysisStateMachineQueue().add(firstSM);
      if (i < STATE_MACHINE_IGNORE_LIMIT) {
        ignoreList.add(firstSM);
      }
    }
    hPersistence.save(orchestrator);

    orchestrate(verificationTaskId);
    AnalysisOrchestrator orchestratorFromDB = hPersistence.createQuery(AnalysisOrchestrator.class).get();
    assertThat(orchestratorFromDB.getAnalysisStateMachineQueue().size()).isEqualTo(10);
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testOrchestrate_ignoreStateMachinesAtCreatedState() {
    AnalysisOrchestrator orchestrator = AnalysisOrchestrator.builder()
                                            .verificationTaskId(cvConfigId)
                                            .status(AnalysisStatus.CREATED)
                                            .analysisStateMachineQueue(new ArrayList<>())
                                            .build();

    AnalysisStateMachine stateMachine =
        dataGenerator.buildStateMachine(AnalysisStatus.SUCCESS, verificationTaskId, timeSeriesAnalysisState);
    hPersistence.save(stateMachine);

    List<AnalysisStateMachine> ignoreList = new ArrayList<>();

    for (int i = 0; i < STATE_MACHINE_IGNORE_LIMIT - 10; i++) {
      AnalysisStateMachine firstSM =
          dataGenerator.buildStateMachine(AnalysisStatus.CREATED, verificationTaskId, timeSeriesAnalysisState);
      firstSM.setAnalysisStartTime(clock.instant().minus(STATE_MACHINE_IGNORE_MINUTES + 20, ChronoUnit.MINUTES));
      firstSM.setAnalysisEndTime(clock.instant().minus(STATE_MACHINE_IGNORE_MINUTES + 10, ChronoUnit.MINUTES));
      orchestrator.getAnalysisStateMachineQueue().add(firstSM);
      if (i < STATE_MACHINE_IGNORE_LIMIT) {
        ignoreList.add(firstSM);
      }
    }
    hPersistence.save(orchestrator);

    orchestrate(cvConfigId);
    AnalysisOrchestrator orchestratorFromDB = hPersistence.createQuery(AnalysisOrchestrator.class).get();
    assertThat(orchestratorFromDB.getAnalysisStateMachineQueue()).isEmpty();
    assertThat(orchestratorFromDB.getStatus().name()).isEqualTo(AnalysisStatus.RUNNING.name());
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testValidateAllFieldsOnUpsert() {
    AnalysisOrchestrator orchestrator = hPersistence.createQuery(AnalysisOrchestrator.class)
                                            .filter(AnalysisOrchestratorKeys.verificationTaskId, cvConfigId)
                                            .get();

    assertThat(orchestrator).isNull();

    orchestrationService.queueAnalysis(cvConfigId, clock.instant(), clock.instant().minus(5, ChronoUnit.MINUTES));
    AnalysisOrchestrator dbOrchestrator = hPersistence.createQuery(AnalysisOrchestrator.class)
                                              .filter(AnalysisOrchestratorKeys.verificationTaskId, cvConfigId)
                                              .get();

    Set<String> nullableFields = Sets.newHashSet();
    nullableFields.add(AnalysisOrchestratorKeys.analysisOrchestrationIteration);

    List<Field> fields = ReflectionUtils.getAllDeclaredAndInheritedFields(AnalysisOrchestrator.class);
    fields.stream().filter(field -> !nullableFields.contains(field.getName())).forEach(field -> {
      try {
        field.setAccessible(true);
        assertThat(field.get(dbOrchestrator)).withFailMessage("field %s is null", field.getName()).isNotNull();
      } catch (IllegalAccessException e) {
        throw new RuntimeException(e);
      }
    });
  }

  @Test
  @Owner(developers = DEEPAK_CHHIKARA)
  @Category(UnitTests.class)
  public void testQueueAnalysis_forSLIVerificationTask() {
    List<String> serviceLevelIndicatorIdentifiers =
        serviceLevelIndicatorService.create(builderFactory.getProjectParams(),
            Collections.singletonList(builderFactory.getServiceLevelIndicatorDTOBuilder()), generateUuid(),
            generateUuid(), generateUuid());
    ServiceLevelIndicator serviceLevelIndicator = serviceLevelIndicatorService.getServiceLevelIndicator(
        builderFactory.getProjectParams(), serviceLevelIndicatorIdentifiers.get(0));
    String sliId = serviceLevelIndicator.getUuid();

    AnalysisOrchestrator orchestrator = hPersistence.createQuery(AnalysisOrchestrator.class)
                                            .filter(AnalysisOrchestratorKeys.verificationTaskId, sliId)
                                            .get();

    assertThat(orchestrator).isNull();
    orchestrationService.queueAnalysis(sliId, clock.instant(), clock.instant().minus(5, ChronoUnit.MINUTES));

    orchestrator = hPersistence.createQuery(AnalysisOrchestrator.class)
                       .filter(AnalysisOrchestratorKeys.verificationTaskId, sliId)
                       .get();

    assertThat(orchestrator).isNotNull();
    assertThat(orchestrator.getUuid()).isNotNull();
    assertThat(orchestrator.getStatus().name()).isEqualTo(AnalysisStatus.CREATED.name());
    assertThat(orchestrator.getAnalysisStateMachineQueue().get(0).getCurrentState().getType())
        .isEqualTo(AnalysisState.StateType.SLI_METRIC_ANALYSIS);
  }

  private AnalysisOrchestrator getOrchestrator(String verificationTaskId) {
    return hPersistence.createQuery(AnalysisOrchestrator.class)
        .filter(AnalysisOrchestratorKeys.verificationTaskId, verificationTaskId)
        .get();
  }
  public void orchestrate(String verificationTaskId) {
    AnalysisOrchestrator orchestrator = getOrchestrator(verificationTaskId);
    orchestrationService.orchestrate(orchestrator);
  }
}
