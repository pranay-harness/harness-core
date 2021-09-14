/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.state.inspection;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import java.util.Collection;
import java.util.List;

@OwnedBy(CDC)
public interface StateInspectionService {
  StateInspection get(String stateExecutionInstanceId);
  List<StateInspection> listUsingSecondary(Collection<String> stateExecutionInstanceIds);
  void append(String stateExecutionInstanceId, StateInspectionData data);
  void append(String stateExecutionInstanceId, List<StateInspectionData> data);
}
