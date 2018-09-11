package software.wings.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;
import static org.mockito.internal.util.reflection.Whitebox.setInternalState;
import static software.wings.api.PhaseElement.PhaseElementBuilder.aPhaseElement;
import static software.wings.api.ServiceElement.Builder.aServiceElement;
import static software.wings.beans.Environment.Builder.anEnvironment;
import static software.wings.beans.WorkflowExecution.WorkflowExecutionBuilder.aWorkflowExecution;
import static software.wings.sm.StateExecutionInstance.Builder.aStateExecutionInstance;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import io.harness.eraro.ErrorCode;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.WingsBaseTest;
import software.wings.beans.PipelineExecution;
import software.wings.beans.PipelineStageExecution;
import software.wings.beans.User;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.WorkflowType;
import software.wings.beans.baseline.WorkflowExecutionBaseline;
import software.wings.common.Constants;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.security.UserThreadLocal;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.StateMachineExecutor;
import software.wings.sm.StateType;
import software.wings.sm.WorkflowStandardParams;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by rsingh on 2/16/18.
 */
public class WorkflowExecutionBaselineServiceTest extends WingsBaseTest {
  private static final Logger logger = LoggerFactory.getLogger(WorkflowExecutionBaselineServiceTest.class);

  @Inject private WingsPersistence wingsPersistence;
  @Inject private WorkflowExecutionService workflowExecutionService;

  private String appId;
  private String workflowId;
  private final String userEmail = "rsingh@harness.io";
  private final String userName = "raghu";
  private final User user = User.Builder.anUser().withEmail(userEmail).withName(userName).build();

  @Before
  public void setUp() {
    appId = UUID.randomUUID().toString();
    workflowId = UUID.randomUUID().toString();
    wingsPersistence.save(user);
    UserThreadLocal.set(user);
  }

  @Test()
  public void testNoWorkflowExecution() {
    String workflowExecutionId = UUID.randomUUID().toString();
    try {
      workflowExecutionService.markBaseline(appId, workflowExecutionId, true);
      fail("Did not fail for invalid workflow execution id");
    } catch (WingsException e) {
      assertEquals(ErrorCode.BASELINE_CONFIGURATION_ERROR, e.getCode());
      assertEquals("No workflow execution found with id: " + workflowExecutionId + " appId: " + appId, e.getMessage());
    }
  }

  @Test
  public void testNoPipelineExecutionsForPipeline() {
    WorkflowExecution workflowExecution =
        aWorkflowExecution().withWorkflowId(workflowId).withWorkflowType(WorkflowType.PIPELINE).build();
    workflowExecution.setAppId(appId);
    String workflowExecutionId = wingsPersistence.save(workflowExecution);
    try {
      workflowExecutionService.markBaseline(appId, workflowExecutionId, true);
    } catch (WingsException e) {
      assertEquals(ErrorCode.BASELINE_CONFIGURATION_ERROR, e.getCode());
      assertEquals("Pipeline has not been executed.", e.getMessage());
    }
  }

  @Test
  public void testNoWorkflowExecutionsForPipeline() {
    WorkflowExecution workflowExecution =
        aWorkflowExecution().withWorkflowId(workflowId).withWorkflowType(WorkflowType.PIPELINE).build();
    workflowExecution.setAppId(appId);
    PipelineExecution pipelineExecution = PipelineExecution.Builder.aPipelineExecution().build();
    workflowExecution.setPipelineExecution(pipelineExecution);
    String workflowExecutionId = wingsPersistence.save(workflowExecution);
    try {
      workflowExecutionService.markBaseline(appId, workflowExecutionId, true);
    } catch (WingsException e) {
      assertEquals(ErrorCode.BASELINE_CONFIGURATION_ERROR, e.getCode());
      assertEquals("No workflows have been executed for this pipeline.", e.getMessage());
    }
  }

  @Test
  public void testNoVerificationSteps() {
    WorkflowExecution workflowExecution =
        aWorkflowExecution().withWorkflowId(workflowId).withWorkflowType(WorkflowType.PIPELINE).build();
    workflowExecution.setAppId(appId);
    PipelineStageExecution pipelineStageExecution = PipelineStageExecution.Builder.aPipelineStageExecution().build();
    PipelineExecution pipelineExecution = PipelineExecution.Builder.aPipelineExecution()
                                              .withPipelineStageExecutions(Lists.newArrayList(pipelineStageExecution))
                                              .build();
    workflowExecution.setPipelineExecution(pipelineExecution);
    String workflowExecutionId = wingsPersistence.save(workflowExecution);
    try {
      workflowExecutionService.markBaseline(appId, workflowExecutionId, true);
    } catch (WingsException e) {
      assertEquals(ErrorCode.BASELINE_CONFIGURATION_ERROR, e.getCode());
      assertEquals("Either there is no workflow execution with verification steps "
              + "or verification steps haven't been executed for the workflow.",
          e.getMessage());
    }
  }

