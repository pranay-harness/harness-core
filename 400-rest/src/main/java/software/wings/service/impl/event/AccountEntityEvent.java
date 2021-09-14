/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.impl.event;

import io.harness.event.model.EventInfo;

import software.wings.beans.Account;

import lombok.Value;

@Value
public class AccountEntityEvent implements EventInfo {
  private Account account;
}
