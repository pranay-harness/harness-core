package io.harness.delegate.beans;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

@Data
@Builder
@FieldNameConstants(innerTypeName = "ScopingRulesKeys")
public class ScopingRules {
  private List<ScopingRuleDetails> scopingRuleDetails;
}
