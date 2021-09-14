/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.impl;

import io.harness.logging.AccountLogContext;
import io.harness.logging.AutoLogContext;

import software.wings.sm.StateType;

import java.util.HashMap;
import java.util.Map;

public class VerificationLogContext extends AutoLogContext {
  public static final String cvConfigIdKeyword = "cvConfigId";
  public static final String stateTypeKeyword = "stateType";

  private static Map<String, String> getContext(
      String accountId, String cvConfigId, String stateExecutionId, StateType stateType) {
    Map<String, String> contextMap = new HashMap<>();
    contextMap.put(AccountLogContext.ID, accountId);
    if (cvConfigId != null) {
      contextMap.put(cvConfigIdKeyword, cvConfigId);
    }
    if (stateExecutionId != null) {
      contextMap.put(StateExecutionInstanceLogContext.ID, stateExecutionId);
    }
    if (stateType != null) {
      contextMap.put(stateTypeKeyword, stateType.getName());
    }
    return contextMap;
  }

  public VerificationLogContext(
      String accountId, String cvConfigId, String stateExecutionId, StateType stateType, OverrideBehavior behavior) {
    super(getContext(accountId, cvConfigId, stateExecutionId, stateType), behavior);
  }
}
