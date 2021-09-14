/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

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
