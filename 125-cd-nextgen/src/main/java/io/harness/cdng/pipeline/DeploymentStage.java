package io.harness.cdng.pipeline;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.beans.ParameterField;
import io.harness.cdng.service.beans.ServiceConfig;
import io.harness.cdng.variables.StageVariables;
import io.harness.cdng.visitor.helpers.deploymentstage.DeploymentStageVisitorHelper;
import io.harness.common.SwaggerConstants;
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
@JsonTypeName(DeploymentStage.DEPLOYMENT_NAME)
@SimpleVisitorHelper(helperClass = DeploymentStageVisitorHelper.class)
public class DeploymentStage implements CDStage, Visitable {
  @JsonIgnore public static final String DEPLOYMENT_NAME = "Deployment";
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
}
