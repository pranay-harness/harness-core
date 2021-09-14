/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.pms.barriers.service;

import io.harness.pms.barriers.beans.BarrierExecutionInfo;
import io.harness.steps.barriers.beans.BarrierSetupInfo;

import java.util.List;

public interface PMSBarrierService {
  List<BarrierSetupInfo> getBarrierSetupInfoList(String yaml);
  List<BarrierExecutionInfo> getBarrierExecutionInfoList(String stageSetupId, String planExecutionId);
  BarrierExecutionInfo getBarrierExecutionInfo(String barrierSetupId, String planExecutionId);
}