  @Test
  public void testMarkAndCreateBaselinesForPipeline() {
    int numOfWorkflowExecutions = 10;
    List<String> envIds = new ArrayList<>();
    List<String> serviceIds = new ArrayList<>();
    List<String> workflowExecutionIds = new ArrayList<>();
    List<WorkflowExecution> workflowExecutions = new ArrayList<>();
    String pipelineExecutionId = UUID.randomUUID().toString();

    for (int i = 0; i < numOfWorkflowExecutions; i++) {
      String envId = UUID.randomUUID().toString();
      envIds.add(envId);
      String serviceId = UUID.randomUUID().toString();
      serviceIds.add(serviceId);

      WorkflowExecution workflowExecution = aWorkflowExecution()
                                                .withWorkflowId(workflowId)
                                                .withEnvId(envId)
                                                .withServiceIds(Lists.newArrayList(serviceId))
                                                .withPipelineExecutionId(pipelineExecutionId)
                                                .build();
      workflowExecution.setAppId(appId);

      String workflowExecutionId = wingsPersistence.save(workflowExecution);
      workflowExecutionIds.add(workflowExecutionId);
      workflowExecutions.add(workflowExecution);
      StateExecutionInstance stateExecutionInstance = aStateExecutionInstance()
                                                          .withExecutionUuid(workflowExecutionId)
                                                          .withStateType(StateType.DYNA_TRACE.name())
                                                          .build();
      stateExecutionInstance.setAppId(appId);
      wingsPersistence.save(stateExecutionInstance);
    }

    for (String workflowExecutionId : workflowExecutionIds) {
      WorkflowExecution workflowExecution = wingsPersistence.get(WorkflowExecution.class, appId, workflowExecutionId);
      assertFalse(workflowExecution.isBaseline());
    }

    WorkflowExecution workflowExecution = aWorkflowExecution()
                                              .withWorkflowId(workflowId)
                                              .withWorkflowType(WorkflowType.PIPELINE)
                                              .withUuid(pipelineExecutionId)
                                              .build();
    workflowExecution.setAppId(appId);
    PipelineStageExecution pipelineStageExecution =
        PipelineStageExecution.Builder.aPipelineStageExecution().withWorkflowExecutions(workflowExecutions).build();
    PipelineExecution pipelineExecution = PipelineExecution.Builder.aPipelineExecution()
                                              .withPipelineStageExecutions(Lists.newArrayList(pipelineStageExecution))
                                              .build();
    workflowExecution.setPipelineExecution(pipelineExecution);
    String workflowExecutionId = wingsPersistence.save(workflowExecution);
    Set<WorkflowExecutionBaseline> workflowExecutionBaselines =
        workflowExecutionService.markBaseline(appId, workflowExecutionId, true);
    assertEquals(numOfWorkflowExecutions, workflowExecutionBaselines.size());
    assertEquals(numOfWorkflowExecutions, wingsPersistence.createQuery(WorkflowExecutionBaseline.class).count());

    for (WorkflowExecutionBaseline baseline : workflowExecutionBaselines) {
      assertEquals(appId, baseline.getAppId());
      assertEquals(workflowId, baseline.getWorkflowId());
      assertEquals(workflowExecutionId, baseline.getPipelineExecutionId());
      assertTrue(envIds.contains(baseline.getEnvId()));
      assertTrue(serviceIds.contains(baseline.getServiceId()));
      assertTrue(workflowExecutionIds.contains(baseline.getWorkflowExecutionId()));
    }

    for (String executionId : workflowExecutionIds) {
      WorkflowExecution workflowExecution1 = wingsPersistence.get(WorkflowExecution.class, appId, executionId);
      assertTrue(workflowExecution1.isBaseline());
    }

    WorkflowExecution savedPipelineExecution = wingsPersistence.get(WorkflowExecution.class, workflowExecutionId);
    pipelineExecution = savedPipelineExecution.getPipelineExecution();
    List<PipelineStageExecution> pipelineStageExecutions = pipelineExecution.getPipelineStageExecutions();
    pipelineStageExecutions.forEach(
        stageExecution -> stageExecution.getWorkflowExecutions().forEach(pipelineWorkflowExecution -> {
          assertTrue(pipelineWorkflowExecution.isBaseline());
        }));

    // unmark the baseline and verify
    workflowExecutionBaselines = workflowExecutionService.markBaseline(appId, workflowExecutionId, false);
    assertEquals(numOfWorkflowExecutions, workflowExecutionBaselines.size());
    assertThat(wingsPersistence.createQuery(WorkflowExecutionBaseline.class).count()).isEqualTo(0);
    savedPipelineExecution = wingsPersistence.get(WorkflowExecution.class, workflowExecutionId);

    pipelineExecution = savedPipelineExecution.getPipelineExecution();
    pipelineStageExecutions = pipelineExecution.getPipelineStageExecutions();
    pipelineStageExecutions.forEach(
        stageExecution -> stageExecution.getWorkflowExecutions().forEach(pipelineWorkflowExecution -> {
          assertFalse(pipelineWorkflowExecution.isBaseline());
        }));

    // mark again and verify
    workflowExecutionBaselines = workflowExecutionService.markBaseline(appId, workflowExecutionId, true);
    assertEquals(numOfWorkflowExecutions, workflowExecutionBaselines.size());
    assertEquals(numOfWorkflowExecutions, wingsPersistence.createQuery(WorkflowExecutionBaseline.class).count());

    for (WorkflowExecutionBaseline baseline : workflowExecutionBaselines) {
      assertEquals(appId, baseline.getAppId());
      assertEquals(workflowId, baseline.getWorkflowId());
      assertEquals(workflowExecutionId, baseline.getPipelineExecutionId());
      assertTrue(envIds.contains(baseline.getEnvId()));
      assertTrue(serviceIds.contains(baseline.getServiceId()));
      assertTrue(workflowExecutionIds.contains(baseline.getWorkflowExecutionId()));
    }

    for (String executionId : workflowExecutionIds) {
      WorkflowExecution workflowExecution1 = wingsPersistence.get(WorkflowExecution.class, appId, executionId);
      assertTrue(workflowExecution1.isBaseline());
    }

    savedPipelineExecution = wingsPersistence.get(WorkflowExecution.class, workflowExecutionId);
    pipelineExecution = savedPipelineExecution.getPipelineExecution();
    pipelineStageExecutions = pipelineExecution.getPipelineStageExecutions();
    pipelineStageExecutions.forEach(
        stageExecution -> stageExecution.getWorkflowExecutions().forEach(pipelineWorkflowExecution -> {
          assertTrue(pipelineWorkflowExecution.isBaseline());
        }));
  }

