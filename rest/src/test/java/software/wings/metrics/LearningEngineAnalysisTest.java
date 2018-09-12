package software.wings.metrics;

import static io.harness.persistence.HQuery.excludeAuthority;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static software.wings.service.impl.newrelic.LearningEngineAnalysisTask.TIME_SERIES_ANALYSIS_TASK_TIME_OUT;

import com.google.inject.Inject;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import software.wings.WingsBaseTest;
import software.wings.beans.ServiceSecretKey;
import software.wings.beans.ServiceSecretKey.ServiceApiVersion;
import software.wings.beans.ServiceSecretKey.ServiceType;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.newrelic.LearningEngineAnalysisTask;
import software.wings.service.impl.newrelic.LearningEngineExperimentalAnalysisTask;
import software.wings.service.intfc.LearningEngineService;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.StateType;
import software.wings.utils.Misc;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Created by rsingh on 1/9/18.
 */
public class LearningEngineAnalysisTest extends WingsBaseTest {
  @Inject private LearningEngineService learningEngineService;
  @Inject private WingsPersistence wingsPersistence;

  private String workflowExecutionId;
  private String stateExecutionId;

  @Before
  public void setup() {
    workflowExecutionId = UUID.randomUUID().toString();
    stateExecutionId = UUID.randomUUID().toString();
  }

  @After
  public void tearDown() {
    LearningEngineAnalysisTask.TIME_SERIES_ANALYSIS_TASK_TIME_OUT = TimeUnit.MINUTES.toMillis(2);
  }

  @Test
  public void testQueueWithStatus() {
    int numOfTasks = 100;
    for (int i = 0; i < numOfTasks; i++) {
      workflowExecutionId = UUID.randomUUID().toString();
      stateExecutionId = UUID.randomUUID().toString();
      LearningEngineAnalysisTask learningEngineAnalysisTask = LearningEngineAnalysisTask.builder()
                                                                  .state_execution_id(stateExecutionId)
                                                                  .workflow_execution_id(workflowExecutionId)
                                                                  .executionStatus(ExecutionStatus.QUEUED)
                                                                  .build();
      learningEngineService.addLearningEngineAnalysisTask(learningEngineAnalysisTask);
    }

    assertEquals(numOfTasks, wingsPersistence.createQuery(LearningEngineAnalysisTask.class).count());

    for (int i = 1; i <= numOfTasks; i++) {
      LearningEngineAnalysisTask analysisTask =
          learningEngineService.getNextLearningEngineAnalysisTask(ServiceApiVersion.V1);
      assertEquals(ExecutionStatus.RUNNING, analysisTask.getExecutionStatus());

      assertEquals(numOfTasks - i,
          wingsPersistence.createQuery(LearningEngineAnalysisTask.class, excludeAuthority)
              .filter("executionStatus", ExecutionStatus.QUEUED)
              .filter("retry", 0)
              .asList()
              .size());
      assertEquals(i,
          wingsPersistence.createQuery(LearningEngineAnalysisTask.class, excludeAuthority)
              .filter("executionStatus", ExecutionStatus.RUNNING)
              .filter("retry", 1)
              .asList()
              .size());
    }

    assertNull(learningEngineService.getNextLearningEngineAnalysisTask(ServiceApiVersion.V1));
  }

  @Test
  public void testAlreadyQueued() {
    int numOfTasks = 5;
    for (int i = 0; i < numOfTasks; i++) {
      LearningEngineAnalysisTask learningEngineAnalysisTask = LearningEngineAnalysisTask.builder()
                                                                  .state_execution_id(stateExecutionId)
                                                                  .workflow_execution_id(workflowExecutionId)
                                                                  .executionStatus(ExecutionStatus.QUEUED)
                                                                  .analysis_minute(0)
                                                                  .build();
      learningEngineService.addLearningEngineAnalysisTask(learningEngineAnalysisTask);
    }

    List<LearningEngineAnalysisTask> learningEngineAnalysisTasks =
        wingsPersistence.createQuery(LearningEngineAnalysisTask.class, excludeAuthority).asList();
    assertEquals(1, learningEngineAnalysisTasks.size());
    LearningEngineAnalysisTask analysisTask = learningEngineAnalysisTasks.get(0);
    assertEquals(workflowExecutionId, analysisTask.getWorkflow_execution_id());
    assertEquals(stateExecutionId, analysisTask.getState_execution_id());
    assertEquals(0, analysisTask.getAnalysis_minute());
    assertEquals(0, analysisTask.getRetry());
  }

  @Test
  public void testAlreadyQueuedForMinute() {
    LearningEngineAnalysisTask learningEngineAnalysisTask = LearningEngineAnalysisTask.builder()
                                                                .state_execution_id(stateExecutionId)
                                                                .workflow_execution_id(workflowExecutionId)
                                                                .executionStatus(ExecutionStatus.QUEUED)
                                                                .analysis_minute(0)
                                                                .build();
    learningEngineService.addLearningEngineAnalysisTask(learningEngineAnalysisTask);

    learningEngineAnalysisTask =
        LearningEngineAnalysisTask.builder()
            .state_execution_id(stateExecutionId)
            .workflow_execution_id(workflowExecutionId)
            .executionStatus(ExecutionStatus.QUEUED)
            .analysis_minute((int) (TimeUnit.MILLISECONDS.toMinutes(TIME_SERIES_ANALYSIS_TASK_TIME_OUT)))
            .build();
    assertFalse(learningEngineService.addLearningEngineAnalysisTask(learningEngineAnalysisTask));
  }

