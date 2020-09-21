package io.harness.cdng.pipeline;

import io.harness.cdng.environment.yaml.EnvironmentYaml;
import io.harness.cdng.infra.InfrastructureDef;
import io.harness.cdng.infra.beans.InfraUseFromStage;
import io.harness.cdng.visitor.LevelNodeQualifierName;
import io.harness.cdng.visitor.helpers.pipelineinfrastructure.PipelineInfrastructureVisitorHelper;
import io.harness.state.Step;
import io.harness.walktree.beans.LevelNode;
import io.harness.walktree.beans.VisitableChildren;
import io.harness.walktree.visitor.SimpleVisitorHelper;
import io.harness.walktree.visitor.Visitable;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Wither;

import java.util.List;

@Data
@Builder
@SimpleVisitorHelper(helperClass = PipelineInfrastructureVisitorHelper.class)
public class PipelineInfrastructure implements Visitable {
  private InfrastructureDef infrastructureDefinition;
  @Wither private InfraUseFromStage useFromStage;
  private EnvironmentYaml environment;
  private List<Step> steps;
  private List<Step> rollbackSteps;

  // For Visitor Framework Impl
  String metadata;

  public PipelineInfrastructure applyUseFromStage(PipelineInfrastructure infrastructureToUseFrom) {
    return infrastructureToUseFrom.withUseFromStage(this.useFromStage);
  }

  @Override
  public VisitableChildren getChildrenToWalk() {
    VisitableChildren children = VisitableChildren.builder().build();
    children.add("infrastructureDefinition", infrastructureDefinition);
    children.add("environment", environment);
    return children;
  }

  @Override
  public LevelNode getLevelNode() {
    return LevelNode.builder().qualifierName(LevelNodeQualifierName.PIPELINE_INFRASTRUCTURE).build();
  }
}