  @Test
  public void testMarkBaselinesUpdateForPipeline() {
    int numOfWorkflowExecutions = 10;
    int numOfPipelines = 5;
    List<String> envIds = new ArrayList<>();
    List<String> serviceIds = new ArrayList<>();
    List<List<String>> workflowExecutionIds = new ArrayList<>();
    List<String> pipelineIds = new ArrayList<>();

    for (int n = 0; n < numOfWorkflowExecutions; n++) {
      envIds.add(UUID.randomUUID().toString());
      serviceIds.add(UUID.randomUUID().toString());
    }

    String pipeLineExecId = "";
    for (int i = 0; i < numOfPipelines; i++) {
      pipeLineExecId = UUID.randomUUID().toString();
      logger.info("running for pipeline " + i);
      workflowExecutionIds.add(new ArrayList<>());
      List<WorkflowExecution> workflowExecutions = new ArrayList<>();
      for (int j = 0; j < numOfWorkflowExecutions; j++) {
        WorkflowExecution workflowExecution = aWorkflowExecution()
                                                  .withWorkflowId(workflowId)
                                                  .withEnvId(envIds.get(j))
                                                  .withServiceIds(Lists.newArrayList(serviceIds.get(j)))
                                                  .withUuid("workflowExecution-" + i + "-" + j)
                                                  .withPipelineExecutionId(pipeLineExecId)
                                                  .build();
        workflowExecution.setAppId(appId);

        String workflowExecutionId = wingsPersistence.save(workflowExecution);
        workflowExecutionIds.get(i).add(workflowExecutionId);
        workflowExecutions.add(workflowExecution);
        StateExecutionInstance stateExecutionInstance = aStateExecutionInstance()
                                                            .withExecutionUuid(workflowExecutionId)
                                                            .withStateType(StateType.DYNA_TRACE.name())
                                                            .build();
        stateExecutionInstance.setAppId(appId);
        wingsPersistence.save(stateExecutionInstance);
      }

      for (String workflowExecutionId : workflowExecutionIds.get(i)) {
        WorkflowExecution workflowExecution = wingsPersistence.get(WorkflowExecution.class, appId, workflowExecutionId);
        assertFalse(workflowExecution.isBaseline());
      }

      WorkflowExecution workflowExecution = aWorkflowExecution()
                                                .withWorkflowId(workflowId)
                                                .withWorkflowType(WorkflowType.PIPELINE)
                                                .withUuid(pipeLineExecId)
                                                .build();
      workflowExecution.setAppId(appId);
      PipelineStageExecution pipelineStageExecution =
          PipelineStageExecution.Builder.aPipelineStageExecution().withWorkflowExecutions(workflowExecutions).build();
      PipelineExecution pipelineExecution = PipelineExecution.Builder.aPipelineExecution()
                                                .withPipelineStageExecutions(Lists.newArrayList(pipelineStageExecution))
                                                .build();
      workflowExecution.setPipelineExecution(pipelineExecution);
      wingsPersistence.save(workflowExecution);
      pipelineIds.add(pipeLineExecId);

      Set<WorkflowExecutionBaseline> workflowExecutionBaselines =
          workflowExecutionService.markBaseline(appId, pipeLineExecId, true);
      assertEquals(numOfWorkflowExecutions, workflowExecutionBaselines.size());
      assertEquals(numOfWorkflowExecutions, wingsPersistence.createQuery(WorkflowExecutionBaseline.class).count());

      for (WorkflowExecutionBaseline baseline : workflowExecutionBaselines) {
        assertEquals(appId, baseline.getAppId());
        assertEquals(workflowId, baseline.getWorkflowId());
        assertTrue(envIds.contains(baseline.getEnvId()));
        assertTrue(serviceIds.contains(baseline.getServiceId()));
        assertTrue("failed for " + baseline, workflowExecutionIds.get(i).contains(baseline.getWorkflowExecutionId()));
      }

      for (int pipeline = 0; pipeline <= i; pipeline++) {
        WorkflowExecution pipelineExec =
            wingsPersistence.get(WorkflowExecution.class, appId, pipelineIds.get(pipeline));
        if (pipeline == i) {
          assertTrue("failing for loop " + pipeline + " i: " + i, pipelineExec.isBaseline());
        } else {
          assertFalse("failing for loop " + pipeline + " i: " + i, pipelineExec.isBaseline());
        }

        for (String executionId : workflowExecutionIds.get(pipeline)) {
          WorkflowExecution workflowExecution1 = wingsPersistence.get(WorkflowExecution.class, appId, executionId);

          if (pipeline == i) {
            assertTrue(workflowExecution1.isBaseline());
          } else {
            assertFalse(workflowExecution1.isBaseline());
          }
        }
      }

      // unmark and verify
      workflowExecutionBaselines = workflowExecutionService.markBaseline(appId, pipeLineExecId, false);
      assertEquals(numOfWorkflowExecutions, workflowExecutionBaselines.size());
      assertEquals(0, wingsPersistence.createQuery(WorkflowExecutionBaseline.class).count());

      for (WorkflowExecutionBaseline baseline : workflowExecutionBaselines) {
        assertEquals(appId, baseline.getAppId());
        assertEquals(workflowId, baseline.getWorkflowId());
        assertTrue(envIds.contains(baseline.getEnvId()));
        assertTrue(serviceIds.contains(baseline.getServiceId()));
      }

      for (String executionId : workflowExecutionIds.get(i)) {
        WorkflowExecution workflowExecution1 = wingsPersistence.get(WorkflowExecution.class, appId, executionId);
        assertFalse(workflowExecution1.isBaseline());
      }

      WorkflowExecution savedPipelineExecution = wingsPersistence.get(WorkflowExecution.class, pipeLineExecId);
      pipelineExecution = savedPipelineExecution.getPipelineExecution();
      List<PipelineStageExecution> pipelineStageExecutions = pipelineExecution.getPipelineStageExecutions();
      pipelineStageExecutions.forEach(
          stageExecution -> stageExecution.getWorkflowExecutions().forEach(pipelineWorkflowExecution -> {
            assertFalse(pipelineWorkflowExecution.isBaseline());
          }));

      // mark again and verify
      workflowExecutionBaselines = workflowExecutionService.markBaseline(appId, pipeLineExecId, true);
      assertEquals(numOfWorkflowExecutions, workflowExecutionBaselines.size());
      assertEquals(numOfWorkflowExecutions, wingsPersistence.createQuery(WorkflowExecutionBaseline.class).count());

      for (WorkflowExecutionBaseline baseline : workflowExecutionBaselines) {
        assertEquals(appId, baseline.getAppId());
        assertEquals(workflowId, baseline.getWorkflowId());
        assertTrue(envIds.contains(baseline.getEnvId()));
        assertTrue(serviceIds.contains(baseline.getServiceId()));
      }

      for (String executionId : workflowExecutionIds.get(i)) {
        WorkflowExecution workflowExecution1 = wingsPersistence.get(WorkflowExecution.class, appId, executionId);
        assertTrue(workflowExecution1.isBaseline());
      }

      savedPipelineExecution = wingsPersistence.get(WorkflowExecution.class, pipeLineExecId);
      pipelineExecution = savedPipelineExecution.getPipelineExecution();
      pipelineStageExecutions = pipelineExecution.getPipelineStageExecutions();
      pipelineStageExecutions.forEach(
          stageExecution -> stageExecution.getWorkflowExecutions().forEach(pipelineWorkflowExecution -> {
            assertTrue(pipelineWorkflowExecution.isBaseline());
          }));
    }
  }

