package software.wings.waitnotify;

import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Serialized;
import software.wings.beans.Base;
import software.wings.sm.ExecutionStatus;

import java.util.Arrays;
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

  @Serialized private NotifyCallback callback;

  private long timeoutMsec;

  private ExecutionStatus status = ExecutionStatus.NEW;

  public WaitInstance() {}

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
    this.correlationIds = Arrays.asList(correlationIds);
  }

  public List<String> getCorrelationIds() {
    return correlationIds;
  }

  public void setCorrelationIds(List<String> correlationIds) {
    this.correlationIds = correlationIds;
  }

  public NotifyCallback getCallback() {
    return callback;
  }

  public void setCallback(NotifyCallback callback) {
    this.callback = callback;
  }

  public long getTimeoutMsec() {
    return timeoutMsec;
  }

  public void setTimeoutMsec(long timeoutMsec) {
    this.timeoutMsec = timeoutMsec;
  }

  public ExecutionStatus getStatus() {
    return status;
  }

  public void setStatus(ExecutionStatus status) {
    this.status = status;
  }

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

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), correlationIds, callback, timeoutMsec, status);
  }
}
