package io.harness.cdng.pipeline;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.beans.ParameterField;
import io.harness.cdng.service.beans.ServiceConfig;
import io.harness.cdng.variables.StageVariables;
import io.harness.cdng.visitor.LevelNodeQualifierName;
import io.harness.cdng.visitor.helpers.deploymentstage.DeploymentStageVisitorHelper;
import io.harness.common.SwaggerConstants;
import io.harness.pipeline.executions.NGStageType;
import io.harness.walktree.beans.LevelNode;
import io.harness.walktree.beans.VisitableChildren;
import io.harness.walktree.visitor.SimpleVisitorHelper;
import io.harness.walktree.visitor.Visitable;
import io.harness.yaml.core.ExecutionElement;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;

@Data
@Builder
@JsonTypeName("Deployment")
@SimpleVisitorHelper(helperClass = DeploymentStageVisitorHelper.class)
public class DeploymentStage implements CDStage, Visitable {
  @JsonIgnore public static String DEPLOYMENT_NAME = "Deployment";
  @JsonIgnore public static NGStageType DEPLOYMENT_STAGE_TYPE = NGStageType.builder().type(DEPLOYMENT_NAME).build();

  @Getter(onMethod = @__(@JsonIgnore)) @JsonIgnore String identifier;
  @Getter(onMethod = @__(@JsonIgnore)) @JsonIgnore String name;
  ServiceConfig service;
  PipelineInfrastructure infrastructure;
  ExecutionElement execution;
  StageVariables stageVariables;
  @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) ParameterField<String> skipCondition;

  // For Visitor Framework Impl
  String metadata;

  @Override
  public VisitableChildren getChildrenToWalk() {
    VisitableChildren children = VisitableChildren.builder().build();
    // the ordering [service,infrastructure, execution] is necessary
    children.add("service", service);
    children.add("infrastructure", infrastructure);
    children.add("execution", execution);
    children.add("stageVariables", stageVariables);
    return children;
  }

  @Override
  public NGStageType getStageType() {
    return DEPLOYMENT_STAGE_TYPE;
  }

  public LevelNode getLevelNode() {
    return LevelNode.builder().qualifierName(LevelNodeQualifierName.SPEC).build();
  }
}