  @Test
  public void testMarkAndCreateBaselinesForWorkflow() {
    int numOfWorkflowExecutions = 10;
    List<String> workflowExecutionIds = new ArrayList<>();
    List<WorkflowExecution> workflowExecutions = new ArrayList<>();
    String envId = UUID.randomUUID().toString();
    String serviceId = UUID.randomUUID().toString();

    for (int i = 0; i < numOfWorkflowExecutions; i++) {
      WorkflowExecution workflowExecution = aWorkflowExecution()
                                                .withWorkflowId(workflowId)
                                                .withEnvId(envId)
                                                .withServiceIds(Lists.newArrayList(serviceId))
                                                .withWorkflowType(WorkflowType.ORCHESTRATION)
                                                .build();
      workflowExecution.setAppId(appId);

      String workflowExecutionId = wingsPersistence.save(workflowExecution);
      workflowExecutionIds.add(workflowExecutionId);
      workflowExecutions.add(workflowExecution);
      StateExecutionInstance stateExecutionInstance = aStateExecutionInstance()
                                                          .withExecutionUuid(workflowExecutionId)
                                                          .withStateType(StateType.DYNA_TRACE.name())
                                                          .build();
      stateExecutionInstance.setAppId(appId);
      wingsPersistence.save(stateExecutionInstance);
    }

    for (String workflowExecutionId : workflowExecutionIds) {
      WorkflowExecution workflowExecution = wingsPersistence.get(WorkflowExecution.class, appId, workflowExecutionId);
      assertFalse(workflowExecution.isBaseline());
    }

    for (int i = 0; i < numOfWorkflowExecutions; i++) {
      String workflowExecutionId = workflowExecutionIds.get(i);
      Set<WorkflowExecutionBaseline> workflowExecutionBaselines =
          workflowExecutionService.markBaseline(appId, workflowExecutionId, true);
      assertEquals(1, workflowExecutionBaselines.size());
      assertEquals(1, wingsPersistence.createQuery(WorkflowExecutionBaseline.class).count());
      WorkflowExecutionBaseline baseline = workflowExecutionBaselines.iterator().next();
      assertEquals(appId, baseline.getAppId());
      assertEquals(workflowId, baseline.getWorkflowId());
      assertEquals(envId, baseline.getEnvId());
      assertEquals(serviceId, baseline.getServiceId());

      for (String executionId : workflowExecutionIds) {
        WorkflowExecution workflowExecution = wingsPersistence.get(WorkflowExecution.class, appId, executionId);
        if (workflowExecutionId.equals(executionId)) {
          assertTrue(workflowExecution.isBaseline());
        } else {
          assertFalse(workflowExecution.isBaseline());
        }
      }

      // unmark and test
      workflowExecutionBaselines = workflowExecutionService.markBaseline(appId, workflowExecutionId, false);
      assertEquals(1, workflowExecutionBaselines.size());
      assertEquals(0, wingsPersistence.createQuery(WorkflowExecutionBaseline.class).count());

      for (String executionId : workflowExecutionIds) {
        WorkflowExecution workflowExecution = wingsPersistence.get(WorkflowExecution.class, appId, executionId);
        assertFalse(workflowExecution.isBaseline());
      }

      // mark again and test
      workflowExecutionBaselines = workflowExecutionService.markBaseline(appId, workflowExecutionId, true);
      assertEquals(1, workflowExecutionBaselines.size());
      assertEquals(1, wingsPersistence.createQuery(WorkflowExecutionBaseline.class).count());

      for (String executionId : workflowExecutionIds) {
        WorkflowExecution workflowExecution = wingsPersistence.get(WorkflowExecution.class, appId, executionId);
        if (workflowExecutionId.equals(executionId)) {
          assertTrue(workflowExecution.isBaseline());
        } else {
          assertFalse(workflowExecution.isBaseline());
        }
      }
    }
  }

