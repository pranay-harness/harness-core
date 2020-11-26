package io.harness.cdng.infra;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.EXTERNAL_PROPERTY;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

import io.harness.cdng.infra.yaml.Infrastructure;
import io.harness.cdng.visitor.YamlTypes;
import io.harness.cdng.visitor.helpers.pipelineinfrastructure.InfrastructureDefVisitorHelper;
import io.harness.walktree.beans.LevelNode;
import io.harness.walktree.beans.VisitableChildren;
import io.harness.walktree.visitor.SimpleVisitorHelper;
import io.harness.walktree.visitor.Visitable;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Builder;
import lombok.Data;
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
  Infrastructure infrastructure;

  // For Visitor Framework Impl
  String metadata;

  // Use Builder as Constructor then only external property(visible) will be filled.
  @Builder
  public InfrastructureDef(String type, Infrastructure infrastructure) {
    this.type = type;
    this.infrastructure = infrastructure;
  }

  @Override
  public VisitableChildren getChildrenToWalk() {
    VisitableChildren children = VisitableChildren.builder().build();
    children.add("infrastructure", infrastructure);
    return children;
  }

  @Override
  public LevelNode getLevelNode() {
    return LevelNode.builder().qualifierName(YamlTypes.INFRASTRUCTURE_DEF).build();
  }
}
