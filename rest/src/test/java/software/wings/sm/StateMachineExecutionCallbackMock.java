package software.wings.sm;

import static io.harness.data.structure.UUIDGenerator.generateUuid;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

/**
 * Created by rishi on 2/25/17.
 */
public class StateMachineExecutionCallbackMock implements StateMachineExecutionCallback {
  private static Map<String, CountDownLatch> signalIdsMap = new HashMap<>();
  private String signalId;

  public StateMachineExecutionCallbackMock() {
    this.signalId = generateUuid();
    signalIdsMap.put(signalId, new CountDownLatch(1));
  }

  @Override
  public void callback(ExecutionContext context, ExecutionStatus status, Exception ex) {
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
