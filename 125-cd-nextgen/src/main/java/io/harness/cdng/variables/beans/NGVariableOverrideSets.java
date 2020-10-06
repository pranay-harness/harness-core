package io.harness.cdng.variables.beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.cdng.visitor.LevelNodeQualifierName;
import io.harness.cdng.visitor.helpers.variables.VariableOverridesVisitorHelper;
import io.harness.data.validator.EntityIdentifier;
import io.harness.walktree.beans.LevelNode;
import io.harness.walktree.visitor.SimpleVisitorHelper;
import io.harness.walktree.visitor.Visitable;
import io.harness.yaml.core.intfc.OverrideSetsWrapper;
import io.harness.yaml.core.variables.NGVariable;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName("overrideSet")
@SimpleVisitorHelper(helperClass = VariableOverridesVisitorHelper.class)
public class NGVariableOverrideSets implements OverrideSetsWrapper, Visitable {
  @EntityIdentifier String identifier;
  List<NGVariable> variables;

  // For Visitor Framework Impl
  String metadata;

  @Override
  public LevelNode getLevelNode() {
    return LevelNode.builder()
        .qualifierName(
            LevelNodeQualifierName.VARIABLE_OVERRIDE_SETS + LevelNodeQualifierName.PATH_CONNECTOR + identifier)
        .build();
  }
}
