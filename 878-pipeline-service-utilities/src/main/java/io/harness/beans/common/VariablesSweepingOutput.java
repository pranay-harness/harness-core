package io.harness.beans.common;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.sdk.core.data.ExecutionSweepingOutput;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.HashMap;
import org.springframework.data.annotation.TypeAlias;

@TypeAlias("variablesSweepingOutput")
@JsonTypeName("variablesSweepingOutput")
@OwnedBy(CDC)
public class VariablesSweepingOutput extends HashMap<String, Object> implements ExecutionSweepingOutput {}
