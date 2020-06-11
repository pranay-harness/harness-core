package io.harness.state.io;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(CDC)
@Redesign
//@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "class") TODO JsonTypeInfo remove
public interface StepParameters {}
