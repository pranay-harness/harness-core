/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.beans;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DelegateRegisterResponse {
  String delegateId;
  Action action;
  String migrateUrl;
  String sequenceNum;
  String delegateRandomToken;

  public enum Action { SELF_DESTRUCT, MIGRATE }
}
