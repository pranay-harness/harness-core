/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.beans.command;

import software.wings.stencils.Stencil;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Created by peeyushaggarwal on 6/27/16.
 */
public interface CommandUnitDescriptor extends Stencil<CommandUnit> {
  @Override @JsonIgnore Class<? extends CommandUnit> getTypeClass();
}
