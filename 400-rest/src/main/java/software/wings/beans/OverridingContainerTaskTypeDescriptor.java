/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans;

import software.wings.beans.container.ContainerTask;
import software.wings.beans.container.ContainerTaskTypeDescriptor;
import software.wings.stencils.OverridingStencil;
import software.wings.stencils.StencilCategory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Optional;

/**
 * Created by anubhaw on 2/6/17.
 */
public class OverridingContainerTaskTypeDescriptor
    implements OverridingStencil<ContainerTask>, ContainerTaskTypeDescriptor {
  private ContainerTaskTypeDescriptor containerTaskTypeDescriptor;
  private Optional<String> overridingName = Optional.empty();
  private Optional<JsonNode> overridingJsonSchema = Optional.empty();

  public OverridingContainerTaskTypeDescriptor(ContainerTaskTypeDescriptor containerTaskTypeDescriptor) {
    this.containerTaskTypeDescriptor = containerTaskTypeDescriptor;
  }

  @Override
  public String getType() {
    return containerTaskTypeDescriptor.getType();
  }

  @Override
  @JsonIgnore
  public Class<? extends ContainerTask> getTypeClass() {
    return containerTaskTypeDescriptor.getTypeClass();
  }

  @Override
  public JsonNode getJsonSchema() {
    return overridingJsonSchema.isPresent() ? overridingJsonSchema.get().deepCopy()
                                            : containerTaskTypeDescriptor.getJsonSchema();
  }

  @Override
  public Object getUiSchema() {
    return containerTaskTypeDescriptor.getUiSchema();
  }

  @Override
  public String getName() {
    return overridingName.orElse(containerTaskTypeDescriptor.getName());
  }

  @Override
  public OverridingStencil getOverridingStencil() {
    return containerTaskTypeDescriptor.getOverridingStencil();
  }

  @Override
  public ContainerTask newInstance(String id) {
    return containerTaskTypeDescriptor.newInstance(id);
  }

  @Override
  public boolean matches(Object context) {
    return containerTaskTypeDescriptor.matches(context);
  }

  @Override
  public JsonNode getOverridingJsonSchema() {
    return overridingJsonSchema.orElse(null);
  }

  @Override
  public void setOverridingJsonSchema(JsonNode overridingJsonSchema) {
    this.overridingJsonSchema = Optional.ofNullable(overridingJsonSchema);
  }

  @Override
  public String getOverridingName() {
    return overridingName.orElse(null);
  }

  @Override
  public void setOverridingName(String overridingName) {
    this.overridingName = Optional.ofNullable(overridingName);
  }

  @Override
  public StencilCategory getStencilCategory() {
    return containerTaskTypeDescriptor == null ? null : containerTaskTypeDescriptor.getStencilCategory();
  }

  @Override
  public Integer getDisplayOrder() {
    return containerTaskTypeDescriptor == null ? Integer.valueOf(DEFAULT_DISPLAY_ORDER)
                                               : containerTaskTypeDescriptor.getDisplayOrder();
  }
}
