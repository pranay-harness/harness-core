/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.impl.log;

import software.wings.service.impl.analysis.SetupTestNodeData;
import software.wings.sm.states.CustomLogVerificationState.LogCollectionInfo;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
public class CustomLogSetupTestNodeData extends SetupTestNodeData {
  LogCollectionInfo logCollectionInfo;
  String host;
}
