package software.wings.service.impl;

import static software.wings.common.UUIDGenerator.generateUuid;

import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionStatus;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

/**
 * The type Workflow execution update mock.
 */
public class WorkflowExecutionUpdateMock extends WorkflowExecutionUpdate {
  private static Map<String, CountDownLatch> signalIdsMap = new HashMap<>();

  private String signalId;

  /**
   * Instantiates a new Workflow execution update mock.
   *
   */
  public WorkflowExecutionUpdateMock() {
    super();
    this.signalId = generateUuid();
    signalIdsMap.put(signalId, new CountDownLatch(1));
  }

  /**
   * Instantiates a new Workflow execution update mock.
   *
   * @param workflowExecutionId the workflowExecution id
   */
  public WorkflowExecutionUpdateMock(String appId, String workflowExecutionId) {
    super(appId, workflowExecutionId);
    this.signalId = generateUuid();
    signalIdsMap.put(signalId, new CountDownLatch(1));
  }

  @Override
  public void callback(ExecutionContext context, ExecutionStatus status, Exception ex) {
    System.out.println(status);
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

  public void await() throws InterruptedException {
    signalIdsMap.get(signalId).await();
  }
}
