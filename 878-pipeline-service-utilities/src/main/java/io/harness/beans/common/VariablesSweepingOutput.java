/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.beans.common;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.sdk.core.data.ExecutionSweepingOutput;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.HashMap;
import org.springframework.data.annotation.TypeAlias;

@TypeAlias("variablesSweepingOutput")
@JsonTypeName("variablesSweepingOutput")
@OwnedBy(CDC)
@RecasterAlias("io.harness.beans.common.VariablesSweepingOutput")
public class VariablesSweepingOutput extends HashMap<String, Object> implements ExecutionSweepingOutput {}
