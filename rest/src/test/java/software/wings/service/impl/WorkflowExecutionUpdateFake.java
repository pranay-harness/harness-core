package software.wings.service.impl;

import static io.harness.data.structure.UUIDGenerator.generateUuid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionStatus;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * The type Workflow execution update fake.
 */
public class WorkflowExecutionUpdateFake extends WorkflowExecutionUpdate {
  private static final Logger logger = LoggerFactory.getLogger(WorkflowExecutionUpdateFake.class);

  private static Map<String, CountDownLatch> signalIdsMap = new HashMap<>();

  private String signalId;

  /**
   * Instantiates a new Workflow execution update mock.
   *
   */
  public WorkflowExecutionUpdateFake() {
    this.signalId = generateUuid();
    signalIdsMap.put(signalId, new CountDownLatch(1));
  }

  /**
   * Instantiates a new Workflow execution update mock.
   *
   * @param workflowExecutionId the workflowExecution id
   */
  public WorkflowExecutionUpdateFake(String appId, String workflowExecutionId) {
    super(appId, workflowExecutionId);
    this.signalId = generateUuid();
    signalIdsMap.put(signalId, new CountDownLatch(1));
  }

  @Override
  public void callback(ExecutionContext context, ExecutionStatus status, Exception ex) {
    logger.info(status.toString());
    super.callback(context, status, ex);
    signalIdsMap.get(signalId).countDown();
  }

  /**
   * Gets signal id.
   *
   * @return the signal id
   */
  public String getSignalId() {
    return signalId;
  }

  /**
   * Sets signal id.
   *
   * @param signalId the signal id
   */
  public void setSignalId(String signalId) {
    this.signalId = signalId;
  }

  public void await(Duration timeout) throws InterruptedException {
    signalIdsMap.get(signalId).await(timeout.toMillis(), TimeUnit.MILLISECONDS);
  }
}
