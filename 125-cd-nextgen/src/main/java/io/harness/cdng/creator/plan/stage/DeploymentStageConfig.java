package io.harness.cdng.creator.plan.stage;

import io.harness.EntityType;
import io.harness.cdng.pipeline.PipelineInfrastructure;
import io.harness.cdng.service.beans.ServiceConfig;
import io.harness.cdng.visitor.helpers.deploymentstage.DeploymentStageVisitorHelper;
import io.harness.common.SwaggerConstants;
import io.harness.plancreator.execution.ExecutionElementConfig;
import io.harness.plancreator.stages.stage.StageInfoConfig;
import io.harness.pms.yaml.ParameterField;
import io.harness.walktree.beans.LevelNode;
import io.harness.walktree.beans.VisitableChild;
import io.harness.walktree.beans.VisitableChildren;
import io.harness.walktree.visitor.SimpleVisitorHelper;
import io.harness.walktree.visitor.Visitable;
import io.harness.yaml.schema.YamlSchemaRoot;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import org.springframework.data.annotation.TypeAlias;

@Data
@Builder
@AllArgsConstructor
@JsonTypeName("Deployment")
@TypeAlias("deploymentStageConfig")
@SimpleVisitorHelper(helperClass = DeploymentStageVisitorHelper.class)
@YamlSchemaRoot(EntityType.DEPLOYMENT_STAGE)
public class DeploymentStageConfig implements StageInfoConfig, Visitable {
  @Getter(onMethod_ = { @ApiModelProperty(hidden = true) }) @ApiModelProperty(hidden = true) String uuid;
  ServiceConfig serviceConfig;
  PipelineInfrastructure infrastructure;
  ExecutionElementConfig execution;
  @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) ParameterField<String> skipCondition;

  // For Visitor Framework Impl
  @Getter(onMethod_ = { @ApiModelProperty(hidden = true) }) @ApiModelProperty(hidden = true) String metadata;

  @Override
  public LevelNode getLevelNode() {
    return null;
  }

  @Override
  public VisitableChildren getChildrenToWalk() {
    List<VisitableChild> children = new ArrayList<>();
    children.add(VisitableChild.builder().value(serviceConfig).fieldName("serviceConfig").build());
    children.add(VisitableChild.builder().value(infrastructure).fieldName("infrastructure").build());
    return VisitableChildren.builder().visitableChildList(children).build();
  }
}