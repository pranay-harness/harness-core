/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.beans;

import software.wings.stencils.Stencil;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Created by peeyushaggarwal on 4/11/17.
 */
public interface InfrastructureMappingDescriptor extends Stencil<InfrastructureMapping> {
  @Override @JsonIgnore Class<? extends InfrastructureMapping> getTypeClass();
}