  @Test
  public void testMarkAndCreateBaselinesForPipelineAndWorkflow() {
    int numOfWorkflowExecutions = 10;
    List<String> envIds = new ArrayList<>();
    List<String> serviceIds = new ArrayList<>();
    List<String> workflowExecutionIds = new ArrayList<>();
    List<WorkflowExecution> workflowExecutions = new ArrayList<>();
    String pipelineExecutionId = UUID.randomUUID().toString();

    for (int i = 0; i < numOfWorkflowExecutions; i++) {
      String envId = UUID.randomUUID().toString();
      envIds.add(envId);
      String serviceId = UUID.randomUUID().toString();
      serviceIds.add(serviceId);

      WorkflowExecution workflowExecution = aWorkflowExecution()
                                                .withWorkflowId(workflowId)
                                                .withEnvId(envId)
                                                .withServiceIds(Lists.newArrayList(serviceId))
                                                .withPipelineExecutionId(pipelineExecutionId)
                                                .build();
      workflowExecution.setAppId(appId);

      String workflowExecutionId = wingsPersistence.save(workflowExecution);
      workflowExecutionIds.add(workflowExecutionId);
      workflowExecutions.add(workflowExecution);
      StateExecutionInstance stateExecutionInstance = aStateExecutionInstance()
                                                          .withExecutionUuid(workflowExecutionId)
                                                          .withStateType(StateType.DYNA_TRACE.name())
                                                          .build();
      stateExecutionInstance.setAppId(appId);
      wingsPersistence.save(stateExecutionInstance);
    }

    for (String workflowExecutionId : workflowExecutionIds) {
      WorkflowExecution workflowExecution = wingsPersistence.get(WorkflowExecution.class, appId, workflowExecutionId);
      assertFalse(workflowExecution.isBaseline());
    }

    WorkflowExecution piplelineExecution = aWorkflowExecution()
                                               .withWorkflowId(workflowId)
                                               .withWorkflowType(WorkflowType.PIPELINE)
                                               .withUuid(pipelineExecutionId)
                                               .build();
    piplelineExecution.setAppId(appId);
    PipelineStageExecution pipelineStageExecution =
        PipelineStageExecution.Builder.aPipelineStageExecution().withWorkflowExecutions(workflowExecutions).build();
    PipelineExecution pipelineExecution = PipelineExecution.Builder.aPipelineExecution()
                                              .withPipelineStageExecutions(Lists.newArrayList(pipelineStageExecution))
                                              .build();
    piplelineExecution.setPipelineExecution(pipelineExecution);
    wingsPersistence.save(piplelineExecution);
    Set<WorkflowExecutionBaseline> workflowExecutionBaselines =
        workflowExecutionService.markBaseline(appId, pipelineExecutionId, true);
    assertEquals(numOfWorkflowExecutions, workflowExecutionBaselines.size());

    for (WorkflowExecutionBaseline baseline : workflowExecutionBaselines) {
      assertEquals(appId, baseline.getAppId());
      assertEquals(workflowId, baseline.getWorkflowId());
      assertEquals(pipelineExecutionId, baseline.getPipelineExecutionId());
      assertTrue(envIds.contains(baseline.getEnvId()));
      assertTrue(serviceIds.contains(baseline.getServiceId()));
      assertTrue(workflowExecutionIds.contains(baseline.getWorkflowExecutionId()));
    }

    for (String executionId : workflowExecutionIds) {
      WorkflowExecution workflowExecution1 = wingsPersistence.get(WorkflowExecution.class, appId, executionId);
      assertTrue(workflowExecution1.isBaseline());
    }

    WorkflowExecution savedPipelineExecution = wingsPersistence.get(WorkflowExecution.class, pipelineExecutionId);
    pipelineExecution = savedPipelineExecution.getPipelineExecution();
    List<PipelineStageExecution> pipelineStageExecutions = pipelineExecution.getPipelineStageExecutions();
    pipelineStageExecutions.forEach(
        stageExecution -> stageExecution.getWorkflowExecutions().forEach(pipelineWorkflowExecution -> {
          assertTrue(pipelineWorkflowExecution.isBaseline());
        }));

    final int workflowNum = new Random().nextInt(10) % numOfWorkflowExecutions;
    String envId = envIds.get(workflowNum);
    String serviceId = serviceIds.get(workflowNum);

    WorkflowExecution workflowExecution = aWorkflowExecution()
                                              .withWorkflowId(workflowId)
                                              .withEnvId(envId)
                                              .withWorkflowType(WorkflowType.ORCHESTRATION)
                                              .withServiceIds(Lists.newArrayList(serviceId))
                                              .build();
    workflowExecution.setAppId(appId);
    String workflowExecutionId = wingsPersistence.save(workflowExecution);

    StateExecutionInstance stateExecutionInstance = aStateExecutionInstance()
                                                        .withExecutionUuid(workflowExecutionId)
                                                        .withStateType(StateType.DYNA_TRACE.name())
                                                        .build();
    stateExecutionInstance.setAppId(appId);
    wingsPersistence.save(stateExecutionInstance);

    workflowExecutionBaselines = workflowExecutionService.markBaseline(appId, workflowExecutionId, true);
    assertEquals(1, workflowExecutionBaselines.size());
    WorkflowExecutionBaseline workflowExecutionBaseline = workflowExecutionBaselines.iterator().next();
    assertEquals(envId, workflowExecutionBaseline.getEnvId());
    assertEquals(serviceId, workflowExecutionBaseline.getServiceId());
    assertEquals(workflowId, workflowExecutionBaseline.getWorkflowId());
    assertEquals(workflowExecutionId, workflowExecutionBaseline.getWorkflowExecutionId());
    assertNull(workflowExecutionBaseline.getPipelineExecutionId());

    savedPipelineExecution = wingsPersistence.get(WorkflowExecution.class, pipelineExecutionId);
    pipelineExecution = savedPipelineExecution.getPipelineExecution();
    pipelineStageExecutions = pipelineExecution.getPipelineStageExecutions();
    AtomicInteger numOfBaselineWorkflow = new AtomicInteger(0);
    AtomicInteger numOfNonBaselineWorkflow = new AtomicInteger(0);
    pipelineStageExecutions.forEach(stageExecution -> {
      for (int i = 0; i < numOfWorkflowExecutions; i++) {
        if (i == workflowNum) {
          assertFalse(stageExecution.getWorkflowExecutions().get(i).isBaseline());
          numOfNonBaselineWorkflow.incrementAndGet();
        } else {
          assertTrue(stageExecution.getWorkflowExecutions().get(i).isBaseline());
          numOfBaselineWorkflow.incrementAndGet();
        }
      }
    });

    assertEquals(1, numOfNonBaselineWorkflow.get());
    assertEquals(numOfWorkflowExecutions - 1, numOfBaselineWorkflow.get());
  }

