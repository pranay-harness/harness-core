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
