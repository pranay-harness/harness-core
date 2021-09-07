package io.harness.cdng.variables.beans;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.visitor.helpers.variables.VariableOverridesVisitorHelper;
import io.harness.data.validator.EntityIdentifier;
import io.harness.pms.sdk.core.data.ExecutionSweepingOutput;
import io.harness.walktree.visitor.SimpleVisitorHelper;
import io.harness.walktree.visitor.Visitable;
import io.harness.yaml.core.variables.NGVariable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModelProperty;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;
import lombok.Value;

@OwnedBy(HarnessTeam.CDC)
@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName("overrideSet")
@SimpleVisitorHelper(helperClass = VariableOverridesVisitorHelper.class)
public class NGVariableOverrideSets implements Visitable {
  @EntityIdentifier String identifier;
  List<NGVariable> variables;

  // For Visitor Framework Impl
  @Getter(onMethod_ = { @ApiModelProperty(hidden = true) }) @ApiModelProperty(hidden = true) String metadata;

  @RecasterAlias("io.harness.cdng.variables.beans.NGVariableOverrideSets$NGVariableOverrideSetsSweepingOutput")
  public static class NGVariableOverrideSetsSweepingOutput
      extends HashMap<String, NGVariableOverrideSetsSweepingOutputInner> implements ExecutionSweepingOutput {}

  @Value
  public static class NGVariableOverrideSetsSweepingOutputInner {
    Map<String, Object> variables;
  }
}
