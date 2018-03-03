package software.wings.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static software.wings.beans.WorkflowExecution.WorkflowExecutionBuilder.aWorkflowExecution;
import static software.wings.sm.StateExecutionInstance.Builder.aStateExecutionInstance;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.WingsBaseTest;
import software.wings.beans.ErrorCode;
import software.wings.beans.PipelineExecution;
import software.wings.beans.PipelineStageExecution;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.WorkflowType;
import software.wings.beans.baseline.WorkflowExecutionBaseline;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.StateType;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Created by rsingh on 2/16/18.
 */
public class WorkflowExecutionBaselineServiceTest extends WingsBaseTest {
  private static final Logger logger = LoggerFactory.getLogger(WorkflowExecutionBaselineServiceTest.class);

  @Inject private WingsPersistence wingsPersistence;
  @Inject private WorkflowExecutionService workflowExecutionService;

  private String appId;
  private String workflowId;

  @Before
  public void setUp() {
    appId = UUID.randomUUID().toString();
    workflowId = UUID.randomUUID().toString();
  }

  @Test()
  public void testNoWorkflowExecution() {
    String workflowExecutionId = UUID.randomUUID().toString();
    try {
      workflowExecutionService.markAsBaseline(appId, workflowExecutionId);
      fail("Did not fail for invalid workflow execution id");
    } catch (WingsException e) {
      assertEquals(ErrorCode.BASELINE_CONFIGURATION_ERROR, e.getResponseMessage().getCode());
      assertEquals("No workflow execution found with id: " + workflowExecutionId + " appId: " + appId,
          e.getResponseMessage().getMessage());
    }
  }

  @Test
  public void testNotPipeline() {
    WorkflowExecution workflowExecution =
        aWorkflowExecution().withWorkflowId(workflowId).withWorkflowType(WorkflowType.ORCHESTRATION).build();
    workflowExecution.setAppId(appId);
    String workflowExecutionId = wingsPersistence.save(workflowExecution);
    try {
      workflowExecutionService.markAsBaseline(appId, workflowExecutionId);
    } catch (WingsException e) {
      assertEquals(ErrorCode.BASELINE_CONFIGURATION_ERROR, e.getResponseMessage().getCode());
      assertEquals("Only pipelines can be marked as baseline.", e.getResponseMessage().getMessage());
    }
  }

  @Test
  public void testNoPipelineExecutionsForPipeline() {
    WorkflowExecution workflowExecution =
        aWorkflowExecution().withWorkflowId(workflowId).withWorkflowType(WorkflowType.PIPELINE).build();
    workflowExecution.setAppId(appId);
    String workflowExecutionId = wingsPersistence.save(workflowExecution);
    try {
      workflowExecutionService.markAsBaseline(appId, workflowExecutionId);
    } catch (WingsException e) {
      assertEquals(ErrorCode.BASELINE_CONFIGURATION_ERROR, e.getResponseMessage().getCode());
      assertEquals("Pipeline has not been executed.", e.getResponseMessage().getMessage());
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
      workflowExecutionService.markAsBaseline(appId, workflowExecutionId);
    } catch (WingsException e) {
      assertEquals(ErrorCode.BASELINE_CONFIGURATION_ERROR, e.getResponseMessage().getCode());
      assertEquals("No workflows have been executed for this pipeline.", e.getResponseMessage().getMessage());
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
      workflowExecutionService.markAsBaseline(appId, workflowExecutionId);
    } catch (WingsException e) {
      assertEquals(ErrorCode.BASELINE_CONFIGURATION_ERROR, e.getResponseMessage().getCode());
      assertEquals("There is no workflow execution in this pipeline with verification steps.",
          e.getResponseMessage().getMessage());
    }
  }

