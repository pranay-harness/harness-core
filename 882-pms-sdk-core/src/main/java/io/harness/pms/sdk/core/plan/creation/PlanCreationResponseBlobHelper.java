package io.harness.pms.sdk.core.plan.creation;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.pms.contracts.plan.PlanCreationBlobResponse;
import io.harness.pms.contracts.plan.PlanCreationContextValue;
import io.harness.pms.contracts.plan.PlanNodeProto;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationResponse;
import io.harness.pms.sdk.core.plan.creation.mappers.PlanNodeProtoMapper;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.HashMap;
import java.util.Map;

@OwnedBy(HarnessTeam.PIPELINE)
@Singleton
public class PlanCreationResponseBlobHelper {
  @Inject PlanNodeProtoMapper planNodeProtoMapper;

  public PlanCreationBlobResponse toBlobResponse(PlanCreationResponse planCreationResponse) {
    PlanCreationBlobResponse.Builder finalBlobResponseBuilder = PlanCreationBlobResponse.newBuilder();
    if (EmptyPredicate.isNotEmpty(planCreationResponse.getNodes())) {
      Map<String, PlanNodeProto> newNodes = new HashMap<>();
      planCreationResponse.getNodes().forEach(
          (k, v) -> newNodes.put(k, planNodeProtoMapper.toPlanNodeProtoWithDecoratedFields(v)));
      finalBlobResponseBuilder.putAllNodes(newNodes);
    }
    if (planCreationResponse.getDependencies() != null) {
      finalBlobResponseBuilder.setDeps(planCreationResponse.getDependencies());
    }
    if (EmptyPredicate.isNotEmpty(planCreationResponse.getStartingNodeId())) {
      finalBlobResponseBuilder.setStartingNodeId(planCreationResponse.getStartingNodeId());
    }
    if (EmptyPredicate.isNotEmpty(planCreationResponse.getContextMap())) {
      for (Map.Entry<String, PlanCreationContextValue> dependency : planCreationResponse.getContextMap().entrySet()) {
        finalBlobResponseBuilder.putContext(dependency.getKey(), dependency.getValue());
      }
    }
    if (planCreationResponse.getGraphLayoutResponse() != null) {
      finalBlobResponseBuilder.setGraphLayoutInfo(planCreationResponse.getGraphLayoutResponse().getLayoutNodeInfo());
    }
    return finalBlobResponseBuilder.build();
  }
}
