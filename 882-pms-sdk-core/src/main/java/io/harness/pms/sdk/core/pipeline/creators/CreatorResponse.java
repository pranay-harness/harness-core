/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.pms.sdk.core.pipeline.creators;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.yaml.YamlField;

import java.util.Map;

@OwnedBy(HarnessTeam.PIPELINE)
public interface CreatorResponse {
  Map<String, YamlField> getDependencies();
  void addDependency(YamlField field);
  void addResolvedDependency(YamlField yamlField);
}
