/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.pms.events.base;

public enum PmsEventCategory {
  INTERRUPT_EVENT,
  ORCHESTRATION_EVENT,
  FACILITATOR_EVENT,
  NODE_START,
  PROGRESS_EVENT,
  NODE_ADVISE,
  NODE_RESUME,
  CREATE_PARTIAL_PLAN,
  PARTIAL_PLAN_RESPONSE
}