  @Test
  public void testMarkAndCreateBaselines() {
    int numOfWorkflowExecutions = 10;
    List<String> envIds = new ArrayList<>();
    List<String> serviceIds = new ArrayList<>();
    List<String> workflowExecutionIds = new ArrayList<>();
    List<WorkflowExecution> workflowExecutions = new ArrayList<>();

    for (int i = 0; i < numOfWorkflowExecutions; i++) {
      String envId = UUID.randomUUID().toString();
      envIds.add(envId);
      String serviceId = UUID.randomUUID().toString();
      serviceIds.add(serviceId);

      WorkflowExecution workflowExecution = aWorkflowExecution()
                                                .withWorkflowId(workflowId)
                                                .withEnvId(envId)
                                                .withServiceIds(Lists.newArrayList(serviceId))
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

    WorkflowExecution workflowExecution =
        aWorkflowExecution().withWorkflowId(workflowId).withWorkflowType(WorkflowType.PIPELINE).build();
    workflowExecution.setAppId(appId);
    PipelineStageExecution pipelineStageExecution =
        PipelineStageExecution.Builder.aPipelineStageExecution().withWorkflowExecutions(workflowExecutions).build();
    PipelineExecution pipelineExecution = PipelineExecution.Builder.aPipelineExecution()
                                              .withPipelineStageExecutions(Lists.newArrayList(pipelineStageExecution))
                                              .build();
    workflowExecution.setPipelineExecution(pipelineExecution);
    String workflowExecutionId = wingsPersistence.save(workflowExecution);
    Set<WorkflowExecutionBaseline> workflowExecutionBaselines =
        workflowExecutionService.markAsBaseline(appId, workflowExecutionId);
    assertEquals(numOfWorkflowExecutions, workflowExecutionBaselines.size());

    for (WorkflowExecutionBaseline baseline : workflowExecutionBaselines) {
      assertEquals(appId, baseline.getAppId());
      assertEquals(workflowId, baseline.getWorkflowId());
      assertTrue(envIds.contains(baseline.getEnvId()));
      assertTrue(serviceIds.contains(baseline.getServiceId()));
      assertTrue(workflowExecutionIds.contains(baseline.getWorkflowExecutionId()));
    }

    for (String executionId : workflowExecutionIds) {
      WorkflowExecution workflowExecution1 = wingsPersistence.get(WorkflowExecution.class, appId, executionId);
      assertTrue(workflowExecution1.isBaseline());
    }
  }

  @Test
  public void testMarkBaselinesUpdate() {
    int numOfWorkflowExecutions = 10;
    int numOfPipelines = 5;
    List<String> envIds = new ArrayList<>();
    List<String> serviceIds = new ArrayList<>();
    List<List<String>> workflowExecutionIds = new ArrayList<>();

    for (int n = 0; n < numOfWorkflowExecutions; n++) {
      envIds.add(UUID.randomUUID().toString());
      serviceIds.add(UUID.randomUUID().toString());
    }

    for (int i = 0; i < numOfPipelines; i++) {
      logger.info("running for pipeline " + i);
      workflowExecutionIds.add(new ArrayList<>());
      List<WorkflowExecution> workflowExecutions = new ArrayList<>();
      for (int j = 0; j < numOfWorkflowExecutions; j++) {
        WorkflowExecution workflowExecution = aWorkflowExecution()
                                                  .withWorkflowId(workflowId)
                                                  .withEnvId(envIds.get(j))
                                                  .withServiceIds(Lists.newArrayList(serviceIds.get(j)))
                                                  .withUuid("workflowExecution-" + i + "-" + j)
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

      WorkflowExecution workflowExecution =
          aWorkflowExecution().withWorkflowId(workflowId).withWorkflowType(WorkflowType.PIPELINE).build();
      workflowExecution.setAppId(appId);
      PipelineStageExecution pipelineStageExecution =
          PipelineStageExecution.Builder.aPipelineStageExecution().withWorkflowExecutions(workflowExecutions).build();
      PipelineExecution pipelineExecution = PipelineExecution.Builder.aPipelineExecution()
                                                .withPipelineStageExecutions(Lists.newArrayList(pipelineStageExecution))
                                                .build();
      workflowExecution.setPipelineExecution(pipelineExecution);
      String workflowExecutionId = wingsPersistence.save(workflowExecution);
      Set<WorkflowExecutionBaseline> workflowExecutionBaselines =
          workflowExecutionService.markAsBaseline(appId, workflowExecutionId);
      assertEquals(numOfWorkflowExecutions, workflowExecutionBaselines.size());

      for (WorkflowExecutionBaseline baseline : workflowExecutionBaselines) {
        assertEquals(appId, baseline.getAppId());
        assertEquals(workflowId, baseline.getWorkflowId());
        assertTrue(envIds.contains(baseline.getEnvId()));
        assertTrue(serviceIds.contains(baseline.getServiceId()));
        assertTrue("failed for " + baseline, workflowExecutionIds.get(i).contains(baseline.getWorkflowExecutionId()));
      }

      for (int pipeline = 0; pipeline <= i; pipeline++) {
        for (String executionId : workflowExecutionIds.get(pipeline)) {
          WorkflowExecution workflowExecution1 = wingsPersistence.get(WorkflowExecution.class, appId, executionId);

          if (pipeline == i) {
            assertTrue(workflowExecution1.isBaseline());
          } else {
            assertFalse(workflowExecution1.isBaseline());
          }
        }
      }
    }
  }
}
