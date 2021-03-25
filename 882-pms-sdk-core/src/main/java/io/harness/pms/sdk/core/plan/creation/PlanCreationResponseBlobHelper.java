package io.harness.pms.sdk.core.plan.creation;

import static io.harness.data.structure.HasPredicate.hasSome;

import io.harness.pms.contracts.plan.PlanCreationBlobResponse;
import io.harness.pms.contracts.plan.PlanCreationContextValue;
import io.harness.pms.contracts.plan.PlanNodeProto;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationResponse;
import io.harness.pms.sdk.core.plan.creation.mappers.PlanNodeProtoMapper;
import io.harness.pms.yaml.YamlField;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.HashMap;
import java.util.Map;

@Singleton
public class PlanCreationResponseBlobHelper {
  @Inject PlanNodeProtoMapper planNodeProtoMapper;

  public PlanCreationBlobResponse toBlobResponse(PlanCreationResponse planCreationResponse) {
    PlanCreationBlobResponse.Builder finalBlobResponseBuilder = PlanCreationBlobResponse.newBuilder();
    if (hasSome(planCreationResponse.getNodes())) {
      Map<String, PlanNodeProto> newNodes = new HashMap<>();
      planCreationResponse.getNodes().forEach(
          (k, v) -> newNodes.put(k, planNodeProtoMapper.toPlanNodeProtoWithDecoratedFields(v)));
      finalBlobResponseBuilder.putAllNodes(newNodes);
    }
    if (hasSome(planCreationResponse.getDependencies())) {
      for (Map.Entry<String, YamlField> dependency : planCreationResponse.getDependencies().entrySet()) {
        finalBlobResponseBuilder.putDependencies(dependency.getKey(), dependency.getValue().toFieldBlob());
      }
    }
    if (hasSome(planCreationResponse.getStartingNodeId())) {
      finalBlobResponseBuilder.setStartingNodeId(planCreationResponse.getStartingNodeId());
    }
    if (hasSome(planCreationResponse.getContextMap())) {
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
