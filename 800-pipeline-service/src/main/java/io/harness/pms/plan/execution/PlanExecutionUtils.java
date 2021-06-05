package io.harness.pms.plan.execution;

import static io.harness.plan.Plan.PlanBuilder;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.dto.LevelDTO;
import io.harness.plan.Plan;
import io.harness.pms.contracts.plan.PlanCreationBlobResponse;
import io.harness.pms.contracts.plan.PlanNodeProto;
import io.harness.pms.yaml.YAMLFieldNameConstants;
import io.harness.pms.yaml.YamlUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(HarnessTeam.PIPELINE)
public class PlanExecutionUtils {
  public Plan extractPlan(PlanCreationBlobResponse planCreationBlobResponse) {
    PlanBuilder planBuilder = Plan.builder();
    Collection<PlanNodeProto> planNodeProtoList = planCreationBlobResponse.getNodesMap().values();
    for (PlanNodeProto planNodeProto : planNodeProtoList) {
      planBuilder.node(planNodeProto);
    }
    if (planCreationBlobResponse.getStartingNodeId() != null) {
      planBuilder.startingNodeId(planCreationBlobResponse.getStartingNodeId());
    }
    if (planCreationBlobResponse.getGraphLayoutInfo() != null) {
      planBuilder.graphLayoutInfo(planCreationBlobResponse.getGraphLayoutInfo());
    }
    return planBuilder.build();
  }

  public String getFQNUsingLevels(List<LevelDTO> levels) {
    List<String> fqnList = new ArrayList<>();
    for (LevelDTO level : levels) {
      if (!YamlUtils.shouldNotIncludeInQualifiedName(level.getIdentifier())
          && !level.getIdentifier().equals(YAMLFieldNameConstants.PARALLEL + level.getSetupId())
          && !level.isSkipExpressionChain()) {
        fqnList.add(level.getIdentifier());
      }
    }
    return String.join(".", fqnList);
  }
}
