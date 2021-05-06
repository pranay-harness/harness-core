package io.harness.plancreator.beans;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.pms.sdk.core.data.ExecutionSweepingOutput;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.HashMap;
import org.springframework.data.annotation.TypeAlias;

@TypeAlias("variablesSweepingOutput")
@JsonTypeName("variablesSweepingOutput")
@TargetModule(HarnessModule._878_PIPELINE_SERVICE_UTILITIES)
public class VariablesSweepingOutput extends HashMap<String, Object> implements ExecutionSweepingOutput {
  @Override
  public String getType() {
    return "variablesSweepingOutput";
  }
}
