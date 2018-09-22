package software.wings.beans;

import com.google.common.base.MoreObjects;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeName;

/**
 * Created by peeyushaggarwal on 1/26/17.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "eventType", include = As.PROPERTY)
@JsonTypeName("DelegateTaskEvent")
@JsonSubTypes({
  @JsonSubTypes.Type(name = "DelegateTaskEvent", value = DelegateTaskEvent.class)
  , @JsonSubTypes.Type(name = "DelegateTaskAbortEvent", value = DelegateTaskAbortEvent.class)
})
public class DelegateTaskEvent {
  private String accountId;
  private String delegateTaskId;
  private boolean sync;

  /**
   * Getter for property 'accountId'.
   *
   * @return Value for property 'accountId'.
   */
  public String getAccountId() {
    return accountId;
  }

  /**
   * Setter for property 'accountId'.
   *
   * @param accountId Value to set for property 'accountId'.
   */
  public void setAccountId(String accountId) {
    this.accountId = accountId;
  }

  /**
   * Getter for property 'delegateTaskId'.
   *
   * @return Value for property 'delegateTaskId'.
   */
  public String getDelegateTaskId() {
    return delegateTaskId;
  }

  /**
   * Setter for property 'delegateTaskId'.
   *
   * @param delegateTaskId Value to set for property 'delegateTaskId'.
   */
  public void setDelegateTaskId(String delegateTaskId) {
    this.delegateTaskId = delegateTaskId;
  }

  /**
   * Getter for property 'sync'.
   *
   * @return Value for property 'sync'.
   */
  public boolean isSync() {
    return sync;
  }

  /**
   * Setter for property 'sync'.
   *
   * @param sync Value to set for property 'sync'.
   */
  public void setSync(boolean sync) {
    this.sync = sync;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("delegateTaskId", delegateTaskId)
        .add("sync", sync)
        .add("accountId", accountId)
        .toString();
  }

  public static final class DelegateTaskEventBuilder {
    private String accountId;
    private String delegateTaskId;
    private boolean sync;

    private DelegateTaskEventBuilder() {}

    public static DelegateTaskEventBuilder aDelegateTaskEvent() {
      return new DelegateTaskEventBuilder();
    }

    public DelegateTaskEventBuilder withAccountId(String accountId) {
      this.accountId = accountId;
      return this;
    }

    public DelegateTaskEventBuilder withDelegateTaskId(String delegateTaskId) {
      this.delegateTaskId = delegateTaskId;
      return this;
    }

    public DelegateTaskEventBuilder withSync(boolean sync) {
      this.sync = sync;
      return this;
    }

    public DelegateTaskEventBuilder but() {
      return aDelegateTaskEvent().withAccountId(accountId).withDelegateTaskId(delegateTaskId).withSync(sync);
    }

    public DelegateTaskEvent build() {
      DelegateTaskEvent delegateTaskEvent = new DelegateTaskEvent();
      delegateTaskEvent.setAccountId(accountId);
      delegateTaskEvent.setDelegateTaskId(delegateTaskId);
      delegateTaskEvent.setSync(sync);
      return delegateTaskEvent;
    }
  }
}
