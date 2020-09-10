package io.harness.cdng.infra;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.EXTERNAL_PROPERTY;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.harness.cdng.infra.yaml.Infrastructure;
import io.harness.cdng.visitor.helpers.pipelineinfrastructure.InfrastructureDefVisitorHelper;
import io.harness.walktree.beans.VisitableChildren;
import io.harness.walktree.visitor.SimpleVisitorHelper;
import io.harness.walktree.visitor.Visitable;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@SimpleVisitorHelper(helperClass = InfrastructureDefVisitorHelper.class)
public class InfrastructureDef implements Visitable {
  String type;
  @JsonProperty("spec")
  @JsonTypeInfo(use = NAME, property = "type", include = EXTERNAL_PROPERTY, visible = true)
  Infrastructure infrastructure;

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
}