  @Test
  public void testRetryExceeded() {
    LearningEngineAnalysisTask learningEngineAnalysisTask = LearningEngineAnalysisTask.builder()
                                                                .state_execution_id(stateExecutionId)
                                                                .workflow_execution_id(workflowExecutionId)
                                                                .executionStatus(ExecutionStatus.QUEUED)
                                                                .retry(LearningEngineAnalysisTask.RETRIES)
                                                                .build();
    wingsPersistence.updateField(LearningEngineAnalysisTask.class, learningEngineAnalysisTask.getUuid(), "retry",
        LearningEngineAnalysisTask.RETRIES);
    assertNull(learningEngineService.getNextLearningEngineAnalysisTask(ServiceApiVersion.V1));
  }

  @Test
  public void testQueueWithTimeOut() throws InterruptedException {
    LearningEngineAnalysisTask.TIME_SERIES_ANALYSIS_TASK_TIME_OUT = TimeUnit.SECONDS.toMillis(5);
    long startTime = System.currentTimeMillis();
    int numOfTasks = 10;
    for (int i = 0; i < numOfTasks; i++) {
      workflowExecutionId = UUID.randomUUID().toString();
      stateExecutionId = UUID.randomUUID().toString();
      LearningEngineAnalysisTask learningEngineAnalysisTask = LearningEngineAnalysisTask.builder()
                                                                  .state_execution_id(stateExecutionId)
                                                                  .workflow_execution_id(workflowExecutionId)
                                                                  .executionStatus(ExecutionStatus.RUNNING)
                                                                  .build();
      learningEngineService.addLearningEngineAnalysisTask(learningEngineAnalysisTask);
    }
    Thread.sleep(TimeUnit.SECONDS.toMillis(10));

    assertEquals(numOfTasks, wingsPersistence.createQuery(LearningEngineAnalysisTask.class).count());

    for (int i = 1; i <= numOfTasks; i++) {
      LearningEngineAnalysisTask analysisTask =
          learningEngineService.getNextLearningEngineAnalysisTask(ServiceApiVersion.V1);
      assertEquals(ExecutionStatus.RUNNING, analysisTask.getExecutionStatus());

      assertEquals(numOfTasks - i,
          wingsPersistence.createQuery(LearningEngineAnalysisTask.class, excludeAuthority)
              .field("lastUpdatedAt")
              .greaterThan(startTime)
              .filter("retry", 0)
              .asList()
              .size());
      assertEquals(i,
          wingsPersistence.createQuery(LearningEngineAnalysisTask.class, excludeAuthority)
              .field("lastUpdatedAt")
              .greaterThan(startTime)
              .filter("retry", 1)
              .asList()
              .size());
    }

    assertNull(learningEngineService.getNextLearningEngineAnalysisTask(ServiceApiVersion.V1));
  }

  @Test
  @Ignore
  public void testInitializeServiceSecretKeys() {
    assertTrue(wingsPersistence.createQuery(ServiceSecretKey.class).asList().isEmpty());
    learningEngineService.initializeServiceSecretKeys();
    List<ServiceSecretKey> serviceSecretKeys = wingsPersistence.createQuery(ServiceSecretKey.class)
                                                   .filter("serviceType", ServiceType.LEARNING_ENGINE)
                                                   .asList();
    assertEquals(1, serviceSecretKeys.size());

    String secretKey = serviceSecretKeys.get(0).getServiceSecret();

    int numOfTries = 24;
    for (int i = 0; i < numOfTries; i++) {
      learningEngineService.initializeServiceSecretKeys();
    }

    serviceSecretKeys = wingsPersistence.createQuery(ServiceSecretKey.class)
                            .filter("serviceType", ServiceType.LEARNING_ENGINE)
                            .asList();
    assertEquals(1, serviceSecretKeys.size());

    assertEquals(secretKey, serviceSecretKeys.get(0).getServiceSecret());
  }

  @Test
  public void testParseVersion() {
    ServiceApiVersion latestVersion = ServiceApiVersion.values()[ServiceApiVersion.values().length - 1];
    assertEquals(latestVersion, Misc.parseApisVersion("application/json"));

    for (ServiceApiVersion serviceApiVersion : ServiceApiVersion.values()) {
      String headerString = "application/" + serviceApiVersion.name().toLowerCase() + "+json, application/json";
      assertEquals(serviceApiVersion, Misc.parseApisVersion(headerString));
    }
  }

  @Test
  public void testUniqueIndexExperimentalTask() {
    assertTrue(
        learningEngineService.addLearningEngineExperimentalAnalysisTask(LearningEngineExperimentalAnalysisTask.builder()
                                                                            .state_execution_id(stateExecutionId)
                                                                            .workflow_execution_id(workflowExecutionId)
                                                                            .stateType(StateType.ELK)
                                                                            .executionStatus(ExecutionStatus.QUEUED)
                                                                            .analysis_minute(14)
                                                                            .build()));

    assertFalse(
        learningEngineService.addLearningEngineExperimentalAnalysisTask(LearningEngineExperimentalAnalysisTask.builder()
                                                                            .state_execution_id(stateExecutionId)
                                                                            .workflow_execution_id(workflowExecutionId)
                                                                            .stateType(StateType.ELK)
                                                                            .executionStatus(ExecutionStatus.QUEUED)
                                                                            .analysis_minute(14)
                                                                            .build()));
  }
}
