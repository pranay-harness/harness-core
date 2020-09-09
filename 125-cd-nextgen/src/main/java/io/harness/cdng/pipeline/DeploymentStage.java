package io.harness.cdng.pipeline;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.cdng.service.beans.ServiceConfig;
import io.harness.cdng.variables.StageVariables;
import io.harness.cdng.visitor.helpers.deploymentstage.DeploymentStageVisitorHelper;
import io.harness.walktree.visitor.SimpleVisitorHelper;
import io.harness.walktree.visitor.Visitable;
import io.harness.yaml.core.ExecutionElement;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

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
  String skipCondition;

  @Override
  public List<Object> getChildrenToWalk() {
    List<Object> children = new ArrayList<>();
    // the ordering [service,infrastructure, execution] is necessary
    children.add(service);
    children.add(infrastructure);
    children.add(execution);
    children.add(stageVariables);
    return children;
  }
}
