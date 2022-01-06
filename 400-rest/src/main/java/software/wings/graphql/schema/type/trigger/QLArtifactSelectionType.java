/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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
