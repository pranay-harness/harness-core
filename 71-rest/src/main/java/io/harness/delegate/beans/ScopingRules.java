package io.harness.delegate.beans;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

import java.util.List;

@Data
@Builder
@FieldNameConstants(innerTypeName = "ScopingRulesKeys")
public class ScopingRules {
  private List<ScopingRuleDetails> scopingRuleDetails;
}
