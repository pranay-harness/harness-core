/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.beans;

import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("DelegateTaskAbortEvent")
public class DelegateTaskAbortEvent extends DelegateTaskEvent {
  public static final class Builder {
    private String delegateTaskId;
    private boolean sync;
    private String accountId;

    private Builder() {}

    public static Builder aDelegateTaskAbortEvent() {
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
      return aDelegateTaskAbortEvent().withDelegateTaskId(delegateTaskId).withSync(sync).withAccountId(accountId);
    }

    public DelegateTaskAbortEvent build() {
      DelegateTaskAbortEvent delegateTaskAbortEvent = new DelegateTaskAbortEvent();
      delegateTaskAbortEvent.setDelegateTaskId(delegateTaskId);
      delegateTaskAbortEvent.setSync(sync);
      delegateTaskAbortEvent.setAccountId(accountId);
      return delegateTaskAbortEvent;
    }
  }
}
