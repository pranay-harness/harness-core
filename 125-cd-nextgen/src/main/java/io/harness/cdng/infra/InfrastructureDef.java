/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cdng.infra;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.EXTERNAL_PROPERTY;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

import io.harness.annotation.RecasterAlias;
import io.harness.cdng.infra.yaml.Infrastructure;
import io.harness.cdng.infra.yaml.InfrastructureType;
import io.harness.cdng.visitor.helpers.pipelineinfrastructure.InfrastructureDefVisitorHelper;
import io.harness.plancreator.execution.ExecutionElementConfig;
import io.harness.walktree.beans.VisitableChildren;
import io.harness.walktree.visitor.SimpleVisitorHelper;
import io.harness.walktree.visitor.Visitable;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.annotations.ApiModelProperty;
import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.TypeAlias;

@Data
@NoArgsConstructor
@SimpleVisitorHelper(helperClass = InfrastructureDefVisitorHelper.class)
@TypeAlias("infrastructureDef")
@RecasterAlias("io.harness.cdng.infra.InfrastructureDef")
public class InfrastructureDef implements Visitable {
  @NotNull @JsonProperty("type") InfrastructureType type;

  @JsonProperty("spec")
  @JsonTypeInfo(use = NAME, property = "type", include = EXTERNAL_PROPERTY, visible = true)
  @NotNull
  Infrastructure spec;

  @JsonProperty("provisioner") @Nullable ExecutionElementConfig provisioner;

  // For Visitor Framework Impl
  @Getter(onMethod_ = { @ApiModelProperty(hidden = true) }) @ApiModelProperty(hidden = true) String metadata;

  // Use Builder as Constructor then only external property(visible) will be filled.
  @Builder
  public InfrastructureDef(InfrastructureType type, Infrastructure spec, ExecutionElementConfig provisioner) {
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
}
