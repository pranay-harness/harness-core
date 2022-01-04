package io.harness.engine.pms.resume;

import io.harness.plan.Node;
import io.harness.pms.contracts.ambiance.Ambiance;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NodeResumeRequest {
  Node planNode;
  Ambiance ambiance;
}
