package io.harness.selection.log;

import java.util.Set;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProfileScopingRulesMetadata {
  String profileId;
  Set<String> scopingRulesDescriptions;
}
