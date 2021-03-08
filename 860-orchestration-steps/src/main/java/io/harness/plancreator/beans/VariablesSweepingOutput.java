package io.harness.plancreator.beans;

import io.harness.pms.sdk.core.data.SweepingOutput;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.HashMap;
import org.springframework.data.annotation.TypeAlias;

@TypeAlias("variablesSweepingOutput")
@JsonTypeName("variablesSweepingOutput")
public class VariablesSweepingOutput extends HashMap<String, Object> implements SweepingOutput {
  @Override
  public String getType() {
    return "variablesSweepingOutput";
  }
}
