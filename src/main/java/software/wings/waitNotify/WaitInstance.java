package software.wings.waitNotify;

import java.util.Arrays;
import java.util.List;

import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Serialized;

import software.wings.beans.Base;

/**
 * @author Rishi
 *
 */
@Entity(value = "waitInstances", noClassnameStored = true)
public class WaitInstance extends Base {
  private List<String> correlationIds;

  @Serialized private NotifyCallback callback;

  private long timeoutMsec;

  public WaitInstance() {}
  public WaitInstance(NotifyCallback callback, String[] correlationIds) {
    this(0, callback, correlationIds);
  }
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
}
