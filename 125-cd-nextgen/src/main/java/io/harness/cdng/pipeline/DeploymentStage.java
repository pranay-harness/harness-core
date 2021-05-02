package io.harness.cdng.pipeline;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.common.SwaggerConstants;
import io.harness.cdng.service.beans.ServiceConfig;
import io.harness.cdng.visitor.YamlTypes;
import io.harness.cdng.visitor.helpers.deploymentstage.DeploymentStageVisitorHelper;
import io.harness.data.structure.EmptyPredicate;
import io.harness.pipeline.executions.NGStageType;
import io.harness.pms.yaml.ParameterField;
import io.harness.walktree.beans.LevelNode;
import io.harness.walktree.beans.VisitableChildren;
import io.harness.walktree.visitor.SimpleVisitorHelper;
import io.harness.walktree.visitor.Visitable;
import io.harness.yaml.core.ExecutionElement;
import io.harness.yaml.core.variables.NGVariable;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import org.springframework.data.annotation.TypeAlias;

@Data
@Builder
@JsonTypeName("Deployment")
@SimpleVisitorHelper(helperClass = DeploymentStageVisitorHelper.class)
@TypeAlias("deploymentStage")
@OwnedBy(CDC)
public class DeploymentStage implements CDStage, Visitable {
  @JsonIgnore public static String DEPLOYMENT_NAME = "Deployment";
  @JsonIgnore public static NGStageType DEPLOYMENT_STAGE_TYPE = NGStageType.builder().type(DEPLOYMENT_NAME).build();

  @Getter(onMethod = @__(@JsonIgnore)) @JsonIgnore String identifier;
  @Getter(onMethod = @__(@JsonIgnore)) @JsonIgnore String name;
  List<NGVariable> variables;
  ServiceConfig serviceConfig;
  PipelineInfrastructure infrastructure;
  ExecutionElement execution;
  @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) ParameterField<String> skipCondition;

  // For Visitor Framework Impl
  String metadata;

  @Override
  public VisitableChildren getChildrenToWalk() {
    VisitableChildren children = VisitableChildren.builder().build();
    // the ordering [variables, service,infrastructure, execution] is necessary
    if (EmptyPredicate.isNotEmpty(variables)) {
      variables.forEach(ngVariable -> children.add("variables", ngVariable));
    }
    children.add("serviceConfig", serviceConfig);
    children.add("infrastructure", infrastructure);
    children.add("execution", execution);
    return children;
  }

  @Override
  public NGStageType getStageType() {
    return DEPLOYMENT_STAGE_TYPE;
  }

  public LevelNode getLevelNode() {
    return LevelNode.builder().qualifierName(YamlTypes.SPEC).isPartOfFQN(false).build();
  }
}
