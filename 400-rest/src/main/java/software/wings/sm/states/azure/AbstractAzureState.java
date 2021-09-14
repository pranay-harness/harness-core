/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.sm.states.azure;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.delegate.beans.pcf.ResizeStrategy;

import software.wings.beans.command.CommandUnit;
import software.wings.sm.ExecutionContext;
import software.wings.sm.State;
import software.wings.sm.StateType;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@JsonIgnoreProperties(ignoreUnknown = true)
@Slf4j
@TargetModule(HarnessModule._870_CG_ORCHESTRATION)
public abstract class AbstractAzureState extends State {
  public AbstractAzureState(String name, StateType stateType) {
    super(name, stateType.name());
  }

  public abstract List<CommandUnit> getCommandUnits(ExecutionContext context, ResizeStrategy resizeStrategy);
}
