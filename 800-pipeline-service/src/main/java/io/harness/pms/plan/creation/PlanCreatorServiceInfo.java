/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.pms.plan.creation;

import io.harness.pms.contracts.plan.PlanCreationServiceGrpc.PlanCreationServiceBlockingStub;

import java.util.Map;
import java.util.Set;
import lombok.Value;

@Value
public class PlanCreatorServiceInfo {
  Map<String, Set<String>> supportedTypes;
  PlanCreationServiceBlockingStub planCreationClient;
}
