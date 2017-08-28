package software.wings.beans;

import com.google.common.base.MoreObjects;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeName;

/**
 * Created by peeyushaggarwal on 1/26/17.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "eventType", include = As.PROPERTY)
@JsonTypeName("DelegateTaskEvent")
public class DelegateTaskEvent {
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

  private String accountId;

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

  public static final class Builder {
    private String delegateTaskId;
    private boolean sync;
    private String accountId;

    private Builder() {}

    public static Builder aDelegateTaskEvent() {
      return new Builder();
    }

    public Builder withDelegateTaskId(String delegateTaskId) {
      this.delegateTaskId = delegateTaskId;
      return this;
    }

    public Builder withSync(boolean sync) {
      this.sync = sync;
      return this;
    }

    public Builder withAccountId(String accountId) {
      this.accountId = accountId;
      return this;
    }

    public Builder but() {
      return aDelegateTaskEvent().withDelegateTaskId(delegateTaskId).withSync(sync).withAccountId(accountId);
    }

    public DelegateTaskEvent build() {
      DelegateTaskEvent delegateTaskEvent = new DelegateTaskEvent();
      delegateTaskEvent.setDelegateTaskId(delegateTaskId);
      delegateTaskEvent.setSync(sync);
      delegateTaskEvent.setAccountId(accountId);
      return delegateTaskEvent;
    }
  }
}
