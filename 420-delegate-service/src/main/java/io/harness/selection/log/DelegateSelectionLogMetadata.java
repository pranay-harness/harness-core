package io.harness.selection.log;

import lombok.Builder;
import lombok.Data;

/**
 * Intended to be used for storing various additional data required for selection logs.
 */
@Data
@Builder
public class DelegateSelectionLogMetadata {
  ProfileScopingRulesMetadata profileScopingRulesMetadata;
}