  @Test
  public void testGetBaselineDetails() {
    StateMachineExecutor stateMachineExecutor = Mockito.mock(StateMachineExecutor.class);
    int numOfWorkflowExecutions = 10;
    List<String> envIds = new ArrayList<>();
    List<String> serviceIds = new ArrayList<>();
    List<String> workflowExecutionIds = new ArrayList<>();
    List<String> stateExecutionIds = new ArrayList<>();
    List<WorkflowExecution> workflowExecutions = new ArrayList<>();
    String pipelineExecutionId = UUID.randomUUID().toString();

    for (int i = 0; i < numOfWorkflowExecutions; i++) {
      String envId = UUID.randomUUID().toString();
      envIds.add(envId);
      String serviceId = UUID.randomUUID().toString();
      serviceIds.add(serviceId);

      WorkflowExecution workflowExecution = aWorkflowExecution()
                                                .withWorkflowId(workflowId)
                                                .withEnvId(envId)
                                                .withServiceIds(Lists.newArrayList(serviceId))
                                                .withPipelineExecutionId(pipelineExecutionId)
                                                .build();
      workflowExecution.setAppId(appId);

      String workflowExecutionId = wingsPersistence.save(workflowExecution);
      workflowExecutionIds.add(workflowExecutionId);
      workflowExecutions.add(workflowExecution);
      StateExecutionInstance stateExecutionInstance = aStateExecutionInstance()
                                                          .withExecutionUuid(workflowExecutionId)
                                                          .withStateType(StateType.DYNA_TRACE.name())
                                                          .build();
      stateExecutionInstance.setAppId(appId);
      String stateExecutionId = wingsPersistence.save(stateExecutionInstance);
      stateExecutionIds.add(stateExecutionId);

      ExecutionContext executionContext = Mockito.mock(ExecutionContext.class);
      WorkflowStandardParams workflowStandardParams = Mockito.mock(WorkflowStandardParams.class);
      when(workflowStandardParams.getEnv()).thenReturn(anEnvironment().withUuid(envId).build());
      when(executionContext.getContextElement(ContextElementType.STANDARD)).thenReturn(workflowStandardParams);
      when(executionContext.getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM))
          .thenReturn(aPhaseElement().withServiceElement(aServiceElement().withUuid(serviceId).build()).build());
      when(stateMachineExecutor.getExecutionContext(appId, workflowExecutionId, stateExecutionId))
          .thenReturn(executionContext);
    }

