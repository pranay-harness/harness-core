package io.harness.cdng.variables.beans;

import io.harness.cdng.visitor.helpers.variables.VariableOverridesVisitorHelper;
import io.harness.data.validator.EntityIdentifier;
import io.harness.walktree.visitor.SimpleVisitorHelper;
import io.harness.walktree.visitor.Visitable;
import io.harness.yaml.core.variables.NGVariable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.Value;

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
}
