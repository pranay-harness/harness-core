package software.wings.waitnotify;

import static java.util.Arrays.asList;

import org.mongodb.morphia.annotations.Entity;
import software.wings.beans.Base;
import software.wings.sm.ExecutionStatus;

import java.util.List;
import java.util.Objects;

/**
 * Represents which waiter is waiting on which correlation Ids and callback to execute when done.
 *
 * @author Rishi
 */
@Entity(value = "waitInstances", noClassnameStored = true)
public class WaitInstance extends Base {
  private List<String> correlationIds;

  private NotifyCallback callback;

  private long timeoutMsec;

  private ExecutionStatus status = ExecutionStatus.NEW;

  /**
   * Instantiates a new wait instance.
   */
  public WaitInstance() {}

  /**
   * Instantiates a new wait instance.
   *
   * @param callback       the callback
   * @param correlationIds the correlation ids
   */
  public WaitInstance(NotifyCallback callback, String[] correlationIds) {
    this(0, callback, correlationIds);
  }

  /**
   * Creates a WaitInstance object.
   *
   * @param timeoutMsec    duration to wait for in milliseconds.
   * @param callback       Callback function whenever all waitInstances are done.
   * @param correlationIds List of ids to wait for.
   */
  public WaitInstance(long timeoutMsec, NotifyCallback callback, String[] correlationIds) {
    this.timeoutMsec = timeoutMsec;
    this.callback = callback;
    this.correlationIds = asList(correlationIds);
  }

  /**
   * Gets correlation ids.
   *
   * @return the correlation ids
   */
  public List<String> getCorrelationIds() {
    return correlationIds;
  }

  /**
   * Sets correlation ids.
   *
   * @param correlationIds the correlation ids
   */
  public void setCorrelationIds(List<String> correlationIds) {
    this.correlationIds = correlationIds;
  }

  /**
   * Gets callback.
   *
   * @return the callback
   */
  public NotifyCallback getCallback() {
    return callback;
  }

  /**
   * Sets callback.
   *
   * @param callback the callback
   */
  public void setCallback(NotifyCallback callback) {
    this.callback = callback;
  }

  /**
   * Gets timeout msec.
   *
   * @return the timeout msec
   */
  public long getTimeoutMsec() {
    return timeoutMsec;
  }

  /**
   * Sets timeout msec.
   *
   * @param timeoutMsec the timeout msec
   */
  public void setTimeoutMsec(long timeoutMsec) {
    this.timeoutMsec = timeoutMsec;
  }

  /**
   * Gets status.
   *
   * @return the status
   */
  public ExecutionStatus getStatus() {
    return status;
  }

  /**
   * Sets status.
   *
   * @param status the status
   */
  public void setStatus(ExecutionStatus status) {
    this.status = status;
  }

  /* (non-Javadoc)
   * @see software.wings.beans.Base#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    if (!super.equals(obj)) {
      return false;
    }
    WaitInstance that = (WaitInstance) obj;
    return timeoutMsec == that.timeoutMsec && Objects.equals(correlationIds, that.correlationIds)
        && Objects.equals(callback, that.callback) && status == that.status;
  }

  /* (non-Javadoc)
   * @see software.wings.beans.Base#hashCode()
   */
  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), correlationIds, callback, timeoutMsec, status);
  }
}
