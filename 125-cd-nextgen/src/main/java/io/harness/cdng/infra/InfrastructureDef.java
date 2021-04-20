package io.harness.cdng.infra;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.EXTERNAL_PROPERTY;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

import io.harness.cdng.infra.yaml.Infrastructure;
import io.harness.cdng.visitor.YamlTypes;
import io.harness.cdng.visitor.helpers.pipelineinfrastructure.InfrastructureDefVisitorHelper;
import io.harness.plancreator.execution.ExecutionElementConfig;
import io.harness.walktree.beans.LevelNode;
import io.harness.walktree.beans.VisitableChildren;
import io.harness.walktree.visitor.SimpleVisitorHelper;
import io.harness.walktree.visitor.Visitable;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.annotations.ApiModelProperty;
import javax.annotation.Nullable;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.TypeAlias;

@Data
@NoArgsConstructor
@SimpleVisitorHelper(helperClass = InfrastructureDefVisitorHelper.class)
@TypeAlias("infrastructureDef")
public class InfrastructureDef implements Visitable {
  String type;
  @JsonProperty("spec")
  @JsonTypeInfo(use = NAME, property = "type", include = EXTERNAL_PROPERTY, visible = true)
  Infrastructure spec;

  @JsonProperty("provisioner") @Nullable ExecutionElementConfig provisioner;

  // For Visitor Framework Impl
  @Getter(onMethod_ = { @ApiModelProperty(hidden = true) }) @ApiModelProperty(hidden = true) String metadata;

  // Use Builder as Constructor then only external property(visible) will be filled.
  @Builder
  public InfrastructureDef(String type, Infrastructure spec, ExecutionElementConfig provisioner) {
    this.type = type;
    this.spec = spec;
    this.provisioner = provisioner;
  }

  @Override
  public VisitableChildren getChildrenToWalk() {
    VisitableChildren children = VisitableChildren.builder().build();
    children.add("spec", spec);
    children.add("provisioner", provisioner);
    return children;
  }

  @Override
  public LevelNode getLevelNode() {
    return LevelNode.builder().qualifierName(YamlTypes.INFRASTRUCTURE_DEF).isPartOfFQN(false).build();
  }
}
