package software.wings.beans;

/**
 * Created by rishi on 10/31/16.
 */
public class RetryAction implements RepairAction {
  private int retryCount;

  public int getRetryCount() {
    return retryCount;
  }

  public void setRetryCount(int retryCount) {
    this.retryCount = retryCount;
  }
}
