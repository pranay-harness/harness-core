package io.harness.pms.creator;

import io.harness.pms.plan.PlanCreationServiceGrpc.PlanCreationServiceBlockingStub;
import lombok.Value;

import java.util.Map;
import java.util.Set;

@Value
public class PlanCreatorServiceInfo {
  Map<String, Set<String>> supportedTypes;
  PlanCreationServiceBlockingStub planCreationClient;
}