    setInternalState(workflowExecutionService, "stateMachineExecutor", stateMachineExecutor);

    for (int i = 0; i < numOfWorkflowExecutions; i++) {
      WorkflowExecution workflowExecution =
          wingsPersistence.get(WorkflowExecution.class, appId, workflowExecutionIds.get(i));
      assertFalse(workflowExecution.isBaseline());

      WorkflowExecutionBaseline baselineDetails = workflowExecutionService.getBaselineDetails(
          appId, workflowExecutionIds.get(i), stateExecutionIds.get(i), workflowExecutionIds.get(i));
      assertNull(baselineDetails);
    }

    WorkflowExecution workflowExecution = aWorkflowExecution()
                                              .withWorkflowId(workflowId)
                                              .withWorkflowType(WorkflowType.PIPELINE)
                                              .withUuid(pipelineExecutionId)
                                              .build();
    workflowExecution.setAppId(appId);
    PipelineStageExecution pipelineStageExecution =
        PipelineStageExecution.Builder.aPipelineStageExecution().withWorkflowExecutions(workflowExecutions).build();
    PipelineExecution pipelineExecution = PipelineExecution.Builder.aPipelineExecution()
                                              .withPipelineStageExecutions(Lists.newArrayList(pipelineStageExecution))
                                              .build();
    workflowExecution.setPipelineExecution(pipelineExecution);
    String workflowExecutionId = wingsPersistence.save(workflowExecution);

