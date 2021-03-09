package io.harness.delegate.beans;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

@Data
@Builder
@FieldNameConstants(innerTypeName = "ScopingRulesKeys")
@TargetModule(Module._420_DELEGATE_SERVICE)
public class ScopingRules {
  private List<ScopingRuleDetails> scopingRuleDetails;
}
