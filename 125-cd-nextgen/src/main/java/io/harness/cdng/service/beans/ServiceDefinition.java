package io.harness.cdng.service.beans;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.EXTERNAL_PROPERTY;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

import io.harness.cdng.service.ServiceSpec;
import io.harness.cdng.visitor.YamlTypes;
import io.harness.cdng.visitor.helpers.serviceconfig.ServiceDefinitionVisitorHelper;
import io.harness.walktree.beans.LevelNode;
import io.harness.walktree.beans.VisitableChildren;
import io.harness.walktree.visitor.SimpleVisitorHelper;
import io.harness.walktree.visitor.Visitable;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@SimpleVisitorHelper(helperClass = ServiceDefinitionVisitorHelper.class)
public class ServiceDefinition implements Visitable {
  String type;
  @JsonProperty("spec")
  @JsonTypeInfo(use = NAME, property = "type", include = EXTERNAL_PROPERTY, visible = true)
  ServiceSpec serviceSpec;

  // For Visitor Framework Impl
  String metadata;

  // Use Builder as Constructor then only external property(visible) will be filled.
  @Builder
  public ServiceDefinition(String type, ServiceSpec serviceSpec) {
    this.type = type;
    this.serviceSpec = serviceSpec;
  }

  @Override
  public VisitableChildren getChildrenToWalk() {
    VisitableChildren children = VisitableChildren.builder().build();
    children.add("serviceSpec", serviceSpec);
    return children;
  }

  @Override
  public LevelNode getLevelNode() {
    return LevelNode.builder().qualifierName(YamlTypes.SERVICE_DEFINITION).build();
  }
}