    Set<WorkflowExecutionBaseline> workflowExecutionBaselines =
        workflowExecutionService.markBaseline(appId, workflowExecutionId, true);
    assertEquals(numOfWorkflowExecutions, workflowExecutionBaselines.size());
    assertEquals(numOfWorkflowExecutions, wingsPersistence.createQuery(WorkflowExecutionBaseline.class).count());

    for (WorkflowExecutionBaseline baseline : workflowExecutionBaselines) {
      assertEquals(appId, baseline.getAppId());
      assertEquals(workflowId, baseline.getWorkflowId());
      assertEquals(workflowExecutionId, baseline.getPipelineExecutionId());
      assertTrue(envIds.contains(baseline.getEnvId()));
      assertTrue(serviceIds.contains(baseline.getServiceId()));
      assertTrue(workflowExecutionIds.contains(baseline.getWorkflowExecutionId()));
    }

    for (int i = 0; i < numOfWorkflowExecutions; i++) {
      WorkflowExecution workflowExecution1 =
          wingsPersistence.get(WorkflowExecution.class, appId, workflowExecutionIds.get(i));
      assertTrue(workflowExecution1.isBaseline());
      WorkflowExecutionBaseline baselineDetails = workflowExecutionService.getBaselineDetails(
          appId, workflowExecutionIds.get(i), stateExecutionIds.get(i), workflowExecutionIds.get(i));
      assertNotNull(baselineDetails);
      assertEquals(userEmail, baselineDetails.getCreatedBy().getEmail());
      assertEquals(userName, baselineDetails.getCreatedBy().getName());
      assertEquals(userEmail, baselineDetails.getLastUpdatedBy().getEmail());
      assertEquals(userName, baselineDetails.getLastUpdatedBy().getName());
    }

    WorkflowExecution savedPipelineExecution = wingsPersistence.get(WorkflowExecution.class, workflowExecutionId);
    pipelineExecution = savedPipelineExecution.getPipelineExecution();
    List<PipelineStageExecution> pipelineStageExecutions = pipelineExecution.getPipelineStageExecutions();
    pipelineStageExecutions.forEach(
        stageExecution -> stageExecution.getWorkflowExecutions().forEach(pipelineWorkflowExecution -> {
          assertTrue(pipelineWorkflowExecution.isBaseline());
        }));
  }
}
