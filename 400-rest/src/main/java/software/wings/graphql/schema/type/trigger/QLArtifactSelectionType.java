/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.graphql.schema.type.trigger;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

@TargetModule(HarnessModule._380_CG_GRAPHQL)
@OwnedBy(HarnessTeam.CDC)
public enum QLArtifactSelectionType {
  FROM_TRIGGERING_ARTIFACT,
  FROM_TRIGGERING_PIPELINE,
  FROM_PAYLOAD_SOURCE,
  LAST_COLLECTED,
  LAST_DEPLOYED_WORKFLOW,
  LAST_DEPLOYED_PIPELINE
}
